package com.ecotea.api.config;

import com.ecotea.api.common.constant.RedisConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多 Redis 实例注册表。当前只注册默认实例，后续可按名称追加。
 */
@Configuration
@RequiredArgsConstructor
public class RedisTemplateHolder {

    private static final Map<String, StringRedisTemplate> TEMPLATE_MAP = new ConcurrentHashMap<>();

    private final StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        TEMPLATE_MAP.put(RedisConstant.DEFAULT_REDIS, stringRedisTemplate);
    }

    public static void register(String name, StringRedisTemplate template) {
        if (name != null && template != null) {
            TEMPLATE_MAP.put(name, template);
        }
    }

    public static StringRedisTemplate get(String name) {
        return TEMPLATE_MAP.get(name);
    }

    public static StringRedisTemplate getDefault() {
        return TEMPLATE_MAP.get(RedisConstant.DEFAULT_REDIS);
    }
}
