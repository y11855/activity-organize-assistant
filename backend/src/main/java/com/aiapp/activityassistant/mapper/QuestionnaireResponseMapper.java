package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.QuestionnaireResponse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：问卷回答/报名记录表（questionnaire_response）的数据访问接口
 */
@Mapper
public interface QuestionnaireResponseMapper extends BaseMapper<QuestionnaireResponse> {
}
