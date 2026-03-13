package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.HashOperation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class HashOperationImpl implements HashOperation {
    private RedisExecute redisExecute;

    public HashOperationImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public void hdel(String key, String... fields) {
        this.redisExecute.execute((jedis) -> {
            return jedis.hdel(key, fields);
        });
    }

    public boolean hexists(String key, String field) {
        return (Boolean)this.redisExecute.execute((jedis) -> {
            return jedis.hexists(key, field);
        });
    }

    public Optional<String> hget(String key, String field) {
        return Optional.ofNullable(this.redisExecute.execute((jedis) -> {
            return jedis.hget(key, field);
        }));
    }

    public Map<String, String> hgetall(String key) {
        return (Map)this.redisExecute.execute((jedis) -> {
            return jedis.hgetAll(key);
        });
    }

    public long hincrby(String key, String field, long increment) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.hincrBy(key, field, increment);
        });
    }

    public Set<String> hkeys(String key) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.hkeys(key);
        });
    }

    public long hlen(String key) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.hlen(key);
        });
    }

    public void hset(String key, String field, String value) {
        this.redisExecute.execute((jedis) -> {
            return jedis.hset(key, field, value);
        });
    }

    public long hsetnx(String key, String field, String value) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.hsetnx(key, field, value);
        });
    }

    public List<String> hvals(String key) {
        return (List)this.redisExecute.execute((jedis) -> {
            return jedis.hvals(key);
        });
    }

    public void hmset(String key, Map<String, String> values) {
        this.redisExecute.execute((jedis) -> {
            return jedis.hmset(key, values);
        });
    }

    public List<String> hmget(String key, String... fields) {
        return (List)this.redisExecute.execute((jedis) -> {
            return jedis.hmget(key, fields);
        });
    }
}