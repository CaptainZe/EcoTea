package com.ecotea.api.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 与 admin ChaiStatus / 字典 CHAI_STATUS 对齐。
 */
@Getter
@AllArgsConstructor
public enum ChaiStatus {
    OFFLINE(0, "下架"),
    ONLINE(1, "上架"),
    ;

    private final Integer code;
    private final String message;

    public static boolean isOnline(Integer code) {
        return ONLINE.code.equals(code);
    }

    public static boolean isOffline(Integer code) {
        return OFFLINE.code.equals(code);
    }
}
