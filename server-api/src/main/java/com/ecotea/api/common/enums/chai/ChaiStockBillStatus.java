package com.ecotea.api.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存单据状态（与 admin ChaiStockBillStatus / 字典 CHAI_STOCK_BILL_STATUS 对齐）。
 * 1 已过账；2 作废；3 归档。
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

    /**
     * 计入历史筛选的状态（已过账、归档；排除作废）。
     */
    public static List<Integer> effectiveCodes() {
        return Collections.unmodifiableList(Arrays.asList(POSTED.code, ARCHIVED.code));
    }

    /** 供 SQL IN (...) 拼接 */
    public static String effectiveCodesCsv() {
        return effectiveCodes().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
