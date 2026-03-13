package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.ListOperation;

import java.util.List;
import java.util.Optional;

public class ListOperationImpl implements ListOperation {
    private RedisExecute redisExecute;

    public ListOperationImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public Optional<String> blpop(String key, int timeout) {
        return Optional.ofNullable(((List<String>)this.redisExecute.execute((jedis) -> {
            return jedis.blpop(timeout, key);
        })).get(0));
    }

    public Optional<String> rpop(String key) {
        return Optional.ofNullable(this.redisExecute.execute((jedis) -> {
            return jedis.rpop(key);
        }));
    }

    public Optional<String> lpop(String key) {
        return Optional.ofNullable(this.redisExecute.execute((jedis) -> {
            return jedis.lpop(key);
        }));
    }

    public Optional<String> brpop(String key, int timeout) {

        return Optional.ofNullable(((List<String>)this.redisExecute.execute((jedis) -> {
            return jedis.brpop(timeout, key);
        })).get(0));
    }

    public long llen(String key) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.llen(key);
        });
    }

    public List<String> lrange(String key, long start, long end) {
        return (List)this.redisExecute.execute((jedis) -> {
            return jedis.lrange(key, start, end);
        });
    }

    public void lset(String key, long index, String value) {
        this.redisExecute.execute((jedis) -> {
            return jedis.lset(key, index, value);
        });
    }

    public void rpush(String key, String... value) {
        this.redisExecute.execute((jedis) -> {
            return jedis.rpush(key, value);
        });
    }

    public void lpush(String key, String... value) {
        this.redisExecute.execute((jedis) -> {
            return jedis.lpush(key, value);
        });
    }

    public Optional<String> lindex(String key, long index) {
        return Optional.ofNullable(this.redisExecute.execute((jedis) -> {
            return jedis.lindex(key, index);
        }));
    }
}
