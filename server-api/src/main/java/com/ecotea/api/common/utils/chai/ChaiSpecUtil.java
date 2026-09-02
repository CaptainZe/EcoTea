package com.ecotea.api.common.utils.chai;

import com.ecotea.api.common.utils.DictUtils;
import com.ecotea.api.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * chai 规格 JSON → 展示串（对齐 admin ChaiSpecUtil.toShow）。
 */
public final class ChaiSpecUtil {

    public static final String SPEC_LABEL_DICT = "CHAI_SPEC_LABEL";

    private ChaiSpecUtil() {
    }

    public static Map<String, Object> parseSpec(String specJson) {
        if (!StringUtils.hasText(specJson)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = JsonUtils.readValue(specJson, new TypeReference<Map<String, Object>>() {});
        return map != null ? map : new LinkedHashMap<>();
    }

    public static String toShow(String specJson) {
        Map<String, Object> map = parseSpec(specJson);
        BigDecimal total = getDecimal(map, "total_net_weight");
        BigDecimal unitWeight = getDecimal(map, "unit_weight");
        Integer unitCount = getInt(map, "unit_count");
        Integer unitLabel = getInt(map, "unit_label");
        String labelText = unitLabel == null ? ""
                : DictUtils.keyValue(SPEC_LABEL_DICT, String.valueOf(unitLabel));
        if (labelText == null) {
            labelText = "";
        }
        return formatShow(total, unitWeight, unitCount, labelText);
    }

    public static String formatShow(BigDecimal totalNetWeight, BigDecimal unitWeight,
                                    Integer unitCount, String unitLabelText) {
        if (totalNetWeight == null || unitWeight == null || unitCount == null) {
            return "";
        }
        String label = StringUtils.hasText(unitLabelText) ? unitLabelText : "";
        return formatWeight(totalNetWeight) + "g(" + formatWeight(unitWeight) + "g*" + unitCount + label + ")";
    }

    private static Integer getInt(Map<String, Object> map, String key) {
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

    private static BigDecimal getDecimal(Map<String, Object> map, String key) {
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

    private static String formatWeight(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }
}
