package com.aiapp.activityassistant.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 树节点视图对象：带 children 的树形结构
 */
@Data
public class TreeNodeVO {

    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private Integer sort;
    private String description;
    private LocalDateTime createTime;

    /** 子节点列表 */
    private List<TreeNodeVO> children;
}
