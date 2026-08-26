package com.appsinnova.admin.business.common.utils.chai;

import java.math.BigDecimal;

/**
 * 无提袋扣减默认值：按回收价区间（左闭右开）推算。
 */
public final class ChaiRecycleNoBagUtil {

    private ChaiRecycleNoBagUtil() {
    }

    /**
     * @param recyclePrice 回收价；null 或无效时返回 0
     */
    public static BigDecimal resolve(BigDecimal recyclePrice) {
        if (recyclePrice == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(resolveAmount(recyclePrice.doubleValue()));
    }

    /**
     * 与前端 editBySpu、历史数据 SQL 区间保持一致。
     */
    public static int resolveAmount(double recyclePrice) {
        if (recyclePrice < 100) {
            return 0;
        }
        if (recyclePrice < 500) {
            return 10;
        }
        if (recyclePrice < 1000) {
            return 15;
        }
        if (recyclePrice < 1500) {
            return 25;
        }
        if (recyclePrice < 2000) {
            return 35;
        }
        if (recyclePrice < 3000) {
            return 40;
        }
        if (recyclePrice < 4000) {
            return 45;
        }
        return 50;
    }
}
