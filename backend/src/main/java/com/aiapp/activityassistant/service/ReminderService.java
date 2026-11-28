package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.ReminderCreateDTO;
import com.aiapp.activityassistant.dto.ReminderUpdateDTO;
import com.aiapp.activityassistant.entity.Reminder;
import com.aiapp.activityassistant.vo.ReminderVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 业务接口：定时提醒领域的能力定义
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
     * 更新提醒（标题/内容/触发时间），仅待触发状态可改；创建者可自定义提醒时间
     */
    void update(Long id, ReminderUpdateDTO dto);

    /**
     * 获取活动下的提醒列表
     */
    List<ReminderVO> listByActivity(Long activityId);

    /**
     * 获取发给当前用户的提醒列表
     */
    List<ReminderVO> listMine();

    /**
     * 扫描并触发到期提醒（由调度器调用）
     */
    void triggerDueReminders();
}
