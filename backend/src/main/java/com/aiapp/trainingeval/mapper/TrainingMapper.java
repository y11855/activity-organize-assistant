package com.aiapp.trainingeval.mapper;

import com.aiapp.trainingeval.entity.Training;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实训记录 Mapper
 */
@Mapper
public interface TrainingMapper extends BaseMapper<Training> {
}
