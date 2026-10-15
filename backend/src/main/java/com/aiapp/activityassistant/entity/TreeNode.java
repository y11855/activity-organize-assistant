package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 树节点：通用树形数据结构，支持任意层级的父子关系
 * （用于演示树形数据结构可视化：部门架构、目录分类等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tree_node")
public class TreeNode extends BaseEntity {

    /** 父节点 ID，根节点为 0 */
    private Long parentId;

    /** 节点名称 */
    private String name;

    /** 节点类型：folder / leaf / item */
    private String type;

    /** 同级排序，越小越靠前 */
    private Integer sort;

    /** 节点描述/备注 */
    private String description;
}
