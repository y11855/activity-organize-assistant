package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 断点续传查询结果：客户端据此跳过已上传分片
 */
@Data
@Schema(description = "分片上传进度")
public class ChunkStatusVO {

    private String uploadId;

    private String fileName;

    private Long fileSize;

    private Long chunkSize;

    private Integer totalChunks;

    @Schema(description = "已上传成功（服务端已校验）的分片序号，从 0 开始")
    private List<Integer> uploadedChunks;

    @Schema(description = "是否已完成合并")
    private boolean merged;
}
