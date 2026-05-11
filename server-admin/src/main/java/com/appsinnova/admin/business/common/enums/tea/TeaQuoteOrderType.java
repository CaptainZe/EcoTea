package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报价单类型，字典 TEA_QUOTE_ORDER_TYPE
 */
@Getter
@AllArgsConstructor
public enum TeaQuoteOrderType {
    Self(100, "自建"),
    Proxy(200, "代建"),
    ;

    private final Integer code;
    private final String message;
}
