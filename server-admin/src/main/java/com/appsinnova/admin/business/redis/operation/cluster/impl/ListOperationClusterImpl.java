package com.appsinnova.admin.business.redis.operation.cluster.impl;

import com.appsinnova.admin.business.redis.operation.ListOperation;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Optional;

public class ListOperationClusterImpl implements ListOperation {
    private JedisCluster jedisCluster;

    public ListOperationClusterImpl(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public Optional<String> blpop(String key, int timeout) {
        return Optional.ofNullable(this.jedisCluster.blpop(timeout, key).get(0));
    }

    public Optional<String> brpop(String key, int timeout) {
        return Optional.ofNullable(this.jedisCluster.brpop(timeout, key).get(0));
    }

    public long llen(String key) {
        return this.jedisCluster.llen(key);
    }

    public List<String> lrange(String key, long start, long end) {
        return this.jedisCluster.lrange(key, start, end);
    }

    public void lset(String key, long index, String value) {
        this.jedisCluster.lset(key, index, value);
    }

    public Optional<String> rpop(String key) {
        return Optional.ofNullable(this.jedisCluster.rpop(key));
    }

    public void rpush(String key, String... value) {
        this.jedisCluster.rpush(key, value);
    }

    public Optional<String> lpop(String key) {
        return Optional.ofNullable(this.jedisCluster.lpop(key));
    }

    public void lpush(String key, String... value) {
        this.jedisCluster.lpush(key, value);
    }

    public Optional<String> lindex(String key, long index) {
        return Optional.ofNullable(this.jedisCluster.lindex(key, index));
    }
}
