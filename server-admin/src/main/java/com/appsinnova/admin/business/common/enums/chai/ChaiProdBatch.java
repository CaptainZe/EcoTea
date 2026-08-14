package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生产批次（与字典 CHAI_PROD_BATCH 一致）
 */
@Getter
@AllArgsConstructor
public enum ChaiProdBatch {
    FIRST_HALF(100, "上半年"),
    SECOND_HALF(200, "下半年"),
    ;

    private final Integer code;
    private final String message;

    /**
     * 非法值归一为上半年，保持半年推算可继续
     */
    public static ChaiProdBatch fromCode(Integer code) {
        if (SECOND_HALF.code.equals(code)) {
            return SECOND_HALF;
        }
        return FIRST_HALF;
    }
}
