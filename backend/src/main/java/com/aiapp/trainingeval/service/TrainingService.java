package com.aiapp.trainingeval.service;

import com.aiapp.trainingeval.dto.TrainingSaveDTO;
import com.aiapp.trainingeval.entity.Training;
import com.aiapp.trainingeval.vo.TrainingVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 实训业务接口
 */
public interface TrainingService extends IService<Training> {

    /**
     * 上传实训成果并触发 AI 校验
     */
    Long submit(TrainingSaveDTO dto);

    /**
     * 获取实训详情（含评分）
     */
    TrainingVO detail(Long id);

    /**
     * 触发 AI 合规校验
     */
    void triggerAiEvaluate(Long trainingId);
}
