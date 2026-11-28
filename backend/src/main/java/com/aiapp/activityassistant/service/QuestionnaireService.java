package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.dto.BehaviorReportDTO;
import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.vo.QuestionnaireResponseVO;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.aiapp.activityassistant.vo.ResponseStatisticsVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 业务接口：报名问卷领域的能力定义
 */
public interface QuestionnaireService extends IService<Questionnaire> {

    /**
     * 为活动自动生成问卷
     */
    Long generate(Long activityId);

    /**
     * 发布问卷
     */
    void publish(Long id);

    /**
     * 截止问卷
     */
    void close(Long id);

    /**
     * 获取报名统计
     */
    List<QuestionnaireVO> listByActivity(Long activityId);

    /**
     * 编辑问卷（标题/描述/封面/题目），仅活动创建人
     */
    void update(Long id, QuestionnaireSaveDTO dto);

    /**
     * 分页查询当前用户所有活动下的问卷列表（含封面、报名数）
     */
    PageVO<QuestionnaireVO> listMine(PageQuery query);

    /**
     * 分页查询全部已发布的报名问卷（所有登录用户均可查看与填写）
     */
    PageVO<QuestionnaireVO> listPublished(PageQuery query);

    /**
     * 按 ID 获取问卷详情（填写页用，登录即可）
     */
    QuestionnaireVO detail(Long id);

    /**
     * 点赞问卷（保存点赞记录，并向 Kafka 上报点赞行为）
     */
    void like(Long id);

    /**
     * 取消点赞（物理删除点赞记录）
     */
    void unlike(Long id);

    /**
     * 分页查询当前用户点赞过的问卷
     */
    PageVO<QuestionnaireVO> listLiked(PageQuery query);

    /**
     * 分页查询当前用户已报名的问卷
     */
    PageVO<QuestionnaireVO> listRegistered(PageQuery query);

    /**
     * 分页查询为当前用户推荐的活动问卷（依据 Flink 聚合的兴趣标签）
     */
    PageVO<QuestionnaireVO> listRecommended(PageQuery query);

    /**
     * 上报用户行为（停留时长 / 点赞），写入 Kafka 供 Flink 计算推荐
     */
    void reportBehavior(BehaviorReportDTO dto);

    /**
     * 提交答卷
     */
    void submitResponse(Long id, ResponseSubmitDTO dto);

    /**
     * 报名人员明细列表（可按状态筛选）
     */
    List<QuestionnaireResponseVO> listResponses(Long questionnaireId, Integer status);

    /**
     * 报名统计看板：总人数、按状态分组、按日期时间线
     */
    ResponseStatisticsVO statistics(Long questionnaireId);

    /**
     * 导出报名人员 Excel 字节流
     */
    byte[] exportResponses(Long questionnaireId);

    /**
     * 确保活动反馈问卷（type=2）存在，返回其 ID；不存在则创建默认反馈问卷
     */
    Long ensureFeedbackQuestionnaire(Long activityId);

    /**
     * 自动关闭已到活动截止时间的报名问卷（type=1），由调度器调用
     *
     * @return 本次关闭的问卷数量
     */
    int closeExpiredQuestionnaires();
}
