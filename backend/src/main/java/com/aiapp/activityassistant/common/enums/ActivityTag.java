package com.aiapp.activityassistant.common.enums;

/**
 * 活动属性标签：创建活动时由发起人必选，发布后报名问卷页展示，并作为推荐算法的兴趣维度。
 */
public enum ActivityTag {

    SPORTS("sports", "运动"),
    ART("art", "艺术"),
    ACADEMIC("academic", "学术"),
    PUBLIC_WELFARE("public_welfare", "公益"),
    SOCIAL("social", "社交"),
    ENTERTAINMENT("entertainment", "娱乐"),
    OTHER("other", "其他");

    private final String code;
    private final String label;

    ActivityTag(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 编码是否合法（空值不合法，标签为必选项）
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (ActivityTag tag : values()) {
            if (tag.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 编码转中文名，未知编码返回空串（兼容历史活动未打标签的数据）
     */
    public static String labelOf(String code) {
        for (ActivityTag tag : values()) {
            if (tag.code.equals(code)) {
                return tag.label;
            }
        }
        return "";
    }
}
