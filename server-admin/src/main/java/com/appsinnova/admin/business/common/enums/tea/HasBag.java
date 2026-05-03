package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 有无提袋：1-有提袋 2-无提袋
 */
@Getter
@AllArgsConstructor
public enum HasBag {
    YES(1, "有提袋"),
    NO(2, "无提袋"),
    ;

    private final Integer code;
    private final String message;
}
