package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.KeyOperation;

public class KeyOperationImpl implements KeyOperation {
    private RedisExecute redisExecute;

    public KeyOperationImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public void del(String key) {
        this.redisExecute.execute((jedis) -> {
            return jedis.del(key);
        });
    }

    public boolean exists(String key) {
        return (Boolean)this.redisExecute.execute((jedis) -> {
            return jedis.exists(key);
        });
    }

    public void expire(String key, int seconds) {
        this.redisExecute.execute((jedis) -> {
            return jedis.expire(key, seconds);
        });
    }
}