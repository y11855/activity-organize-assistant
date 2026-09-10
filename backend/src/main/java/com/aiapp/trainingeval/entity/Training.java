package com.aiapp.trainingeval.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实训记录实体
 */
@Data
@TableName("training")
public class Training implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long studentId;

    private String title;

    /** 实训成果文件 URL */
    private String fileUrl;

    /** 原始内容 */
    private String content;

    /** 状态: 0待审 1AI审核中 2审核完成 3已评分 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
