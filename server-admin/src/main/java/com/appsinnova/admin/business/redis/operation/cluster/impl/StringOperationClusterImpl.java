package com.appsinnova.admin.business.redis.operation.cluster.impl;

import com.appsinnova.admin.business.redis.operation.StringOperation;
import redis.clients.jedis.JedisCluster;

import java.util.Optional;

public class StringOperationClusterImpl implements StringOperation {
    private JedisCluster jedisCluster;

    public StringOperationClusterImpl(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public Optional<String> get(String key) {
        String data = this.jedisCluster.get(key);
        return Optional.ofNullable(data);
    }

    public void set(String key, String value) {
        this.jedisCluster.set(key, value);
    }

    public long incr(String key) {
        return this.jedisCluster.incr(key);
    }

    public long incrBy(String key, int increment) {
        return this.jedisCluster.incrBy(key, (long)increment);
    }

    public long decr(String key) {
        return this.jedisCluster.decr(key);
    }

    public long decrBy(String key, int increment) {
        return this.jedisCluster.decrBy(key, (long)increment);
    }

    public Optional<String> getSet(String key, String value) {
        String data = this.jedisCluster.getSet(key, value);
        return Optional.ofNullable(data);
    }

    public void setex(String key, String value, int expired) {
        this.jedisCluster.setex(key, expired, value);
    }

    public boolean setexnx(String key, String value, int expired) {
        String code = this.jedisCluster.set(key, value, "NX", "EX", (long)expired);
        return code != null && code.equals("OK");
    }

    public void append(String key, String value) {
        this.jedisCluster.append(key, value);
    }

    public void setbit(String key, long offset, String value) {
        this.jedisCluster.setbit(key, offset, value);
    }
}
