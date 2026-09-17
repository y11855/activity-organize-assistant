package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.ReminderCreateDTO;
import com.aiapp.activityassistant.entity.Reminder;
import com.aiapp.activityassistant.vo.ReminderVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 提醒业务接口
 */
public interface ReminderService extends IService<Reminder> {

    /**
     * 创建提醒
     */
    Long create(ReminderCreateDTO dto);

    /**
     * 取消提醒（活动前定时提醒可取消）
     */
    void cancel(Long id);

    /**
     * 获取活动下的提醒列表
     */
    List<ReminderVO> listByActivity(Long activityId);

    /**
     * 扫描并触发到期提醒（由调度器调用）
     */
    void triggerDueReminders();
}
