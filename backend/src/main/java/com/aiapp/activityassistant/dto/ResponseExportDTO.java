package com.aiapp.activityassistant.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出 DTO：报名人员 Excel 导出模型（EasyExcel 列定义）
 */
@Data
public class ResponseExportDTO {

    @ExcelProperty("序号")
    private Integer index;

    @ExcelProperty("填写人姓名")
    private String respondentName;

    @ExcelProperty("报名状态")
    private String status;

    @ExcelProperty("回答内容")
    private String answer;

    @ExcelProperty("提交时间")
    private LocalDateTime submitTime;
}
