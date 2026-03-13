package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.StringOperation;
import com.appsinnova.admin.business.redis.operation.StringSingleOperation;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

public class StringOperationImpl implements StringOperation, StringSingleOperation {
    private RedisExecute redisExecute;

    public StringOperationImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public Optional<String> get(String key) {
        String data = (String)this.redisExecute.execute((jedis) -> {
            return jedis.get(key);
        });
        return Optional.ofNullable(data);
    }

    public void set(String key, String value) {
        this.redisExecute.execute((jedis) -> {
            return jedis.set(key, value);
        });
    }

    public long incr(String key) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.incr(key);
        });
    }

    public long incrBy(String key, int increment) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.incrBy(key, (long)increment);
        });
    }

    public long decr(String key) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.decr(key);
        });
    }

    public long decrBy(String key, int increment) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.decrBy(key, (long)increment);
        });
    }

    public Optional<String> getSet(String key, String value) {
        String data = (String)this.redisExecute.execute((jedis) -> {
            return jedis.getSet(key, value);
        });
        return Optional.ofNullable(data);
    }

    public void setex(String key, String value, int expired) {
        this.redisExecute.execute((jedis) -> {
            return jedis.setex(key, expired, value);
        });
    }

    public boolean setexnx(String key, String value, int expired) {
        String code = (String)this.redisExecute.execute((jedis) -> {
            return jedis.set(key, value, "NX", "EX", expired);
        });
        return code == null ? false : code.equals("OK");
    }

    public void append(String key, String value) {
    }

    public List<String> mget(String... keys) {
        return (List)this.redisExecute.execute((jedis) -> {
            return jedis.mget(keys);
        });
    }

    public void setbit(String key, long offset, String value) {
        this.redisExecute.execute((jedis) -> {
            return jedis.setbit(key, offset, value);
        });
    }

    public void mset(String... keysvalues) {
        Assert.isTrue(keysvalues.length / 2 == 0, "keysvalues 键值对有误");
        this.redisExecute.execute((jedis) -> {
            return jedis.mset(keysvalues);
        });
    }
}

