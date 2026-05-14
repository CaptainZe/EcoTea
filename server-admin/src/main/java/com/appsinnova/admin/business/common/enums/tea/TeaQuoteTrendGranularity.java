package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报价单仪表盘趋势粒度
 */
@Getter
@AllArgsConstructor
public enum TeaQuoteTrendGranularity {
    DAY("day", "按天"),
    WEEK("week", "按周"),
    MONTH("month", "按月"),
    ;

    private final String param;
    private final String message;

    public static TeaQuoteTrendGranularity fromParam(String raw) {
        if (raw == null) {
            return DAY;
        }
        String p = raw.trim().toLowerCase();
        for (TeaQuoteTrendGranularity e : values()) {
            if (e.param.equalsIgnoreCase(p)) {
                return e;
            }
        }
        return DAY;
    }
}
