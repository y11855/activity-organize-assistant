package com.aiapp.activityassistant.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 表单工具：创建报名问卷/表单
 */
@Component
public class FormTool implements Tool {

    @Override
    public String name() {
        return "form";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        // TODO: 接入表单服务（如自建问卷表 + 生成分享链接）
        // params: title, questions[], activityId
        return ToolResult.ok(Map.of(
                "formId", "form_" + System.currentTimeMillis(),
                "shareUrl", "https://example.com/form/" + System.currentTimeMillis()
        ));
    }
}
