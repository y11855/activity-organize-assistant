package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.enums.ActivityTag;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.mq.ActivityEventPublisher;
import com.aiapp.activityassistant.common.mq.ActivityEventType;
import com.aiapp.activityassistant.common.mq.UserBehaviorPublisher;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.BehaviorReportDTO;
import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseExportDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.entity.QuestionnaireLike;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.entity.UserRecommendation;
import com.aiapp.activityassistant.mapper.QuestionnaireLikeMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
import com.aiapp.activityassistant.mapper.UserRecommendationMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.QuestionnaireResponseVO;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.aiapp.activityassistant.vo.ResponseStatisticsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务实现：报名问卷领域的具体逻辑（生成、发布、答卷提交、统计与 Excel 导出）
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl extends ServiceImpl<QuestionnaireMapper, Questionnaire>
        implements QuestionnaireService {

    private final ActivityService activityService;
    private final QuestionnaireResponseMapper responseMapper;
    private final QuestionnaireLikeMapper likeMapper;
    private final UserRecommendationMapper recommendationMapper;
    private final UserService userService;
    private final ActivityEventPublisher eventPublisher;
    private final UserBehaviorPublisher behaviorPublisher;

    @Override
    public Long generate(Long activityId) {
        activityService.checkManage(activityId, "questionnaire:manage");
        // 若已存在则直接返回
        Questionnaire existing = getOne(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getActivityId, activityId));
        if (existing != null) {
            return existing.getId();
        }
        Questionnaire q = new Questionnaire();
        q.setActivityId(activityId);
        q.setTitle("活动报名问卷");
        q.setDescription("请填写报名信息");
        q.setQuestions(
                "[{\"title\":\"是否参加\",\"type\":\"single\",\"required\":true,\"options\":[\"参加\",\"不参加\"]},{\"title\":\"备注\",\"type\":\"text\",\"required\":false}]");
        q.setStatus(1);
        save(q);
        return q.getId();
    }

    @Override
    public void publish(Long id) {
        Questionnaire q = getById(id);
        if (q != null) {
            activityService.checkManage(q.getActivityId(), "questionnaire:manage");
            q.setStatus(1);
            updateById(q);
        }
    }

    @Override
    public void close(Long id) {
        Questionnaire q = getById(id);
        if (q != null) {
            activityService.checkManage(q.getActivityId(), "questionnaire:manage");
            q.setStatus(2);
            updateById(q);
        }
    }

    @Override
    public List<QuestionnaireVO> listByActivity(Long activityId) {
        activityService.checkAccess(activityId);
        List<Questionnaire> list = list(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getActivityId, activityId));
        return list.stream().map(q -> {
            QuestionnaireVO vo = new QuestionnaireVO();
            BeanUtils.copyProperties(q, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void update(Long id, QuestionnaireSaveDTO dto) {
        Questionnaire q = getById(id);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 仅活动创建人可编辑问卷
        activityService.checkManage(q.getActivityId(), "questionnaire:manage");
        q.setTitle(dto.getTitle());
        q.setDescription(dto.getDescription());
        q.setCoverImage(dto.getCoverImage());
        if (dto.getQuestions() != null) {
            q.setQuestions(dto.getQuestions());
        }
        updateById(q);
    }

    @Override
    public PageVO<QuestionnaireVO> listMine(PageQuery query) {
        Long currentUserId = UserContext.getCurrentUserId();
        List<Activity> myActivities = activityService.list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, currentUserId));
        if (myActivities.isEmpty()) {
            return PageVO.of(new ArrayList<>(), 0L, query.getPage(), query.getSize());
        }
        List<Long> activityIds = myActivities.stream().map(Activity::getId).toList();
        Page<Questionnaire> page = page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Questionnaire>()
                        .in(Questionnaire::getActivityId, activityIds)
                        .orderByDesc(Questionnaire::getCreateTime));
        List<QuestionnaireVO> records = toVOs(page.getRecords());
        fillResponseCount(records);
        return PageVO.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageVO<QuestionnaireVO> listPublished(PageQuery query) {
        Page<Questionnaire> page = page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Questionnaire>()
                        .eq(Questionnaire::getStatus, 1)
                        .eq(Questionnaire::getType, 1)
                        .orderByDesc(Questionnaire::getCreateTime));
        List<QuestionnaireVO> records = toVOs(page.getRecords());
        fillResponseCount(records);
        return PageVO.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public QuestionnaireVO detail(Long id) {
        Questionnaire q = getById(id);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        QuestionnaireVO vo = new QuestionnaireVO();
        BeanUtils.copyProperties(q, vo);
        Activity activity = activityService.getById(q.getActivityId());
        if (activity != null) {
            vo.setActivityTitle(activity.getTitle());
            vo.setActivityTag(activity.getTag());
            vo.setActivityTagName(ActivityTag.labelOf(activity.getTag()));
            // 报名页需要向参与者展示活动详情：介绍、时间、地点
            vo.setIntroduction(activity.getIntroduction());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setLocation(activity.getLocation());
        }
        fillLikeInfo(List.of(vo));
        fillRegisteredInfo(List.of(vo));
        return vo;
    }

    @Override
    public void submitResponse(Long id, ResponseSubmitDTO dto) {
        Questionnaire q = getById(id);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(q.getStatus(), 1)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "问卷未发布或已截止，无法提交");
        }
        // 报名问卷（type=1）：活动结束时间到后禁止报名，与定时任务自动关闭问卷形成双保险
        if (Objects.equals(q.getType(), 1)) {
            Activity activity = activityService.getById(q.getActivityId());
            if (activity != null && activity.getEndTime() != null
                    && !LocalDateTime.now().isBefore(activity.getEndTime())) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "活动已截止，无法报名");
            }
        }
        Long currentUserId = UserContext.getCurrentUserId();
        User user = userService.getById(currentUserId);

        // 同一用户对同一问卷只保留一条答卷：已提交过则更新原记录，避免重复提交把报名人数刷高。
        // 历史数据可能已存在重复行，用 limit 1 取最新一条，避免 selectOne 抛 TooManyResults。
        QuestionnaireResponse existing = responseMapper.selectOne(
                new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getQuestionnaireId, q.getId())
                        .eq(QuestionnaireResponse::getRespondentId, currentUserId)
                        .orderByDesc(QuestionnaireResponse::getSubmitTime)
                        .last("limit 1"));
        if (existing != null) {
            existing.setAnswer(dto.getAnswer());
            existing.setSubmitTime(LocalDateTime.now());
            existing.setRespondentName(user == null ? "未知用户" : user.getNickname());
            // 之前取消过报名的用户重新提交时，恢复为已报名
            existing.setStatus(1);
            responseMapper.updateById(existing);
            publishRegistration(q, existing.getRespondentName());
            return;
        }

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaireId(q.getId());
        response.setActivityId(q.getActivityId());
        response.setRespondentId(currentUserId);
        response.setRespondentName(user == null ? "未知用户" : user.getNickname());
        response.setAnswer(dto.getAnswer());
        response.setStatus(1);
        response.setSubmitTime(LocalDateTime.now());
        responseMapper.insert(response);
        publishRegistration(q, response.getRespondentName());
    }

    /** 报名事件投递给活动创建人的大屏（异步，不影响报名主流程） */
    private void publishRegistration(Questionnaire q, String respondentName) {
        Activity activity = activityService.getById(q.getActivityId());
        if (activity != null) {
            eventPublisher.publish(ActivityEventType.REGISTRATION_SUBMITTED, activity.getCreatorId(),
                    activity.getId(), respondentName + " 报名了「" + activity.getTitle() + "」");
        }
    }

    @Override
    public List<QuestionnaireResponseVO> listResponses(Long questionnaireId, Integer status) {
        Questionnaire q = getById(questionnaireId);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        activityService.checkManage(q.getActivityId(), "questionnaire:manage");
        LambdaQueryWrapper<QuestionnaireResponse> qw = new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                .orderByDesc(QuestionnaireResponse::getSubmitTime);
        if (status != null) {
            qw.eq(QuestionnaireResponse::getStatus, status);
        }
        return responseMapper.selectList(qw).stream().map(r -> {
            QuestionnaireResponseVO vo = new QuestionnaireResponseVO();
            vo.setId(r.getId());
            vo.setRespondentId(r.getRespondentId());
            vo.setRespondentName(r.getRespondentName());
            vo.setAnswer(r.getAnswer());
            vo.setStatus(r.getStatus());
            vo.setSubmitTime(r.getSubmitTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ResponseStatisticsVO statistics(Long questionnaireId) {
        Questionnaire q = getById(questionnaireId);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        activityService.checkManage(q.getActivityId(), "questionnaire:manage");

        List<QuestionnaireResponse> all = responseMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId));
        ResponseStatisticsVO vo = new ResponseStatisticsVO();
        vo.setTotal((long) all.size());

        // 按状态分组
        Map<Integer, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus() == null ? 1 : r.getStatus(),
                        Collectors.counting()));
        vo.setByStatus(byStatus);

        // 按日期时间线（yyyy-MM-dd -> 人数），按日期升序
        Map<String, Long> dayMap = all.stream()
                .filter(r -> r.getSubmitTime() != null)
                .collect(Collectors.groupingBy(r -> r.getSubmitTime().toLocalDate().toString(),
                        java.util.TreeMap::new, Collectors.counting()));
        List<Map<String, Object>> timeline = new ArrayList<>();
        dayMap.forEach((date, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", count);
            timeline.add(item);
        });
        vo.setTimeline(timeline);
        return vo;
    }

    @Override
    public byte[] exportResponses(Long questionnaireId) {
        Questionnaire q = getById(questionnaireId);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        activityService.checkManage(q.getActivityId(), "questionnaire:manage");
        List<QuestionnaireResponse> all = responseMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                        .orderByAsc(QuestionnaireResponse::getSubmitTime));
        List<ResponseExportDTO> rows = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            QuestionnaireResponse r = all.get(i);
            ResponseExportDTO dto = new ResponseExportDTO();
            dto.setIndex(i + 1);
            dto.setRespondentName(r.getRespondentName());
            dto.setStatus(r.getStatus() != null && r.getStatus() == 2 ? "已取消" : "已报名");
            dto.setAnswer(r.getAnswer());
            dto.setSubmitTime(r.getSubmitTime());
            rows.add(dto);
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.alibaba.excel.EasyExcel.write(out, ResponseExportDTO.class)
                .sheet("报名人员").doWrite(rows);
        return out.toByteArray();
    }

    @Override
    public int closeExpiredQuestionnaires() {
        // 只处理已发布的报名问卷（type=1）；反馈问卷（type=2）活动结束后才填写，不受截止影响
        List<Questionnaire> list = list(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getStatus, 1)
                .eq(Questionnaire::getType, 1));
        if (list.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> activityIds = list.stream().map(Questionnaire::getActivityId).distinct().toList();
        Map<Long, LocalDateTime> endTimeMap = activityService.listByIds(activityIds).stream()
                .filter(a -> a.getEndTime() != null)
                .collect(Collectors.toMap(Activity::getId, Activity::getEndTime));
        int count = 0;
        for (Questionnaire q : list) {
            LocalDateTime end = endTimeMap.get(q.getActivityId());
            // 活动结束时间已到 → 自动截止报名问卷
            if (end != null && !now.isBefore(end)) {
                q.setStatus(2);
                updateById(q);
                count++;
            }
        }
        return count;
    }

    @Override
    public Long ensureFeedbackQuestionnaire(Long activityId) {
        Questionnaire existing = getOne(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getActivityId, activityId)
                .eq(Questionnaire::getType, 2));
        if (existing != null) {
            if (!Objects.equals(existing.getStatus(), 1)) {
                existing.setStatus(1);
                updateById(existing);
            }
            return existing.getId();
        }
        Questionnaire q = new Questionnaire();
        q.setActivityId(activityId);
        q.setType(2);
        q.setTitle("活动反馈问卷");
        q.setDescription("感谢您参与本次活动，花 1 分钟填写反馈，帮助我们改进！");
        q.setQuestions(
                "[{\"title\":\"整体满意度\",\"type\":\"single\",\"required\":true,\"options\":[\"非常满意\",\"满意\",\"一般\",\"不满意\"]},"
                        + "{\"title\":\"最满意的环节\",\"type\":\"text\",\"required\":false},"
                        + "{\"title\":\"需要改进的地方\",\"type\":\"text\",\"required\":false},"
                        + "{\"title\":\"其他建议\",\"type\":\"text\",\"required\":false}]");
        q.setStatus(1);
        save(q);
        return q.getId();
    }

    @Override
    public void like(Long id) {
        Questionnaire q = getById(id);
        if (q == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long userId = UserContext.getCurrentUserId();
        boolean exists = likeMapper.selectCount(new LambdaQueryWrapper<QuestionnaireLike>()
                .eq(QuestionnaireLike::getQuestionnaireId, id)
                .eq(QuestionnaireLike::getUserId, userId)) > 0;
        if (exists) {
            return;
        }
        QuestionnaireLike like = new QuestionnaireLike();
        like.setQuestionnaireId(id);
        like.setActivityId(q.getActivityId());
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        try {
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 并发重复点赞：唯一键兜底，直接视为已点赞
            return;
        }
        // 点赞行为入 Kafka，Flink 据此提升该用户对活动标签的兴趣得分
        Activity activity = activityService.getById(q.getActivityId());
        behaviorPublisher.publish(userId, id, q.getActivityId(),
                activity == null ? null : activity.getTag(), UserBehaviorPublisher.TYPE_LIKE, 0);
    }

    @Override
    public void unlike(Long id) {
        likeMapper.delete(new LambdaQueryWrapper<QuestionnaireLike>()
                .eq(QuestionnaireLike::getQuestionnaireId, id)
                .eq(QuestionnaireLike::getUserId, UserContext.getCurrentUserId()));
    }

    @Override
    public PageVO<QuestionnaireVO> listLiked(PageQuery query) {
        List<QuestionnaireLike> likes = likeMapper.selectList(new LambdaQueryWrapper<QuestionnaireLike>()
                .eq(QuestionnaireLike::getUserId, UserContext.getCurrentUserId())
                .orderByDesc(QuestionnaireLike::getCreateTime));
        return sliceByIds(likes.stream().map(QuestionnaireLike::getQuestionnaireId).distinct().toList(), query);
    }

    @Override
    public PageVO<QuestionnaireVO> listRegistered(PageQuery query) {
        Long userId = UserContext.getCurrentUserId();
        List<QuestionnaireResponse> responses = responseMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getRespondentId, userId)
                        .ne(QuestionnaireResponse::getStatus, 2)
                        .orderByDesc(QuestionnaireResponse::getSubmitTime));
        List<Long> questionnaireIds = responses.stream()
                .map(QuestionnaireResponse::getQuestionnaireId).distinct().toList();
        if (questionnaireIds.isEmpty()) {
            return PageVO.of(new ArrayList<>(), 0L, query.getPage(), query.getSize());
        }
        // 只展示报名问卷（type=1），活动反馈问卷不计入「我报名的」
        Map<Long, Questionnaire> map = listByIds(questionnaireIds).stream()
                .filter(q -> Objects.equals(q.getType(), 1))
                .collect(Collectors.toMap(Questionnaire::getId, q -> q));
        List<Questionnaire> ordered = questionnaireIds.stream()
                .map(map::get).filter(Objects::nonNull).toList();
        return slice(ordered, query);
    }

    @Override
    public PageVO<QuestionnaireVO> listRecommended(PageQuery query) {
        Long userId = UserContext.getCurrentUserId();
        List<UserRecommendation> interests = recommendationMapper.selectList(
                new LambdaQueryWrapper<UserRecommendation>()
                        .eq(UserRecommendation::getUserId, userId)
                        .orderByDesc(UserRecommendation::getScore)
                        .last("limit 3"));
        Set<Long> registeredIds = registeredQuestionnaireIds(userId);
        if (interests.isEmpty()) {
            // 冷启动：当前用户还没有行为数据，按最新发布回退推荐，避免推荐位空白
            List<Questionnaire> latest = list(new LambdaQueryWrapper<Questionnaire>()
                    .eq(Questionnaire::getStatus, 1)
                    .eq(Questionnaire::getType, 1)
                    .orderByDesc(Questionnaire::getCreateTime));
            return slice(latest.stream().filter(q -> !registeredIds.contains(q.getId())).toList(), query);
        }
        List<String> tags = interests.stream().map(UserRecommendation::getTag).toList();
        List<Activity> matchedActivities = activityService.list(
                new LambdaQueryWrapper<Activity>().in(Activity::getTag, tags));
        if (matchedActivities.isEmpty()) {
            return PageVO.of(new ArrayList<>(), 0L, query.getPage(), query.getSize());
        }
        // 兴趣标签排名：得分越高越靠前；同标签内保持按发布时间倒序
        Map<String, Integer> tagRank = new HashMap<>();
        for (int i = 0; i < tags.size(); i++) {
            tagRank.put(tags.get(i), i);
        }
        Map<Long, Integer> activityRank = matchedActivities.stream()
                .collect(Collectors.toMap(Activity::getId,
                        a -> tagRank.getOrDefault(a.getTag(), Integer.MAX_VALUE)));
        List<Questionnaire> candidates = list(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getStatus, 1)
                .eq(Questionnaire::getType, 1)
                .in(Questionnaire::getActivityId, activityRank.keySet())
                .orderByDesc(Questionnaire::getCreateTime))
                .stream()
                .filter(q -> !registeredIds.contains(q.getId()))
                .sorted((a, b) -> Integer.compare(
                        activityRank.getOrDefault(a.getActivityId(), Integer.MAX_VALUE),
                        activityRank.getOrDefault(b.getActivityId(), Integer.MAX_VALUE)))
                .toList();
        return slice(candidates, query);
    }

    @Override
    public void reportBehavior(BehaviorReportDTO dto) {
        String type = dto.getType();
        if (!UserBehaviorPublisher.TYPE_VIEW.equals(type) && !UserBehaviorPublisher.TYPE_LIKE.equals(type)) {
            return;
        }
        Questionnaire q = getById(dto.getQuestionnaireId());
        if (q == null) {
            // 行为采集属于旁路数据，问卷不存在时静默丢弃，不影响前端
            return;
        }
        Activity activity = activityService.getById(q.getActivityId());
        behaviorPublisher.publish(UserContext.getCurrentUserId(), q.getId(), q.getActivityId(),
                activity == null ? null : activity.getTag(), type, dto.getDurationSeconds());
    }

    /**
     * 问卷实体转 VO：批量补全活动标题/标签、点赞数、当前用户点赞与报名状态
     */
    private List<QuestionnaireVO> toVOs(List<Questionnaire> questionnaires) {
        List<QuestionnaireVO> vos = new ArrayList<>();
        if (questionnaires == null || questionnaires.isEmpty()) {
            return vos;
        }
        Map<Long, Activity> activityMap = activityService.listByIds(
                        questionnaires.stream().map(Questionnaire::getActivityId).distinct().toList())
                .stream().collect(Collectors.toMap(Activity::getId, a -> a));
        for (Questionnaire q : questionnaires) {
            QuestionnaireVO vo = new QuestionnaireVO();
            BeanUtils.copyProperties(q, vo);
            Activity activity = activityMap.get(q.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
                vo.setActivityTag(activity.getTag());
                vo.setActivityTagName(ActivityTag.labelOf(activity.getTag()));
            }
            vos.add(vo);
        }
        fillLikeInfo(vos);
        fillRegisteredInfo(vos);
        return vos;
    }

    /** 批量填充点赞数与当前用户点赞状态 */
    private void fillLikeInfo(List<QuestionnaireVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(QuestionnaireVO::getId).toList();
        Map<Long, Long> countMap = likeMapper.selectList(new LambdaQueryWrapper<QuestionnaireLike>()
                        .select(QuestionnaireLike::getQuestionnaireId)
                        .in(QuestionnaireLike::getQuestionnaireId, ids))
                .stream()
                .collect(Collectors.groupingBy(QuestionnaireLike::getQuestionnaireId, Collectors.counting()));
        Set<Long> likedIds = likeMapper.selectList(new LambdaQueryWrapper<QuestionnaireLike>()
                        .select(QuestionnaireLike::getQuestionnaireId)
                        .eq(QuestionnaireLike::getUserId, UserContext.getCurrentUserId())
                        .in(QuestionnaireLike::getQuestionnaireId, ids))
                .stream().map(QuestionnaireLike::getQuestionnaireId).collect(Collectors.toSet());
        vos.forEach(vo -> {
            vo.setLikeCount(countMap.getOrDefault(vo.getId(), 0L).intValue());
            vo.setLiked(likedIds.contains(vo.getId()));
        });
    }

    /** 批量填充当前用户报名状态 */
    private void fillRegisteredInfo(List<QuestionnaireVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        Set<Long> registeredIds = registeredQuestionnaireIds(UserContext.getCurrentUserId());
        vos.forEach(vo -> vo.setRegistered(registeredIds.contains(vo.getId())));
    }

    /** 当前用户已报名的问卷 ID 集合（已取消报名的记录不计入） */
    private Set<Long> registeredQuestionnaireIds(Long userId) {
        return responseMapper.selectList(new LambdaQueryWrapper<QuestionnaireResponse>()
                        .select(QuestionnaireResponse::getQuestionnaireId)
                        .eq(QuestionnaireResponse::getRespondentId, userId)
                        .ne(QuestionnaireResponse::getStatus, 2))
                .stream().map(QuestionnaireResponse::getQuestionnaireId).collect(Collectors.toSet());
    }

    /** 批量统计报名人数，避免逐条查询 */
    private void fillResponseCount(List<QuestionnaireVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> ids = vos.stream().map(QuestionnaireVO::getId).toList();
        Map<Long, Long> countMap = responseMapper.selectList(new LambdaQueryWrapper<QuestionnaireResponse>()
                        .select(QuestionnaireResponse::getQuestionnaireId)
                        .in(QuestionnaireResponse::getQuestionnaireId, ids))
                .stream()
                .collect(Collectors.groupingBy(QuestionnaireResponse::getQuestionnaireId, Collectors.counting()));
        vos.forEach(vo -> vo.setResponseCount(countMap.getOrDefault(vo.getId(), 0L).intValue()));
    }

    /** 按问卷 ID 顺序加载并分页（保持点赞/报名的时间倒序） */
    private PageVO<QuestionnaireVO> sliceByIds(List<Long> questionnaireIds, PageQuery query) {
        if (questionnaireIds.isEmpty()) {
            return PageVO.of(new ArrayList<>(), 0L, query.getPage(), query.getSize());
        }
        Map<Long, Questionnaire> map = listByIds(questionnaireIds).stream()
                .collect(Collectors.toMap(Questionnaire::getId, q -> q));
        List<Questionnaire> ordered = questionnaireIds.stream()
                .map(map::get).filter(Objects::nonNull).toList();
        return slice(ordered, query);
    }

    /** 内存分页：统一补齐活动信息、点赞与报名状态、报名人数 */
    private PageVO<QuestionnaireVO> slice(List<Questionnaire> all, PageQuery query) {
        long page = query.getPage();
        long size = query.getSize();
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        List<QuestionnaireVO> vos = toVOs(all.subList(from, to));
        fillResponseCount(vos);
        return PageVO.of(vos, all.size(), page, size);
    }
}
