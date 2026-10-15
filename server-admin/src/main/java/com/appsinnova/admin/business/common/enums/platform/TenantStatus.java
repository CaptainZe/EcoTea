package com.appsinnova.admin.business.common.enums.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 店铺租户状态（与 PRD / SQL_TENANT 一致：0停用 1启用）
 */
@Getter
@AllArgsConstructor
public enum TenantStatus {
    DISABLED(0, "停用"),
    ENABLED(1, "启用"),
    ;

    private final Integer code;
    private final String message;

    public static boolean isEnabled(Integer code) {
        return ENABLED.code.equals(code);
    }
}
