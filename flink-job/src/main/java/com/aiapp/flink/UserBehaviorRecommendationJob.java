package com.aiapp.flink;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Set;

/**
 * 活动推荐 Flink 作业：
 * <ol>
 *   <li>消费 Kafka 主题 user-behavior（后端上报的「问卷停留时长」与「点赞」）</li>
 *   <li>按 userId 分组，用 Flink 状态累加每个活动标签的兴趣得分</li>
 *   <li>得分结果实时 upsert 到 MySQL 表 user_recommendation，供后端生成推荐列表</li>
 * </ol>
 * 得分规则：点赞 +5；停留时长每满 60 秒 +1（单次最多 5 分），不足 3 秒视为误触不计分。
 */
public class UserBehaviorRecommendationJob {

    /** 兴趣得分 upsert：同一用户同一标签只保留一行，分数覆盖为 Flink 中的累计值（幂等） */
    private static final String UPSERT_SQL = "INSERT INTO user_recommendation (user_id, tag, score, update_time) "
            + "VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE score = VALUES(score), update_time = NOW()";

    /** 与后端 ActivityTag 枚举保持一致，非法标签直接丢弃 */
    private static final Set<String> VALID_TAGS = Set.of(
            "sports", "art", "academic", "public_welfare", "social", "entertainment", "other");

    private static final String DEFAULT_MYSQL_URL = "jdbc:mysql://localhost:3306/activity_assistant"
            + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
            + "&allowPublicKeyRetrieval=true";

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 开启 checkpoint：作业重启后从快照恢复各用户的兴趣状态，避免得分归零
        env.enableCheckpointing(10_000L);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(params.get("kafka.servers", "localhost:9092"))
                .setTopics(params.get("kafka.topic", "user-behavior"))
                .setGroupId(params.get("kafka.group", "flink-activity-recommendation"))
                // 行为流只关心新数据，从最新位点开始消费
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<BehaviorEvent> behaviors = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-user-behavior")
                .flatMap(new BehaviorParser())
                .name("parse-behavior");

        DataStream<TagScore> scores = behaviors
                .keyBy(BehaviorEvent::getUserId)
                .process(new InterestAggregator())
                .name("aggregate-interest");

        scores.addSink(JdbcSink.sink(
                        UPSERT_SQL,
                        (ps, score) -> {
                            ps.setLong(1, score.userId);
                            ps.setString(2, score.tag);
                            ps.setDouble(3, score.score);
                        },
                        JdbcExecutionOptions.builder()
                                .withBatchSize(50)
                                .withBatchIntervalMs(1000L)
                                .withMaxRetries(3)
                                .build(),
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl(params.get("mysql.url", DEFAULT_MYSQL_URL))
                                .withDriverName("com.mysql.cj.jdbc.Driver")
                                .withUsername(params.get("mysql.username", "root"))
                                .withPassword(params.get("mysql.password", "root"))
                                .build()))
                .name("mysql-user-recommendation");

        env.execute("activity-recommendation-job");
    }

    /**
     * 行为消息解析：坏消息丢弃，避免单条异常阻塞整条流
     */
    public static class BehaviorParser extends RichFlatMapFunction<String, BehaviorEvent> {

        private transient ObjectMapper objectMapper;

        @Override
        public void open(Configuration parameters) {
            objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Override
        public void flatMap(String value, Collector<BehaviorEvent> out) {
            try {
                BehaviorEvent event = objectMapper.readValue(value, BehaviorEvent.class);
                if (event.userId == null || event.tag == null || !VALID_TAGS.contains(event.tag)) {
                    return;
                }
                out.collect(event);
            } catch (Exception e) {
                // 忽略无法解析的消息（格式错误 / 生产端字段变更）
            }
        }
    }

    /**
     * 兴趣聚合：按用户维护「标签 -> 累计得分」状态，每次行为后输出最新的累计值
     */
    public static class InterestAggregator extends KeyedProcessFunction<Long, BehaviorEvent, TagScore> {

        private static final MapStateDescriptor<String, Double> STATE_DESCRIPTOR =
                new MapStateDescriptor<>("tag-interest", Types.STRING, Types.DOUBLE);

        private transient MapState<String, Double> tagScores;

        @Override
        public void open(Configuration parameters) {
            tagScores = getRuntimeContext().getMapState(STATE_DESCRIPTOR);
        }

        @Override
        public void processElement(BehaviorEvent event, Context ctx, Collector<TagScore> out) throws Exception {
            double weight = weightOf(event);
            if (weight <= 0) {
                return;
            }
            Double current = tagScores.get(event.tag);
            double total = (current == null ? 0D : current) + weight;
            tagScores.put(event.tag, total);
            out.collect(new TagScore(event.userId, event.tag, total));
        }

        /**
         * 行为权重：点赞 5 分；停留时长按 60 秒 1 分折算，单次上限 5 分
         */
        private double weightOf(BehaviorEvent event) {
            if ("like".equals(event.type)) {
                return 5D;
            }
            if ("view".equals(event.type)) {
                if (event.duration < 3) {
                    return 0D;
                }
                return Math.min(event.duration, 300) / 60D;
            }
            return 0D;
        }
    }

    /**
     * 行为事件（对应后端 UserBehaviorPublisher 发送的 JSON）
     */
    public static class BehaviorEvent {

        public Long userId;
        public Long questionnaireId;
        public Long activityId;
        /** 活动属性标签 */
        public String tag;
        /** view / like */
        public String type;
        /** 停留秒数 */
        public int duration;

        public Long getUserId() {
            return userId;
        }
    }

    /**
     * 聚合结果：用户在某标签上的累计兴趣得分
     */
    public static class TagScore {

        public long userId;
        public String tag;
        public double score;

        public TagScore() {
        }

        public TagScore(long userId, String tag, double score) {
            this.userId = userId;
            this.tag = tag;
            this.score = score;
        }
    }
}
