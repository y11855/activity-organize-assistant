package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 问卷业务接口
 */
public interface QuestionnaireService extends IService<Questionnaire> {

    /**
     * 为活动自动生成问卷
     */
    Long generate(Long activityId);

    /**
     * 获取活动问卷详情
     */
    QuestionnaireVO detailByActivity(Long activityId);

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
}
