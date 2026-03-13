package com.appsinnova.admin.business.redis.operation.cluster.impl;

import com.appsinnova.admin.business.redis.operation.SortedSetOperation;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Tuple;

import java.util.Set;

public class SortedSetOperationClusterImpl implements SortedSetOperation {
    private JedisCluster jedisCluster;

    public SortedSetOperationClusterImpl(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public void zadd(String key, double score, String member) {
        this.jedisCluster.zadd(key, score, member);
    }

    public long zcard(String key) {
        return this.jedisCluster.zcard(key);
    }

    public long zcount(String key, double min, double max) {
        return this.jedisCluster.zcount(key, min, max);
    }

    public Set<String> zrange(String key, long start, long end) {
        return this.jedisCluster.zrange(key, start, end);
    }

    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return this.jedisCluster.zrangeWithScores(key, start, end);
    }

    public Set<String> zrangeByScore(String key, double min, double max) {
        return this.jedisCluster.zrangeByScore(key, min, max);
    }

    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return this.jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    public long zrank(String key, String member) {
        return this.jedisCluster.zrank(key, member);
    }

    public long zrem(String key, String... members) {
        return this.jedisCluster.zrem(key, members);
    }

    public long zremrangeByScore(String key, double min, double max) {
        return this.jedisCluster.zremrangeByScore(key, min, max);
    }

    public long zremrangeByRank(String key, long start, long end) {
        return this.jedisCluster.zremrangeByRank(key, start, end);
    }

    public Set<String> zrevrange(String key, long start, long end) {
        return this.jedisCluster.zrevrange(key, start, end);
    }

    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return this.jedisCluster.zrevrangeWithScores(key, start, end);
    }

    public Set<String> zrevrangeByScore(String key, double min, double max) {
        return this.jedisCluster.zrevrangeByScore(key, min, max);
    }

    public Set<String> zrevrangeByScore(String key, double min, double max, int offset, int count) {
        return this.jedisCluster.zrevrangeByScore(key, min, max, offset, count);
    }

    public long zrevrank(String key, String member) {
        return this.jedisCluster.zrevrank(key, member);
    }

    public long zinterstore(String dstkey, String... sets) {
        return this.jedisCluster.zinterstore(dstkey, sets);
    }

    public long zunionstore(String dstkey, String... sets) {
        return this.jedisCluster.zunionstore(dstkey, sets);
    }

    public double zscore(String key, String member) {
        return this.jedisCluster.zscore(key, member);
    }

    public double zincrby(String key, double score, String member) {
        return this.jedisCluster.zincrby(key, score, member);
    }
}