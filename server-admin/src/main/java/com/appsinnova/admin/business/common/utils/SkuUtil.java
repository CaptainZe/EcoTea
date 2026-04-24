package com.appsinnova.admin.business.common.utils;

import com.appsinnova.admin.common.utils.DictUtils;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SkuUtil {

    // 生成茶叶SKU编码
    public static String genTeaSkuCode(Integer brand, Long skuId) {
        String skuCodeTemp = "TEA-%s-%s";

        Map<String, String> brandMap = DictUtils.value("TEA_BRAND");
        Map<String, String> brandCodeMap = toPinyinInitialMap(brandMap);
        String brandCode = brandCodeMap.get(String.valueOf(brand));

        String skuUid = String.format("%08d", skuId);
        return  String.format(skuCodeTemp, brandCode, skuUid);
    }

    // 中文 Map 转拼音首字母 Map
    private static Map<String, String> toPinyinInitialMap(Map<String, String> input) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, Integer> counter = new HashMap<>();

        for (Map.Entry<String, String> entry : input.entrySet()) {
            String initial = getInitials(entry.getValue());

            // 处理重复
            int count = counter.getOrDefault(initial, 0) + 1;
            counter.put(initial, count);

            if (count > 1) {
                initial = initial + count;
            }

            result.put(entry.getKey(), initial);
        }

        return result;
    }

    // 获取中文拼音首字母
    private static String getInitials(String chinese) {
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.UPPERCASE);

        for (char c : chinese.toCharArray()) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                try {
                    String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyin != null && pinyin.length > 0) {
                        sb.append(pinyin[0].charAt(0));
                    }
                } catch (Exception e) {
                    // 忽略异常字符
                }
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }
}