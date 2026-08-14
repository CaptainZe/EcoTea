package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * chai SPU/SKU 规格 JSON 工具
 * 结构：total_net_weight / unit_weight / unit_count / unit_label
 */
public final class ChaiSpecUtil {

    private ChaiSpecUtil() {
    }

    public static String buildSpecJson(BigDecimal totalNetWeight, BigDecimal unitWeight,
                                       Integer unitCount, Integer unitLabel) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_net_weight", totalNetWeight);
        map.put("unit_weight", unitWeight);
        map.put("unit_count", unitCount);
        map.put("unit_label", unitLabel);
        return JsonUtils.writeValueAsString(map);
    }

    public static Map<String, Object> parseSpec(String specJson) {
        if (StringUtils.isBlank(specJson)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = JsonUtils.readValue(specJson, new TypeReference<Map<String, Object>>() {});
        return map != null ? map : new LinkedHashMap<>();
    }

    public static Integer getInt(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return null;
        }
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(val));
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal getDecimal(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return null;
        }
        Object val = map.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return new BigDecimal(val.toString());
        }
        try {
            return new BigDecimal(String.valueOf(val));
        } catch (Exception e) {
            return null;
        }
    }

    public static void fillSpecFields(ChaiSpu spu) {
        if (spu == null) {
            return;
        }
        Map<String, Object> map = parseSpec(spu.getSpec());
        spu.setTotalNetWeight(getDecimal(map, "total_net_weight"));
        spu.setUnitWeight(getDecimal(map, "unit_weight"));
        spu.setUnitCount(getInt(map, "unit_count"));
        spu.setUnitLabel(getInt(map, "unit_label"));
    }

    public static void fillSpecFields(ChaiSku sku) {
        if (sku == null) {
            return;
        }
        Map<String, Object> map = parseSpec(sku.getSpec());
        sku.setTotalNetWeight(getDecimal(map, "total_net_weight"));
        sku.setUnitWeight(getDecimal(map, "unit_weight"));
        sku.setUnitCount(getInt(map, "unit_count"));
        sku.setUnitLabel(getInt(map, "unit_label"));
    }

    /**
     * 展示串，如 136g(4g*34泡) 或 7.5g(1.5g*5泡)
     */
    public static String formatShow(BigDecimal totalNetWeight, BigDecimal unitWeight,
                                    Integer unitCount, String unitLabelText) {
        if (totalNetWeight == null || unitWeight == null || unitCount == null) {
            return "";
        }
        String label = StringUtils.isNotBlank(unitLabelText) ? unitLabelText : "";
        return formatWeight(totalNetWeight) + "g(" + formatWeight(unitWeight) + "g*" + unitCount + label + ")";
    }

    private static String formatWeight(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }
}
