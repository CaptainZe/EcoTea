package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSku;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SKU 价格计算与列表展示
 */
public final class ChaiPriceUtil {

    /** 官方价须大于该值才计算「折」，避免默认 1 元算出异常折扣 */
    private static final BigDecimal DISCOUNT_MIN_OFFICIAL = BigDecimal.ONE;

    private ChaiPriceUtil() {
    }

    public static String plain(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    /**
     * 金额，官方价有效时追加「(x折)」。算法与旧 tea 一致：price × 10 / officialPrice。
     */
    public static String formatWithDiscount(BigDecimal price, BigDecimal officialPrice) {
        if (price == null) {
            return "";
        }
        String show = plain(price);
        if (officialPrice != null && officialPrice.compareTo(DISCOUNT_MIN_OFFICIAL) > 0) {
            BigDecimal discount = price.multiply(BigDecimal.TEN)
                    .divide(officialPrice, 2, RoundingMode.HALF_UP);
            show += " (" + discount.stripTrailingZeros().toPlainString() + "折)";
        }
        return show;
    }

    /**
     * 外观破损后的回收价：回收价 × (1 - 压价%)
     */
    public static BigDecimal recycleAfterDamage(BigDecimal recyclePrice, Integer reducePer) {
        if (recyclePrice == null || reducePer == null) {
            return null;
        }
        BigDecimal reduceAmount = recyclePrice
                .multiply(BigDecimal.valueOf(reducePer))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return recyclePrice.subtract(reduceAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public static void fillListShow(ChaiSku sku) {
        if (sku == null) {
            return;
        }
        sku.setOfficialPriceShow(plain(sku.getOfficialPrice()));
        sku.setSalePriceShow(formatWithDiscount(sku.getSalePrice(), sku.getOfficialPrice()));
        sku.setRecyclePriceShow(formatWithDiscount(sku.getRecyclePrice(), sku.getOfficialPrice()));
        sku.setRecycleReduceAmountShow(plain(recycleAfterDamage(sku.getRecyclePrice(), sku.getRecyclePriceReducePer())));
        sku.setRecyclePriceReduceNoBagShow(plain(sku.getRecyclePriceReduceNoBag()));
    }
}
