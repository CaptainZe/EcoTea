package com.ecotea.api.common.utils;

import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.config.RedisTemplateHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 基础工具（多实例：通过 {@link #getRedis(String)} / {@link #defaultRedis()} 取 Template）
 */
@Slf4j
public final class RedisUtils {

    private RedisUtils() {
    }

    public static StringRedisTemplate defaultRedis() {
        return RedisTemplateHolder.getDefault();
    }

    public static StringRedisTemplate getRedis(String name) {
        return RedisTemplateHolder.get(name);
    }

    public static void expire(StringRedisTemplate redis, String key, int expireSec) {
        if (redis == null) {
            log.error("RedisUtils.expire({}) redis is null", key);
            return;
        }
        try {
            if (expireSec > RedisConstant.NO_EXPIRE) {
                redis.expire(key, expireSec, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("RedisUtils.expire({}) Exception: {}", key, e.getMessage());
        }
    }

    public static void delete(StringRedisTemplate redis, String key) {
        if (redis == null) {
            log.error("RedisUtils.delete({}) redis is null", key);
            return;
        }
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.error("RedisUtils.delete({}) Exception: {}", key, e.getMessage());
        }
    }

    public static void set(StringRedisTemplate redis, String key, String value) {
        set(redis, key, value, RedisConstant.NO_EXPIRE);
    }

    public static void set(StringRedisTemplate redis, String key, String value, int expireSec) {
        if (redis == null) {
            log.error("RedisUtils.set({}) redis is null", key);
            return;
        }
        try {
            if (expireSec > RedisConstant.NO_EXPIRE) {
                redis.opsForValue().set(key, value, expireSec, TimeUnit.SECONDS);
            } else {
                redis.opsForValue().set(key, value);
            }
        } catch (Exception e) {
            log.error("RedisUtils.set({}) Exception: {}", key, e.getMessage());
        }
    }

    public static String get(StringRedisTemplate redis, String key) {
        if (redis == null) {
            log.error("RedisUtils.get({}) redis is null", key);
            return null;
        }
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.error("RedisUtils.get({}) Exception: {}", key, e.getMessage());
            return null;
        }
    }

    public static boolean exists(StringRedisTemplate redis, String key) {
        if (redis == null) {
            log.error("RedisUtils.exists({}) redis is null", key);
            return false;
        }
        try {
            Boolean result = redis.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("RedisUtils.exists({}) Exception: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * SET key value NX EX，成功返回 true
     */
    public static boolean setIfAbsent(StringRedisTemplate redis, String key, String value, int expireSec) {
        if (redis == null) {
            log.error("RedisUtils.setIfAbsent({}) redis is null", key);
            return false;
        }
        try {
            Boolean result;
            if (expireSec > RedisConstant.NO_EXPIRE) {
                result = redis.opsForValue().setIfAbsent(key, value, expireSec, TimeUnit.SECONDS);
            } else {
                result = redis.opsForValue().setIfAbsent(key, value);
            }
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("RedisUtils.setIfAbsent({}) Exception: {}", key, e.getMessage());
            return false;
        }
    }
}
