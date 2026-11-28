package com.aiapp.activityassistant.mapper;

import com.aiapp.activityassistant.entity.SysLog;
import com.aiapp.activityassistant.vo.LogOverviewVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus Mapper：系统日志表（sys_log）的数据访问接口。
 *
 * <p>概览统计用一条聚合 SQL 完成，避免多次往返数据库；清空操作为物理删除，
 * 防止日志数据越积越多。</p>
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    /**
     * 概览统计：一次查询返回六项核心指标。
     *
     * <p>在线用户以「近 30 分钟产生过日志的去重用户数」近似，无需额外维护在线状态。</p>
     */
    @Select("""
            SELECT
                COUNT(*) AS total_calls,
                COUNT(CASE WHEN create_time >= CURDATE() THEN 1 END) AS today_calls,
                COUNT(CASE WHEN log_type = 'exception' THEN 1 END) AS exception_count,
                COUNT(CASE WHEN duration >= 1000 THEN 1 END) AS slow_count,
                COUNT(CASE WHEN log_type = 'login' AND success = 0 THEN 1 END) AS login_fail_count,
                (SELECT COUNT(DISTINCT user_id) FROM sys_log
                  WHERE deleted = 0 AND user_id IS NOT NULL
                    AND create_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)) AS online_users
            FROM sys_log
            WHERE deleted = 0
            """)
    LogOverviewVO selectOverview();

    /** 已产生日志的业务模块（筛选项） */
    @Select("SELECT DISTINCT module FROM sys_log WHERE deleted = 0 AND module IS NOT NULL ORDER BY module")
    List<String> selectModules();

    /** 清空全部日志（物理删除，含已逻辑删除的数据） */
    @Delete("DELETE FROM sys_log")
    int purgeAll();
}
