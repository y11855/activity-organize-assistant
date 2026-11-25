package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.Material;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：物料清单表（material）的数据访问接口
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}
