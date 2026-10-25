package com.aiapp.activityassistant.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一分页查询参数。
 *
 * <p>用于跨活动的「我的 xxx」列表；按活动限定的子资源列表保持返回数组，不做分页。</p>
 */
@Data
public class PageQuery {

    /** 每页最大条数，避免一次拉取过多数据 */
    private static final long MAX_SIZE = 100L;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Long page = 1L;

    @Schema(description = "每页条数，默认 10，上限 100", defaultValue = "10")
    private Long size = 10L;

    public Long getPage() {
        return page == null || page < 1 ? 1L : page;
    }

    public Long getSize() {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, MAX_SIZE);
    }
}
