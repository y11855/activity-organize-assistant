package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.ActivityGenerateDTO;
import com.aiapp.activityassistant.dto.ActivitySaveDTO;
import com.aiapp.activityassistant.dto.ReminderDraftDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.vo.ActivityBreakdownNodeVO;
import com.aiapp.activityassistant.vo.ActivityDraftVO;
import com.aiapp.activityassistant.vo.ActivityVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 活动业务接口
 */
public interface ActivityService extends IService<Activity> {

    /**
     * 一句话发起活动（仅落库为草稿，AI 生成需发起人勾选生成项后另行触发）
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
     * 触发 AI 生成活动内容草稿（异步），状态由草稿 → 策划中
     *
     * @param options 发起人勾选的生成项
     */
    void generate(Long id, ActivityGenerateDTO options);

    /**
     * 获取 AI 生成的内容草稿（仅待确认状态，供预览/编辑）
     */
    ActivityDraftVO draft(Long id);

    /**
     * 保存发起人对草稿的编辑（仅待确认状态可编辑）
     */
    void updateDraft(Long id, ActivityDraftVO draft);

    /**
     * 确认发布：把草稿正式落库为任务分工/物料清单/报名问卷/定时提醒
     */
    void publish(Long id);

    /**
     * 取消活动：所有报名问卷自动截止；可选是否删除活动记录及其关联数据
     *
     * @param deleteRecord 是否同时删除活动记录（true 时级联删除关联数据）
     */
    void cancel(Long id, boolean deleteRecord);

    /**
     * 生成活动复盘总结
     */
    String generateReview(Long id);

    /**
     * 校验当前用户是否为活动创建人（权限控制）
     */
    void checkOwnership(Long activityId);

    /**
     * 清除活动详情缓存（活动/子资源发生变更后调用）
     */
    void evictDetailCache(Long activityId);

    /**
     * 获取活动分解树：活动 → 任务/物料/问卷/提醒 的多级树结构
     */
    List<ActivityBreakdownNodeVO> breakdownTree();

    /**
     * 确认任务分工并向被分配成员发送站内消息
     *
     * @return 已通知的成员数
     */
    int confirmTasks(Long id);

    /**
     * 把提醒草稿落库为定时提醒（供发布阶段复用）
     */
    void createReminders(Long activityId, List<ReminderDraftDTO> reminders);
}
