package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.entity.TreeNode;
import com.aiapp.activityassistant.service.TreeNodeService;
import com.aiapp.activityassistant.vo.TreeNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 树形数据结构：通用树节点的增删改查 + 拖拽移动
 */
@Tag(name = "树形数据结构")
@RestController
@RequestMapping("/tree")
@RequiredArgsConstructor
public class TreeNodeController {

    private final TreeNodeService treeNodeService;

    @Operation(summary = "获取完整树结构")
    @GetMapping
    public Result<List<TreeNodeVO>> getTree() {
        return Result.success(treeNodeService.getTree());
    }

    @Operation(summary = "获取节点详情")
    @GetMapping("/{id}")
    public Result<TreeNode> getById(@PathVariable Long id) {
        return Result.success(treeNodeService.getById(id));
    }

    @Operation(summary = "新增节点")
    @PostMapping
    public Result<Long> create(@RequestBody TreeNode node) {
        return Result.success(treeNodeService.create(node));
    }

    @Operation(summary = "更新节点")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TreeNode node) {
        node.setId(id);
        treeNodeService.update(node);
        return Result.success();
    }

    @Operation(summary = "删除节点（含子孙）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        treeNodeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "移动节点（拖拽调整层级）")
    @PutMapping("/{id}/move")
    public Result<Void> move(@PathVariable Long id,
                             @RequestParam Long parentId,
                             @RequestParam(required = false) Integer sort) {
        treeNodeService.move(id, parentId, sort);
        return Result.success();
    }
}
