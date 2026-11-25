package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.ActivityFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：活动资料文件表（activity_file）的数据访问接口
 */
@Mapper
public interface ActivityFileMapper extends BaseMapper<ActivityFile> {
}
