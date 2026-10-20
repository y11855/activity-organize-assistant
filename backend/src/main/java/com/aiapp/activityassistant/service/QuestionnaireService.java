package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.QuestionnaireSaveDTO;
import com.aiapp.activityassistant.dto.ResponseSubmitDTO;
import com.aiapp.activityassistant.entity.Questionnaire;
import com.aiapp.activityassistant.vo.QuestionnaireResponseVO;
import com.aiapp.activityassistant.vo.QuestionnaireVO;
import com.aiapp.activityassistant.vo.ResponseStatisticsVO;
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
}
