package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存单据状态（与字典 CHAI_STOCK_BILL_STATUS 一致）。
 * 1 已过账可转作废/归档；2、3 为终态。
 */
@Getter
@AllArgsConstructor
public enum ChaiStockBillStatus {
    POSTED(1, "已过账"),
    VOIDED(2, "作废"),
    ARCHIVED(3, "归档"),
    ;

    private final Integer code;
    private final String message;

    public static ChaiStockBillStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChaiStockBillStatus item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
