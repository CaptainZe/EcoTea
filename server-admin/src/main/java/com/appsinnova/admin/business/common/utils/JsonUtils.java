package com.appsinnova.admin.business.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;

/**
 * Json 工具类（对齐 server-api；Boot 2.0 使用 PropertyNamingStrategy.SNAKE_CASE）
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JsonUtils() {
    }

    public static String writeValueAsString(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static <T> T readValue(ObjectMapper mapper, String jsonstr, Class<T> clazz) {
        try {
            if (jsonstr == null) {
                return null;
            }
            return mapper.readValue(jsonstr, clazz);
        } catch (IOException e) {
            return null;
        }
    }

    public static <T> T readValue(String jsonstr, Class<T> clazz) {
        return readValue(MAPPER, jsonstr, clazz);
    }

    public static <T> T readValue(ObjectMapper mapper, String jsonstr, TypeReference<T> reference) {
        try {
            if (jsonstr == null) {
                return null;
            }
            return mapper.readValue(jsonstr, reference);
        } catch (IOException e) {
            return null;
        }
    }

    public static <T> T readValue(String jsonstr, TypeReference<T> reference) {
        return readValue(MAPPER, jsonstr, reference);
    }

    public static JsonNode readTree(String jsonstr) {
        try {
            if (jsonstr == null) {
                return null;
            }
            return MAPPER.readTree(jsonstr);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 缓存值为字面量 "null" 时按空处理
     */
    public static <T> T readValueForCache(String cacheData, Class<T> clazz) {
        if ("null".equals(cacheData)) {
            return null;
        }
        return readValue(MAPPER, cacheData, clazz);
    }

    public static <T> T readValueForCache(String cacheData, TypeReference<T> reference) {
        if ("null".equals(cacheData)) {
            return null;
        }
        return readValue(MAPPER, cacheData, reference);
    }
}
