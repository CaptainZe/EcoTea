package com.appsinnova.admin.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DailySeqType {
    QUOTE_ORDER_NO(1, "报价单号使用"),
    ;

    private Integer code;
    private String message;
}