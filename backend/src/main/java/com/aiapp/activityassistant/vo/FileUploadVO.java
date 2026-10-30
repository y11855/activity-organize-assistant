package com.aiapp.activityassistant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "单文件直传结果")
public class FileUploadVO {

    @Schema(description = "资料记录 ID（未关联活动时为空）")
    private Long id;

    private String name;

    private Long size;

    @Schema(description = "静态访问地址")
    private String url;
}
