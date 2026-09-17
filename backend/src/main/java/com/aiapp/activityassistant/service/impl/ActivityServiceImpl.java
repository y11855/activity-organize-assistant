package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.agent.ActivityAgent;
import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.ActivitySaveDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.Material;
import com.aiapp.activityassistant.entity.TaskAssignment;
import com.aiapp.activityassistant.mapper.ActivityMapper;
import com.aiapp.activityassistant.mapper.MaterialMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.TaskService;
import com.aiapp.activityassistant.vo.ActivityVO;
import com.aiapp.activityassistant.vo.MaterialVO;
import com.aiapp.activityassistant.vo.TaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity>
        implements ActivityService {

    private final MaterialMapper materialMapper;
    private final TaskService taskService;
    @Lazy
    private final ActivityAgent activityAgent;

    @Override
    public Long create(ActivitySaveDTO dto) {
        Long currentUserId = UserContext.getCurrentUserId();
        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity);
        activity.setCreatorId(currentUserId);
        activity.setStatus(0);
        save(activity);

        // 异步触发 Agent 全流程编排
        activityAgent.run(activity.getId());
        return activity.getId();
    }

    @Override
    public ActivityVO detail(Long id) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);

        // 物料清单
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().eq(Material::getActivityId, id));
        vo.setMaterials(materials.stream().map(m -> {
            MaterialVO mvo = new MaterialVO();
            BeanUtils.copyProperties(m, mvo);
            return mvo;
        }).collect(Collectors.toList()));

        // 任务列表
        List<TaskAssignment> tasks = taskService.listByActivity(id);
        vo.setTasks(tasks.stream().map(t -> {
            TaskVO tvo = new TaskVO();
            BeanUtils.copyProperties(t, tvo);
            return tvo;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public List<ActivityVO> listMine() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<Activity> list = list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, currentUserId)
                .orderByDesc(Activity::getCreateTime));
        return list.stream().map(a -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void cancel(Long id) {
        checkOwnership(id);
        Activity activity = getById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        activity.setStatus(4);
        updateById(activity);
    }

    @Async
    @Override
    public String generateReview(Long id) {
        checkOwnership(id);
        return activityAgent.review(id);
    }

    @Override
    public void checkOwnership(Long activityId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long currentUserId = UserContext.getCurrentUserId();
        if (!currentUserId.equals(activity.getCreatorId())) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_OWNED);
        }
    }
}
