package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
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
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Override
    public Long generate(Long activityId) {
        activityService.checkOwnership(activityId);
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
    public QuestionnaireVO detailByActivity(Long activityId) {
        Questionnaire q = getOne(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getActivityId, activityId));
        if (q == null) {
            return null;
        }
        QuestionnaireVO vo = new QuestionnaireVO();
        BeanUtils.copyProperties(q, vo);
        Long count = responseMapper.selectCount(new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, q.getId()));
        vo.setResponseCount(count.intValue());
        return vo;
    }

    @Override
    public void publish(Long id) {
        Questionnaire q = getById(id);
        if (q != null) {
            q.setStatus(1);
            updateById(q);
        }
    }

    @Override
    public void close(Long id) {
        Questionnaire q = getById(id);
        if (q != null) {
            q.setStatus(2);
            updateById(q);
        }
    }

    @Override
    public List<QuestionnaireVO> listByActivity(Long activityId) {
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
        activityService.checkOwnership(q.getActivityId());
        q.setTitle(dto.getTitle());
        q.setDescription(dto.getDescription());
        q.setCoverImage(dto.getCoverImage());
        if (dto.getQuestions() != null) {
            q.setQuestions(dto.getQuestions());
        }
        updateById(q);
    }

    @Override
    public List<QuestionnaireVO> listMine() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<Activity> myActivities = activityService.list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, currentUserId));
        if (myActivities.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> activityTitleMap = myActivities.stream()
                .collect(Collectors.toMap(Activity::getId, a -> a.getTitle() == null ? "" : a.getTitle()));
        List<Questionnaire> list = list(new LambdaQueryWrapper<Questionnaire>()
                .in(Questionnaire::getActivityId, activityTitleMap.keySet())
                .orderByDesc(Questionnaire::getCreateTime));
        return list.stream().map(q -> {
            QuestionnaireVO vo = new QuestionnaireVO();
            BeanUtils.copyProperties(q, vo);
            vo.setActivityTitle(activityTitleMap.getOrDefault(q.getActivityId(), ""));
            Long count = responseMapper.selectCount(new LambdaQueryWrapper<QuestionnaireResponse>()
                    .eq(QuestionnaireResponse::getQuestionnaireId, q.getId()));
            vo.setResponseCount(count.intValue());
            return vo;
        }).collect(Collectors.toList());
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
        if (q.getStatus() == null || q.getStatus() != 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "问卷未发布或已截止，无法提交");
        }
        Long currentUserId = UserContext.getCurrentUserId();
        User user = userService.getById(currentUserId);
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaireId(q.getId());
        response.setActivityId(q.getActivityId());
        response.setRespondentId(currentUserId);
        response.setRespondentName(user == null ? "未知用户" : user.getNickname());
        response.setAnswer(dto.getAnswer());
        response.setSubmitTime(LocalDateTime.now());
        responseMapper.insert(response);
    }
}
