package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存单据类型（与字典 CHAI_STOCK_BILL_TYPE 一致）
 */
@Getter
@AllArgsConstructor
public enum ChaiStockBillType {
    IN(1, "入库", "CHAI-SBRK"),
    OUT(2, "出库", "CHAI-SBCK"),
    TRANSFER(3, "调拨", "CHAI-SBDB"),
    ;

    private final Integer code;
    private final String message;
    private final String billNoPrefix;

    public static ChaiStockBillType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChaiStockBillType item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
