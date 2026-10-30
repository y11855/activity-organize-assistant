package com.aiapp.activityassistant.entity;

import com.aiapp.activityassistant.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 活动资料文件（参与者上传的策划书、PPT、小票照片等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_file")
public class ActivityFile extends BaseEntity {

    private Long activityId;

    /** 上传人 ID */
    private Long uploaderId;

    /** 原始文件名（可直接用于下载时的展示名） */
    private String name;

    /** 文件说明（上传人填写，便于其他成员了解用途） */
    private String description;

    /** 扩展名（小写，不含点） */
    private String ext;

    /** 文件字节数 */
    private Long size;

    /** 文件整体 SHA-256，用于下载校验与 ETag */
    private String sha256;

    /** 相对上传根目录的存储路径，如 activity-files/202610/xxx.pptx */
    private String storagePath;
}
