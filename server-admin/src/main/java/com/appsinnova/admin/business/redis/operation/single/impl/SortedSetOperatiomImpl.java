package com.appsinnova.admin.business.redis.operation.single.impl;

import com.appsinnova.admin.business.redis.RedisExecute;
import com.appsinnova.admin.business.redis.operation.SortedSetOperation;
import redis.clients.jedis.Tuple;

import java.util.Set;

public class SortedSetOperatiomImpl implements SortedSetOperation {
    private RedisExecute redisExecute;

    public SortedSetOperatiomImpl(RedisExecute redisExecute) {
        this.redisExecute = redisExecute;
    }

    public void zadd(String key, double score, String member) {
        this.redisExecute.execute((jedis) -> {
            return jedis.zadd(key, score, member);
        });
    }

    public long zcard(String key) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zcard(key);
        });
    }

    public long zcount(String key, double min, double max) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zcount(key, min, max);
        });
    }

    public Set<String> zrange(String key, long start, long end) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrange(key, start, end);
        });
    }

    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrangeWithScores(key, start, end);
        });
    }

    public Set<String> zrangeByScore(String key, double min, double max) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrangeByScore(key, min, max);
        });
    }

    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrangeByScore(key, min, max, offset, count);
        });
    }

    public long zrank(String key, String member) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zrank(key, member);
        });
    }

    public long zrem(String key, String... members) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zrem(key, members);
        });
    }

    public long zremrangeByScore(String key, double min, double max) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zremrangeByScore(key, min, max);
        });
    }

    public long zremrangeByRank(String key, long start, long end) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zremrangeByRank(key, start, end);
        });
    }

    public Set<String> zrevrange(String key, long start, long end) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrevrange(key, start, end);
        });
    }

    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrevrangeWithScores(key, start, end);
        });
    }

    public Set<String> zrevrangeByScore(String key, double min, double max) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrevrangeByScore(key, min, max);
        });
    }

    public Set<String> zrevrangeByScore(String key, double min, double max, int offset, int count) {
        return (Set)this.redisExecute.execute((jedis) -> {
            return jedis.zrevrangeByScore(key, min, max, offset, count);
        });
    }

    public long zrevrank(String key, String member) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zrevrank(key, member);
        });
    }

    public long zinterstore(String dstkey, String... sets) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zinterstore(dstkey, sets);
        });
    }

    public long zunionstore(String dstkey, String... sets) {
        return (Long)this.redisExecute.execute((jedis) -> {
            return jedis.zunionstore(dstkey, sets);
        });
    }

    public double zscore(String key, String member) {
        return (Double)this.redisExecute.execute((jedis) -> {
            return jedis.zscore(key, member);
        });
    }

    public double zincrby(String key, double score, String member) {
        return (Double)this.redisExecute.execute((jedis) -> {
            return jedis.zincrby(key, score, member);
        });
    }
}
