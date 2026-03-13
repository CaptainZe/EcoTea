package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.StringSingleOperation;

import java.util.List;

public class StringSingleOperationImpl implements StringSingleOperation {
    private RedisExecute redisExecute;

    public StringSingleOperationImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public List<String> mget(String... keys) {
        return (List)this.redisExecute.execute((jedis) -> {
            return jedis.mget(keys);
        });
    }

    public void mset(String... keysvalues) {
        this.redisExecute.execute((jedis) -> {
            return jedis.mset(keysvalues);
        });
    }
}
