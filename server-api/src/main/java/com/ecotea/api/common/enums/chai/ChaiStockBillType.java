package com.ecotea.api.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存单据类型（与 admin ChaiStockBillType / 字典 CHAI_STOCK_BILL_TYPE 对齐）。
 */
@Getter
@AllArgsConstructor
public enum ChaiStockBillType {
    IN(1, "入库"),
    OUT(2, "出库"),
    TRANSFER(3, "调拨"),
    ;

    private final Integer code;
    private final String message;
}
