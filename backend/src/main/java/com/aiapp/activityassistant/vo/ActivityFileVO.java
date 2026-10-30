package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "活动资料文件")
public class ActivityFileVO {

    private Long id;

    private Long activityId;

    @Schema(description = "原始文件名")
    private String name;

    @Schema(description = "文件说明")
    private String description;

    private String ext;

    @Schema(description = "文件大小（字节）")
    private Long size;

    private Long uploaderId;

    @Schema(description = "上传人昵称")
    private String uploaderName;

    @Schema(description = "整体 SHA-256")
    private String sha256;

    @Schema(description = "静态访问地址（图片可直接预览）")
    private String url;

    /** 当前用户是否可删除：活动创建者可删全部，普通成员仅可删自己上传的 */
    @Schema(description = "是否可删除")
    private Boolean canDelete;

    private LocalDateTime createTime;
}
