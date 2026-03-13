package com.appsinnova.admin.business.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisExecute {

    private JedisPool jedisPool;

    public RedisExecute(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public <T> T execute(RedisExecuteCommand<T> command) {
        Jedis jedis = this.jedisPool.getResource();

        Object data;
        try {
            data = command.executeCommand(jedis);
        } finally {
            jedis.close();
        }

        return (T) data;
    }
}
