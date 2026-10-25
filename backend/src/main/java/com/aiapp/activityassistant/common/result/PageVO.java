package com.aiapp.activityassistant.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应结构。
 *
 * <p>
 * 分页元数据统一使用 Integer：项目全局把 Long 序列化为字符串（防雪花 ID 精度丢失），
 * 分页数值若用 Long 会变成 "10" 之类字符串，前端 el-pagination 需要数字。
 * </p>
 */
@Data
public class PageVO<T> implements Serializable {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private Integer total;

    /** 当前页码，从 1 开始 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    public PageVO() {
        this.records = Collections.emptyList();
        this.total = 0;
        this.page = 1;
        this.size = 10;
    }

    public PageVO(List<T> records, long total, long page, long size) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = (int) total;
        this.page = (int) page;
        this.size = (int) size;
    }

    public static <T> PageVO<T> of(List<T> records, long total, long page, long size) {
        return new PageVO<>(records, total, page, size);
    }
}
