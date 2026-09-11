package com.ecotea.api.common.utils.chai;

import com.ecotea.api.common.enums.base.YesOrNo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 销售价展示（对齐 admin ChaiPriceUtil 折扣算法）。
 */
public final class ChaiPriceUtil {

    private static final BigDecimal DISCOUNT_MIN_OFFICIAL = BigDecimal.ONE;

    private ChaiPriceUtil() {
    }

    public static boolean isNonSale(Integer nonSale) {
        return YesOrNo.YES.getCode().equals(nonSale);
    }

    public static String plain(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    public static String formatOfficialPrice(Integer nonSale, BigDecimal officialPrice) {
        if (isNonSale(nonSale)) {
            return "非卖品";
        }
        if (officialPrice == null) {
            return "-";
        }
        return plain(officialPrice) + "元";
    }

    /**
     * 折扣文案，如「3.5折」；非卖品、官方价无效或售价为空时返回 null。算法：sale × 10 / official。
     */
    public static String formatDiscountShow(Integer nonSale, BigDecimal salePrice, BigDecimal officialPrice) {
        if (isNonSale(nonSale)) {
            return null;
        }
        if (salePrice == null || officialPrice == null
                || officialPrice.compareTo(DISCOUNT_MIN_OFFICIAL) <= 0) {
            return null;
        }
        BigDecimal discount = salePrice.multiply(BigDecimal.TEN)
                .divide(officialPrice, 2, RoundingMode.HALF_UP);
        return discount.stripTrailingZeros().toPlainString() + "折";
    }

    /**
     * 特价展示；非卖品不追加折扣；官方价有效时追加「(x折)」。
     */
    public static String formatSaleWithDiscount(Integer nonSale, BigDecimal salePrice, BigDecimal officialPrice) {
        if (salePrice == null) {
            return "-";
        }
        String show = plain(salePrice) + "元";
        String discountShow = formatDiscountShow(nonSale, salePrice, officialPrice);
        if (discountShow != null) {
            show += "(" + discountShow + ")";
        }
        return show;
    }
}
