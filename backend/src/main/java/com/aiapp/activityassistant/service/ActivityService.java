package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.ActivitySaveDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.vo.ActivityVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 活动业务接口
 */
public interface ActivityService extends IService<Activity> {

    /**
     * 一句话发起活动，触发 Agent 全流程（策划→问卷→任务→提醒）
     */
    Long create(ActivitySaveDTO dto);

    /**
     * 获取活动详情（含物料、任务）
     */
    ActivityVO detail(Long id);

    /**
     * 列出当前用户创建的活动（权限控制）
     */
    List<ActivityVO> listMine();

    /**
     * 取消活动
     */
    void cancel(Long id);

    /**
     * 生成活动复盘总结
     */
    String generateReview(Long id);

    /**
     * 校验当前用户是否为活动创建人（权限控制）
     */
    void checkOwnership(Long activityId);
}
