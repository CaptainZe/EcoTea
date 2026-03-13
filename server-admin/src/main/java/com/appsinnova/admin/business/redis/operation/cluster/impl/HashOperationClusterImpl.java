package com.appsinnova.admin.business.redis.operation.cluster.impl;

import com.appsinnova.admin.business.redis.operation.HashOperation;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class HashOperationClusterImpl implements HashOperation {
    private JedisCluster jedisCluster;

    public HashOperationClusterImpl(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public void hdel(String key, String... fields) {
        this.jedisCluster.hdel(key, fields);
    }

    public boolean hexists(String key, String field) {
        return this.jedisCluster.hexists(key, field);
    }

    public Optional<String> hget(String key, String field) {
        return Optional.ofNullable(this.jedisCluster.hget(key, field));
    }

    public Map<String, String> hgetall(String key) {
        return this.jedisCluster.hgetAll(key);
    }

    public long hincrby(String key, String field, long increment) {
        return this.jedisCluster.hincrBy(key, field, increment);
    }

    public Set<String> hkeys(String key) {
        return this.jedisCluster.hkeys(key);
    }

    public long hlen(String key) {
        return this.jedisCluster.hlen(key);
    }

    public void hset(String key, String field, String value) {
        this.jedisCluster.hset(key, field, value);
    }

    public long hsetnx(String key, String field, String value) {
        return this.jedisCluster.hsetnx(key, field, value);
    }

    public List<String> hvals(String key) {
        return this.jedisCluster.hvals(key);
    }

    public void hmset(String key, Map<String, String> values) {
        this.jedisCluster.hmset(key, values);
    }

    public List<String> hmget(String key, String... fields) {
        return this.jedisCluster.hmget(key, fields);
    }
}
