package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 外观情况：1-外观完整 2-外观破损
 */
@Getter
@AllArgsConstructor
public enum AppearanceCondition {
    COMPLETE(1, "外观完整"),
    DAMAGED(2, "外观破损"),
    ;

    private final Integer code;
    private final String message;
}
