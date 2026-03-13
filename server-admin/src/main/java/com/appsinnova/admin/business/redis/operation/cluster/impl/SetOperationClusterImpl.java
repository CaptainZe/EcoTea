package com.appsinnova.admin.business.redis.operation.cluster.impl;

import com.appsinnova.admin.business.redis.operation.SetOperation;
import redis.clients.jedis.JedisCluster;

import java.util.Optional;
import java.util.Set;

public class SetOperationClusterImpl implements SetOperation {
    private JedisCluster jedisCluster;

    public SetOperationClusterImpl(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public void sadd(String key, String... values) {
        this.jedisCluster.sadd(key, values);
    }

    public long scard(String key) {
        return this.jedisCluster.scard(key);
    }

    public boolean sismember(String key, String value) {
        return this.jedisCluster.sismember(key, value);
    }

    public Set<String> smembers(String key) {
        return this.jedisCluster.smembers(key);
    }

    public long srem(String key, String... values) {
        return this.jedisCluster.srem(key, values);
    }

    public Optional<String> spop(String key) {
        return Optional.ofNullable(this.jedisCluster.spop(key));
    }
}