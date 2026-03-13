package com.appsinnova.admin.business.redis.operation.cluster.impl;

import com.appsinnova.admin.business.redis.operation.KeyOperation;
import redis.clients.jedis.JedisCluster;

public class KeyOperationClusterImpl implements KeyOperation {
    private JedisCluster jedisCluster;

    public KeyOperationClusterImpl(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public void del(String key) {
        this.jedisCluster.del(key);
    }

    public boolean exists(String key) {
        return this.jedisCluster.exists(key);
    }

    public void expire(String key, int seconds) {
        this.jedisCluster.expire(key, seconds);
    }
}
