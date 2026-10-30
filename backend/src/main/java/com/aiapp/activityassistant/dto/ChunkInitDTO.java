package com.aiapp.activityassistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分片上传初始化参数：客户端只报文件名和大小，分片大小由服务端统一决定，
 * 避免不同客户端切分出无法合并的分片。
 */
@Data
@Schema(description = "分片上传初始化")
public class ChunkInitDTO {

    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 文件说明（可选，上传时填写的用途说明） */
    @Size(max = 500, message = "文件说明不能超过 500 字")
    private String description;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于 0")
    private Long fileSize;
}
