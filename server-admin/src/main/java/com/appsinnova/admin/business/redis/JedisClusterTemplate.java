package com.appsinnova.admin.business.redis;

import com.appsinnova.admin.business.redis.operation.cluster.impl.*;
import redis.clients.jedis.JedisCluster;

public class JedisClusterTemplate extends JedisTemplate {
    public JedisClusterTemplate(JedisCluster jedisCluster) {
        super(new KeyOperationClusterImpl(jedisCluster), new StringOperationClusterImpl(jedisCluster), new HashOperationClusterImpl(jedisCluster),
                new ListOperationClusterImpl(jedisCluster), new SetOperationClusterImpl(jedisCluster), new SortedSetOperationClusterImpl(jedisCluster));
    }
}
