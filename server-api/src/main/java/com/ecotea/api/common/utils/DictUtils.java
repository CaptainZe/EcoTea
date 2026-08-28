package com.ecotea.api.common.utils;

import com.ecotea.api.domain.sys.SysDict;
import com.ecotea.api.service.sys.SysDictService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字典工具（sys_dict），仅保留 value / keyValue
 */
public final class DictUtils {

    private DictUtils() {
    }

    /**
     * 获取字典值集合（格式：code:text,code:text）
     */
    public static Map<String, String> value(String label) {
        SysDictService dictService = SpringContextUtil.getBean(SysDictService.class);
        SysDict dict = dictService.getByNameOk(label);
        if (dict == null || dict.getValue() == null) {
            return Collections.emptyMap();
        }
        Map<String, String> value = new LinkedHashMap<>();
        String[] outerSplit = dict.getValue().split(",");
        for (String osp : outerSplit) {
            String[] split = osp.split(":");
            if (split.length > 1) {
                value.put(split[0], split[1]);
            }
        }
        return value;
    }

    /**
     * 根据选项编码获取选项值
     */
    public static String keyValue(String label, String code) {
        Map<String, String> map = value(label);
        return map.get(code);
    }
}
