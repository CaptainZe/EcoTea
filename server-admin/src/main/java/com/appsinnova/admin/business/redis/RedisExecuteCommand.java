package com.appsinnova.admin.business.redis;

import redis.clients.jedis.Jedis;

public interface RedisExecuteCommand<T> {
    T executeCommand(Jedis jedis);
}

