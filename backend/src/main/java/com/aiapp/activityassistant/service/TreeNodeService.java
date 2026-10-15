package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.entity.TreeNode;
import com.aiapp.activityassistant.vo.TreeNodeVO;

import java.util.List;

/**
 * 树节点服务
 */
public interface TreeNodeService {

    /** 获取完整树结构 */
    List<TreeNodeVO> getTree();

    /** 获取单个节点详情 */
    TreeNode getById(Long id);

    /** 新增节点 */
    Long create(TreeNode node);

    /** 更新节点 */
    void update(TreeNode node);

    /** 删除节点（含所有子孙，逻辑删除） */
    void delete(Long id);

    /** 移动节点到新父节点下 */
    void move(Long id, Long newParentId, Integer sort);
}
