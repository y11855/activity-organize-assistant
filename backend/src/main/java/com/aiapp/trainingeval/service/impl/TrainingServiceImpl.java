package com.aiapp.trainingeval.service.impl;

import com.aiapp.trainingeval.common.exception.BusinessException;
import com.aiapp.trainingeval.common.result.ResultCode;
import com.aiapp.trainingeval.dto.TrainingSaveDTO;
import com.aiapp.trainingeval.entity.Training;
import com.aiapp.trainingeval.mapper.TrainingMapper;
import com.aiapp.trainingeval.service.TrainingService;
import com.aiapp.trainingeval.vo.TrainingVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 实训业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl extends ServiceImpl<TrainingMapper, Training>
        implements TrainingService {

    @Override
    public Long submit(TrainingSaveDTO dto) {
        Training training = new Training();
        BeanUtils.copyProperties(dto, training);
        training.setStatus(0);
        save(training);
        // 异步触发 AI 校验
        triggerAiEvaluate(training.getId());
        return training.getId();
    }

    @Override
    public TrainingVO detail(Long id) {
        Training training = getById(id);
        if (training == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        TrainingVO vo = new TrainingVO();
        BeanUtils.copyProperties(training, vo);
        return vo;
    }

    @Async
    @Override
    public void triggerAiEvaluate(Long trainingId) {
        log.info("触发 AI 校验, trainingId={}", trainingId);
        // TODO: 调用 AiProvider 进行合规校验、步骤完整性检查、逻辑漏洞识别
        // 1. 文件解析（Word / PDF / 图片 OCR）
        // 2. 构造提示词
        // 3. 调用 AiProvider.chat(...)
        // 4. 解析 AI 返回结构，落库
    }
}
