package com.ecotea.api.common.enums.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用是/否（0/1）。与 admin YesOrNo 码值对齐。
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

    public static boolean isYes(Integer code) {
        return YES.code.equals(code);
    }

    public static boolean isNo(Integer code) {
        return NO.code.equals(code);
    }
}
