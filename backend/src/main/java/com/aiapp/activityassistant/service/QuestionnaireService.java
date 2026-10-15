package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
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

    /**
     * 编辑问卷（标题/描述/封面/题目），仅活动创建人
     */
    void update(Long id, QuestionnaireSaveDTO dto);

    /**
     * 当前用户所有活动下的问卷列表（含封面、报名数）
     */
    List<QuestionnaireVO> listMine();

    /**
     * 按 ID 获取问卷详情（填写页用，登录即可）
     */
    QuestionnaireVO detail(Long id);

    /**
     * 提交答卷（填写人取当前登录用户）
     */
    void submitResponse(Long id, ResponseSubmitDTO dto);
}
