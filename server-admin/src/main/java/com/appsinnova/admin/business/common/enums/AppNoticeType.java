package com.appsinnova.admin.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppNoticeType {
    // 茶叶相关
    TEA_SHIPPING_ADDRESS(100, "回收地址"),
    TEA_PLATFORM_RULES(101, "平台规则"),
    TEA_ORDER_GUIDE(102, "报单指南"),
    ;

    private Integer code;
    private String message;
}