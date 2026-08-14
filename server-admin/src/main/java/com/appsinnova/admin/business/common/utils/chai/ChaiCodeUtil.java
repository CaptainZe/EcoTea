package com.appsinnova.admin.business.common.utils.chai;

/**
 * chai 域 SPU / SKU 业务编码
 * <ul>
 *   <li>SPU：{@code CHAI-{spuId 8位}}</li>
 *   <li>SKU：{@code CHAI-{spuId 8位}-{skuId 8位}}</li>
 * </ul>
 */
public final class ChaiCodeUtil {

    private ChaiCodeUtil() {
    }

    public static String spuCode(Long spuId) {
        if (spuId == null) {
            throw new IllegalArgumentException("spuId不能为空");
        }
        return String.format("CHAI-%08d", spuId);
    }

    public static String skuCode(Long spuId, Long skuId) {
        if (spuId == null) {
            throw new IllegalArgumentException("spuId不能为空");
        }
        if (skuId == null) {
            throw new IllegalArgumentException("skuId不能为空");
        }
        return String.format("CHAI-%08d-%08d", spuId, skuId);
    }
}
