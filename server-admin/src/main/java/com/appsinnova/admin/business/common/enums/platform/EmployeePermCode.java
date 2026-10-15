package com.appsinnova.admin.business.common.enums.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 小程序员工店内权限码（代码枚举，不落库字典；与工作台分组对齐）
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

    public static EmployeePermCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EmployeePermCode item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }

    /** 首任店长勾选模板（不落库） */
    public static List<String> managerTemplateCodes() {
        return Arrays.stream(values()).map(EmployeePermCode::getCode).collect(Collectors.toList());
    }
}
