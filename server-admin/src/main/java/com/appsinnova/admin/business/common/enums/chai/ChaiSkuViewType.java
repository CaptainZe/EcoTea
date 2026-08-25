package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 茶叶价目只读视图类型
 */
@Getter
@AllArgsConstructor
public enum ChaiSkuViewType {
    INTERNAL("完整价目", false),
    SALES("销售价目", false),
    RECYCLE("回收价目", true),
    ;

    private final String title;
    /** 是否强制「当前时间向前 6 个半年」 */
    private final boolean recentHalfYearOnly;
}
