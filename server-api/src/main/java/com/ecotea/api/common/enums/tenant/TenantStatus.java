package com.ecotea.api.common.enums.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
