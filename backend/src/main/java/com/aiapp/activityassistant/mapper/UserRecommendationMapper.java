package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.UserRecommendation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：用户兴趣得分表（Flink 写入，后端读取）
 */
@Mapper
public interface UserRecommendationMapper extends BaseMapper<UserRecommendation> {
}
