package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * chai 域通用状态（与字典 CHAI_STATUS 一致）
 */
@Getter
@AllArgsConstructor
public enum ChaiStatus {
    OFFLINE(0, "下架"),
    ONLINE(1, "上架"),
    ;

    private final Integer code;
    private final String message;
}
