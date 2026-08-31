package com.appsinnova.admin.business.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 分布式锁（对齐 server-api；语义：true=已锁住/拿不到，false=获取成功可继续）
 */
@Slf4j
public final class LockUtils {

    private LockUtils() {
    }

    /**
     * @param expireSec 锁过期秒数
     * @return true 表示已上锁（拿不到）；false 表示获取成功
     */
    public static boolean lock(String lockKey, int expireSec) {
        return lock(RedisUtils.defaultRedis(), lockKey, expireSec);
    }

    public static boolean lock(StringRedisTemplate redis, String lockKey, int expireSec) {
        try {
            if (redis == null) {
                log.error("LockUtils.lock error. 无效的 redis 实例.");
                return true;
            }
            return !RedisUtils.setIfAbsent(redis, lockKey, "1", expireSec);
        } catch (Exception e) {
            log.error("LockUtils.lock error. Exception: {}", e.getMessage());
            return true;
        }
    }

    public static void unLock(String lockKey) {
        unLock(RedisUtils.defaultRedis(), lockKey);
    }

    public static void unLock(StringRedisTemplate redis, String lockKey) {
        try {
            if (redis != null) {
                RedisUtils.delete(redis, lockKey);
            }
        } catch (Exception e) {
            log.error("LockUtils.unLock error. Exception: {}", e.getMessage());
        }
    }
}
