package com.appsinnova.admin.business.redis;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * redis分布式锁
     * @param key
     * @param value 当前时间+超时时间
     * @return
     */
    public boolean lock(String key, String value) {
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value))) {
            return true;
        }
        String currentValue = redisTemplate.opsForValue().get(key);
        //如果锁过期
        if (StringUtils.isNotEmpty(currentValue)
            && (Long.parseLong(currentValue) < System.currentTimeMillis())) {
            //获取上一个锁的值,多个过来只能有一个值和原来的一样
            String oldValue = redisTemplate.opsForValue().getAndSet(key, value);
            if (StringUtils.isNotEmpty(oldValue) && oldValue.equals(currentValue)) {
                log.info("recycle redis check lock expire");
                return true;
            }
        }
        return false;
    }

    /**
     * 解锁
     * @param key
     * @param value
     */
    public void unlock(String key, String value) {
        try {
            String currentValue = redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotEmpty(currentValue) && currentValue.equals(value)){
                redisTemplate.opsForValue().getOperations().delete(key);
            }
            log.info("redis unlock success");
        } catch (RuntimeException e) {
            log.error("redis unlock fail, RuntimeException: ", e);
        }
    }
}
