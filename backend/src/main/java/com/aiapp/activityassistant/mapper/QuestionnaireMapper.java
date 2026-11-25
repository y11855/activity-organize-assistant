package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Questionnaire;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：问卷表（questionnaire）的数据访问接口
 */
@Mapper
public interface QuestionnaireMapper extends BaseMapper<Questionnaire> {
}
