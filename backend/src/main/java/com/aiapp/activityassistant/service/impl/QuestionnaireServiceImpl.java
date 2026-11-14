package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.dto.PageQuery;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.mq.ActivityEventPublisher;
import com.aiapp.activityassistant.common.mq.ActivityEventType;
import com.aiapp.activityassistant.common.result.PageVO;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseExportDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.QuestionnaireMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 问卷业务实现
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl extends ServiceImpl<QuestionnaireMapper, Questionnaire>
        implements QuestionnaireService {

    private final ActivityService activityService;
    private final QuestionnaireResponseMapper responseMapper;
    private final UserService userService;
    private final ActivityEventPublisher eventPublisher;

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
        Map<Long, String> activityTitleMap = myActivities.stream()
                .collect(Collectors.toMap(Activity::getId, a -> a.getTitle() == null ? "" : a.getTitle()));
        Page<Questionnaire> page = page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Questionnaire>()
                        .in(Questionnaire::getActivityId, activityTitleMap.keySet())
                        .orderByDesc(Questionnaire::getCreateTime));
        List<QuestionnaireVO> records = page.getRecords().stream().map(q -> {
            QuestionnaireVO vo = new QuestionnaireVO();
            BeanUtils.copyProperties(q, vo);
            vo.setActivityTitle(activityTitleMap.getOrDefault(q.getActivityId(), ""));
            Long count = responseMapper.selectCount(new LambdaQueryWrapper<QuestionnaireResponse>()
                    .eq(QuestionnaireResponse::getQuestionnaireId, q.getId()));
            vo.setResponseCount(count.intValue());
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageVO<QuestionnaireVO> listPublished(PageQuery query) {
        Page<Questionnaire> page = page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Questionnaire>()
                        .eq(Questionnaire::getStatus, 1)
                        .eq(Questionnaire::getType, 1)
                        .orderByDesc(Questionnaire::getCreateTime));
        List<Questionnaire> records = page.getRecords();
        if (records.isEmpty()) {
            return PageVO.of(new ArrayList<>(), page.getTotal(), page.getCurrent(), page.getSize());
        }
        List<Long> activityIds = records.stream().map(Questionnaire::getActivityId).distinct().toList();
        Map<Long, String> activityTitleMap = activityService.listByIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, a -> a.getTitle() == null ? "" : a.getTitle()));
        // 一次性统计本页各问卷的报名人数，避免逐条查询
        List<Long> questionnaireIds = records.stream().map(Questionnaire::getId).toList();
        Map<Long, Long> countMap = responseMapper.selectList(new LambdaQueryWrapper<QuestionnaireResponse>()
                        .select(QuestionnaireResponse::getQuestionnaireId)
                        .in(QuestionnaireResponse::getQuestionnaireId, questionnaireIds))
                .stream()
                .collect(Collectors.groupingBy(QuestionnaireResponse::getQuestionnaireId, Collectors.counting()));
        List<QuestionnaireVO> list = records.stream().map(q -> {
            QuestionnaireVO vo = new QuestionnaireVO();
            BeanUtils.copyProperties(q, vo);
            vo.setActivityTitle(activityTitleMap.getOrDefault(q.getActivityId(), ""));
            vo.setResponseCount(countMap.getOrDefault(q.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(list, page.getTotal(), page.getCurrent(), page.getSize());
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
        vo.setActivityTitle(activity == null ? "" : activity.getTitle());
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
}
