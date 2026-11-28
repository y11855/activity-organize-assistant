package com.aiapp.activityassistant.common.log;

import java.util.regex.Pattern;

/**
 * 日志文本处理工具：截断超长内容、脱敏敏感字段。
 *
 * <p>日志表字段有限，且不应保存密码等凭据，因此写入前统一走这里处理。</p>
 */
public final class LogTextUtils {

    /** 单条日志文本最大长度（请求参数、响应、异常信息共用） */
    public static final int MAX_TEXT_LENGTH = 2000;

    /**
     * 敏感字段匹配：JSON 中的 password / code / token 等字段值替换为 ******。
     * 兼容大小写，只处理「字符串值」，不影响正常业务数据。
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|confirmPassword|code|captchaCode|token)\"\\s*:\\s*\")[^\"]*(\")",
            Pattern.CASE_INSENSITIVE);

    private LogTextUtils() {
    }

    /** 敏感字段脱敏 */
    public static String maskSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return SENSITIVE_PATTERN.matcher(text).replaceAll("$1******$2");
    }

    /** 按默认长度截断 */
    public static String truncate(String text) {
        return truncate(text, MAX_TEXT_LENGTH);
    }

    /** 按指定长度截断，超出部分用省略号标记 */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
