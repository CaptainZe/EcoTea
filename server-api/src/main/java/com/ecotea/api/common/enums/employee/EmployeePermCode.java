package com.ecotea.api.common.enums.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 小程序员工店内权限码（与 Admin / 工作台分组对齐）
 */
@Getter
@AllArgsConstructor
public enum EmployeePermCode {
    SALE("SALE", "销售"),
    RECYCLE("RECYCLE", "回收"),
    STOCK("STOCK", "库存"),
    FINANCE("FINANCE", "财务"),
    SHOP("SHOP", "店铺管理"),
    ;

    private final String code;
    private final String message;

    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (EmployeePermCode item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
