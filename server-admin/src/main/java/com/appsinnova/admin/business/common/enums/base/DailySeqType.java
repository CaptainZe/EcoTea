package com.appsinnova.admin.business.common.enums.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DailySeqType {
    QUOTE_ORDER_NO(1, "报价单号使用"),
    CHAI_STOCK_IN(10, "茶叶库存入库单号"),
    CHAI_STOCK_OUT(11, "茶叶库存出库单号"),
    CHAI_STOCK_TRANSFER(12, "茶叶库存调拨单号"),
    ;

    private Integer code;
    private String message;
}