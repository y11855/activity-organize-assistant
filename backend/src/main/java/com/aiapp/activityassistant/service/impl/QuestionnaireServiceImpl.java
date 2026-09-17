package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.aiapp.activityassistant.mapper.QuestionnaireMapper;
import com.aiapp.activityassistant.mapper.QuestionnaireResponseMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.QuestionnaireService;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
        q.setQuestions("[{\"title\":\"是否参加\",\"type\":\"single\",\"required\":true,\"options\":[\"参加\",\"不参加\"]},{\"title\":\"备注\",\"type\":\"text\",\"required\":false}]");
        q.setStatus(1);
        q.setCreateTime(LocalDateTime.now());
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
}
