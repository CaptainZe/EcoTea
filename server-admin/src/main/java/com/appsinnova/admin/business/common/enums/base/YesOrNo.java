package com.appsinnova.admin.business.common.enums.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用是/否（0/1）。未传或无法识别时返回 null（表示不限）。
 */
@Getter
@AllArgsConstructor
public enum YesOrNo {
    NO(0, "否"),
    YES(1, "是"),
    ;

    private final Integer code;
    private final String message;

    public static YesOrNo fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (YesOrNo item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static YesOrNo fromRequestParam(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        String raw = param.trim();
        for (YesOrNo item : values()) {
            if (String.valueOf(item.code).equals(raw)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 请求参数 → 查询用 Integer；未传/非法 → null（不限）
     */
    public static Integer parseQueryCode(String param) {
        YesOrNo item = fromRequestParam(param);
        return item == null ? null : item.getCode();
    }
}
