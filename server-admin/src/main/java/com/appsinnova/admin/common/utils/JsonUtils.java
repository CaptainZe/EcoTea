package com.appsinnova.admin.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;

/**
 * Json工具类
 *
 * @author xie.zy
 * @date 2023/8/9
 */
public class JsonUtils {
    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String writeValueAsString(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static String writePrettyJson(Object obj) {
        try {
            String jsonString = mapper.writeValueAsString(obj);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            // 使用 JsonParser 解析 JSON 字符串
            JsonElement jsonElement = JsonParser.parseString(jsonString);
            return gson.toJson(jsonElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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
        return readValue(mapper, jsonstr, clazz);
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
        return readValue(mapper, jsonstr, reference);
    }

    /**
     * ex:jedisTemplate.setex(cacheKey, JsonUtils.writeValueAsString(userModel), RedisConstant.USER_CACHE_TIEM);
     * 由于使用JsonUtils.writeValueAsString(userModel)将对象转为字符串时, 如果对象为null, 则返回null;
     * 如果此时, 将null进行缓存, 缓存值为 "null",
     * 因此获取redis缓存值后, 需要做一次检查
     */
    public static <T> T readValueForCache(String cacheData, Class<T> clazz) {
        if ("null".equals(cacheData)) {
            return null;
        }

        return readValue(mapper, cacheData, clazz);
    }

    /**
     * ex:jedisTemplate.setex(cacheKey, JsonUtils.writeValueAsString(userModel), RedisConstant.USER_CACHE_TIEM);
     * 由于使用JsonUtils.writeValueAsString(userModel)将对象转为字符串时, 如果对象为null, 则返回null;
     * 如果此时, 将null进行缓存, 缓存值为 "null",
     * 因此获取redis缓存值后, 需要做一次检查
     */
    public static <T> T readValueForCache(String cacheData, TypeReference<T> reference) {
        if ("null".equals(cacheData)) {
            return null;
        }

        return readValue(mapper, cacheData, reference);
    }
}