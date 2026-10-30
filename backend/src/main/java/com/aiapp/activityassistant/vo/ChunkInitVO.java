package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "分片上传会话")
public class ChunkInitVO {

    @Schema(description = "上传会话 ID，后续上传/查询/合并都使用它")
    private String uploadId;

    @Schema(description = "服务端规定的分片大小（字节）")
    private Long chunkSize;

    @Schema(description = "总分片数")
    private Integer totalChunks;
}
