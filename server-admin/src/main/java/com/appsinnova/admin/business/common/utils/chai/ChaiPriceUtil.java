package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.common.enums.base.YesOrNo;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 价格计算与列表展示（SPU / SKU）
 */
public final class ChaiPriceUtil {

    /** 官方价须大于该值才计算「折」，避免默认 1 元算出异常折扣 */
    private static final BigDecimal DISCOUNT_MIN_OFFICIAL = BigDecimal.ONE;

    private ChaiPriceUtil() {
    }

    public static boolean isNonSale(Integer nonSale) {
        return YesOrNo.isYes(nonSale);
    }

    public static String plain(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    /** 列表官方价：非卖品显示文案；否则去尾零；null 显示 "-" */
    public static String formatOfficialPrice(Integer nonSale, BigDecimal officialPrice) {
        if (isNonSale(nonSale)) {
            return "非卖品";
        }
        if (officialPrice == null) {
            return "-";
        }
        return plain(officialPrice);
    }

    /** @deprecated 请使用 {@link #formatOfficialPrice(Integer, BigDecimal)} */
    public static String formatOfficialPrice(BigDecimal officialPrice) {
        return formatOfficialPrice(0, officialPrice);
    }

    public static void fillSpuListShow(ChaiSpu spu) {
        if (spu == null) {
            return;
        }
        spu.setOfficialPriceShow(formatOfficialPrice(spu.getNonSale(), spu.getOfficialPrice()));
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
        sku.setOfficialPriceShow(formatOfficialPrice(sku.getNonSale(), sku.getOfficialPrice()));
        BigDecimal officialForDiscount = isNonSale(sku.getNonSale()) ? null : sku.getOfficialPrice();
        sku.setSalePriceShow(formatWithDiscount(sku.getSalePrice(), officialForDiscount));
        sku.setRecyclePriceShow(formatWithDiscount(sku.getRecyclePrice(), officialForDiscount));
        sku.setRecycleReduceAmountShow(plain(recycleAfterDamage(sku.getRecyclePrice(), sku.getRecyclePriceReducePer())));
        sku.setRecyclePriceReduceNoBagShow(plain(sku.getRecyclePriceReduceNoBag()));
    }

    /**
     * 保存前规范化：非卖品强制清空官价；非非卖品时校验官价必填。
     */
    public static void normalizeAndValidateOfficial(Integer nonSale, BigDecimal officialPrice,
                                                    java.util.function.Consumer<BigDecimal> setOfficial,
                                                    java.util.function.Consumer<Integer> setNonSale) {
        boolean flag = isNonSale(nonSale);
        setNonSale.accept(flag ? 1 : 0);
        if (flag) {
            setOfficial.accept(null);
            return;
        }
        if (officialPrice == null || officialPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("官方价必填且须大于0");
        }
        setOfficial.accept(officialPrice);
    }
}
