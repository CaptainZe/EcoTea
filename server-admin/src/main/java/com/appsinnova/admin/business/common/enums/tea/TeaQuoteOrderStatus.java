package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报价单状态，字典 TEA_QUOTE_ORDER_STATUS
 */
@Getter
@AllArgsConstructor
public enum TeaQuoteOrderStatus {
    SUBMITTED(100, "已提交"),
    CONFIRMED(200, "已确认"),
    ACCEPTED(300, "已验收"),
    PAID(400, "已打款"),
    CANCELLED(500, "已取消"),
    ;

    private final Integer code;
    private final String message;
}
