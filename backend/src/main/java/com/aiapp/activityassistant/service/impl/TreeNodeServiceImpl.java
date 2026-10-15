package com.aiapp.activityassistant.service.impl;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.entity.TreeNode;
import com.aiapp.activityassistant.mapper.TreeNodeMapper;
import com.aiapp.activityassistant.service.TreeNodeService;
import com.aiapp.activityassistant.vo.TreeNodeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TreeNodeServiceImpl extends ServiceImpl<TreeNodeMapper, TreeNode>
        implements TreeNodeService {

    @Override
    public List<TreeNodeVO> getTree() {
        // 1. 查出全部节点
        List<TreeNode> all = list(new LambdaQueryWrapper<TreeNode>()
                .orderByAsc(TreeNode::getSort));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 转 VO，用 Map 建索引便于查找父节点
        Map<Long, TreeNodeVO> voMap = new HashMap<>();
        for (TreeNode n : all) {
            TreeNodeVO vo = new TreeNodeVO();
            BeanUtils.copyProperties(n, vo);
            vo.setChildren(new ArrayList<>());
            voMap.put(vo.getId(), vo);
        }

        // 3. 组装父子关系
        List<TreeNodeVO> roots = new ArrayList<>();
        for (TreeNodeVO vo : voMap.values()) {
            Long pid = vo.getParentId();
            if (pid == null || pid == 0L || !voMap.containsKey(pid)) {
                roots.add(vo);
            } else {
                voMap.get(pid).getChildren().add(vo);
            }
        }
        return roots;
    }

    @Override
    public TreeNode getById(Long id) {
        TreeNode node = super.getById(id);
        if (node == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return node;
    }

    @Override
    public Long create(TreeNode node) {
        if (node.getParentId() == null) {
            node.setParentId(0L);
        }
        if (node.getSort() == null) {
            node.setSort(0);
        }
        save(node);
        return node.getId();
    }

    @Override
    public void update(TreeNode node) {
        if (super.getById(node.getId()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        updateById(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 收集所有子孙节点 ID
        List<Long> toDelete = new ArrayList<>();
        toDelete.add(id);
        collectDescendants(id, toDelete);
        // 批量逻辑删除
        removeByIds(toDelete);
    }

    /** 递归收集所有子孙节点 ID */
    private void collectDescendants(Long parentId, List<Long> ids) {
        List<TreeNode> children = list(new LambdaQueryWrapper<TreeNode>()
                .eq(TreeNode::getParentId, parentId));
        for (TreeNode child : children) {
            ids.add(child.getId());
            collectDescendants(child.getId(), ids);
        }
    }

    @Override
    public void move(Long id, Long newParentId, Integer sort) {
        TreeNode node = getById(id);
        // 禁止移动到自己的子孙下面（会成环）
        if (newParentId != null && newParentId != 0L) {
            List<Long> descendants = new ArrayList<>();
            collectDescendants(id, descendants);
            if (descendants.contains(newParentId) || newParentId.equals(id)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "不能移动到自身或子孙节点下");
            }
        }
        node.setParentId(newParentId == null ? 0L : newParentId);
        if (sort != null) {
            node.setSort(sort);
        }
        updateById(node);
    }
}
