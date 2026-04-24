package com.appsinnova.admin.common.utils;

import com.appsinnova.admin.component.thymeleaf.utility.DictUtil;
import com.appsinnova.admin.system.domain.Dict;
import com.appsinnova.admin.system.service.DictService;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字典工具类
 *
 * @author xie.zy
 * @date 2023/8/18
 */
public class DictUtils {

    /**
     * 获取字典值集合
     *
     * @param label 字典标识
     */
    public static Map<String, String> value(String label) {
        Map<String, String> value = null;
        DictService dictService = SpringContextUtil.getBean(DictService.class);
        Dict dict = dictService.getByNameOk(label);
        if (dict != null) {
            String dictValue = dict.getValue();
            String[] outerSplit = dictValue.split(",");
            value = new LinkedHashMap<>();
            for (String osp : outerSplit) {
                String[] split = osp.split(":");
                if (split.length > 1) {
                    value.put(split[0], split[1]);
                }
            }
        }
        return value;
    }

    public static String getValue(String label) {
        String value = "";
        DictService dictService = SpringContextUtil.getBean(DictService.class);
        Dict dict = dictService.getByNameOk(label);
        if (dict != null) {
            value = dict.getValue();
        }

        return value;
    }

    /**
     * 根据选项编码获取选项值
     *
     * @param label 字典标识
     * @param code  选项编码
     */
    public static String keyValue(String label, String code) {
        Map<String, String> list = DictUtils.value(label);
        return list != null ? list.get(code) : "";
    }

    public static Long keyValueForLong(String label, String code, Long defaultValue) {
        String valueStr = DictUtils.keyValue(label, code);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Long.valueOf(valueStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Integer keyValueForInt(String label, String code, Integer defaultValue) {
        String valueStr = DictUtils.keyValue(label, code);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Integer.valueOf(valueStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void updateValue(String label, Map<String, String> valueMap) {
        String value = "";
        if (valueMap != null && !valueMap.isEmpty()) {
            for (String key : valueMap.keySet()) {
                if (StringUtils.isNotEmpty(value)) {
                    value += ",";
                }
                value += key;
                value += ":";
                value += valueMap.get(key);
            }
        }

        updateValue(label, value);
    }

    public static void updateValue(String label, String value) {
        DictService dictService = SpringContextUtil.getBean(DictService.class);
        Dict dict = dictService.getByNameOk(label);
        dict.setValue(value);
        dictService.updateDictValue(dict);
        if (dict.getId() != null) {
            DictUtil.clearCache(dict.getName());
        }
    }

    public static void updateValue(String label, String code, String value) {
        DictService dictService = SpringContextUtil.getBean(DictService.class);
        Dict dict = dictService.getByNameOk(label);
        boolean update = false;
        if (dict != null) {
            String dictValue = dict.getValue();
            String newValue = "";
            String[] outerSplit = dictValue.split(",");
            for (String osp : outerSplit) {
                String[] split = osp.split(":");
                if (split.length > 1) {
                    if (!newValue.isEmpty())
                        newValue += ",";
                    if (split[0] != null && split[0].equals(code)) {
                        String newOsp = split[0] + ":" + value;
                        newValue += newOsp;
                        update = true;
                    } else {
                        newValue += osp;
                    }
                }
            }

            if (update) {
                dict.setValue(newValue.trim());
                dictService.updateDictValue(dict);
                if (dict.getId() != null) {
                    DictUtil.clearCache(dict.getName());
                }
            }
        }
    }
}
