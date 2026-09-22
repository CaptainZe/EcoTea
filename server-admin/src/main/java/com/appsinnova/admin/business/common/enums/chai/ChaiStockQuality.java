package com.appsinnova.admin.business.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存品相（单据行 quality / 结存分桶）。
 * 完整件数 = 总数 − 无提袋 − 破损 − 破损无提袋，不单独落库。
 */
@Getter
@AllArgsConstructor
public enum ChaiStockQuality {
    /** 完整（外观完整有提袋） */
    INTACT(1, "完整"),
    /** 外观完整无提袋 */
    NO_BAG(2, "无提袋"),
    /** 外观破损有提袋 */
    DAMAGED(3, "破损"),
    /** 外观破损无提袋 */
    DAMAGED_NO_BAG(4, "破损无袋"),
    ;

    private final Integer code;
    private final String message;

    public static ChaiStockQuality fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChaiStockQuality item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 当前品相可出/可调件数。
     * buckets: [qty, qtyNoBag, qtyDamaged, qtyDamagedNoBag]
     */
    public int availableOf(int[] buckets) {
        if (buckets == null || buckets.length < 4) {
            return 0;
        }
        int qty = Math.max(0, buckets[0]);
        int noBag = Math.max(0, buckets[1]);
        int damaged = Math.max(0, buckets[2]);
        int damagedNoBag = Math.max(0, buckets[3]);
        switch (this) {
            case NO_BAG:
                return noBag;
            case DAMAGED:
                return damaged;
            case DAMAGED_NO_BAG:
                return damagedNoBag;
            case INTACT:
            default:
                return Math.max(0, qty - noBag - damaged - damagedNoBag);
        }
    }

    public String insufficientMessage() {
        switch (this) {
            case NO_BAG:
                return "无提袋库存不足";
            case DAMAGED:
                return "破损库存不足";
            case DAMAGED_NO_BAG:
                return "破损无袋库存不足";
            case INTACT:
            default:
                return "完整库存不足";
        }
    }

    public String conflictMessage() {
        return insufficientMessage() + "或并发冲突，请重试";
    }
}
