package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.SetOperation;
import com.appsinnova.admin.business.redis.operation.SetSingleOperation;

import java.util.Optional;
import java.util.Set;

public class SetOperatiomImpl implements SetOperation, SetSingleOperation {
    private RedisExecute redisExecute;

    public SetOperatiomImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public void sadd(String key, String... values) {
        this.redisExecute.execute((jedis) -> {
            return jedis.sadd(key, values);
        });
    }

    public long scard(String key) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.scard(key);
        });
    }

    public boolean sismember(String key, String value) {
        return (Boolean)this.redisExecute.execute((jedis) -> {
            return jedis.sismember(key, value);
        });
    }

    public Set<String> smembers(String key) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.smembers(key);
        });
    }

    public long srem(String key, String... values) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.srem(key, values);
        });
    }

    public Optional<String> spop(String key) {
        return Optional.ofNullable(this.redisExecute.execute((jedis) -> {
            return jedis.spop(key);
        }));
    }

    public Set<String> sdiff(String... keys) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.sdiff(keys);
        });
    }

    public Set<String> sunion(String... keys) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.sunion(keys);
        });
    }

    public long sunionstore(String key, String... keys) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.sunionstore(key, keys);
        });
    }
}
