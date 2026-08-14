package com.appsinnova.admin.business.common.utils.chai;

import com.appsinnova.admin.common.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * chai 表单/页面共用工具（年份选项、图片 URL 列表等）
 */
public final class ChaiFormHelper {

    /** 默认：当前年 + 近 5 年，共 6 个 */
    public static final int DEFAULT_YEAR_OPTION_COUNT = 6;

    /**
     * 供前端页面 JS 使用的驼峰 JSON（勿用 {@link JsonUtils}，其全局为 SNAKE_CASE）
     */
    private static final ObjectMapper CAMEL_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private ChaiFormHelper() {
    }

    public static List<Integer> buildYearOptions() {
        return buildYearOptions(DEFAULT_YEAR_OPTION_COUNT);
    }

    /**
     * @param count 选项数量（含当前年）
     */
    public static List<Integer> buildYearOptions(int count) {
        int size = count <= 0 ? DEFAULT_YEAR_OPTION_COUNT : count;
        int current = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            years.add(current - i);
        }
        return years;
    }

    public static List<String> parseUrlList(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JsonUtils.readValue(json, new TypeReference<List<String>>() {});
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 序列化为驼峰 JSON，供 Thymeleaf 页面内 JS 读取
     */
    public static String toCamelJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return CAMEL_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "null";
        }
    }
}
