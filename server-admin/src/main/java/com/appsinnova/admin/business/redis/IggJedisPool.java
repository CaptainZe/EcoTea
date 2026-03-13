package com.appsinnova.admin.business.redis;

import org.springframework.beans.factory.annotation.Value;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;

//@Configuration
public class IggJedisPool {
    private static JedisPool pool;//jedis连接池

    @Value("${redispool.max.total}")
    private  Integer maxTotal ; //最大连接数

    @Value("${redispool.max.idle}")
    private  Integer maxIdle ;//在jedispool中最大的idle状态(空闲的)的jedis实例的个数

    @Value("${redispool.min.idle}")
    private Integer minIdle ;//在jedispool中最小的idle状态(空闲的)的jedis实例的个数

    @Value("${redispool.test.borrow}")
    private  Boolean testOnBorrow ;//在borrow一个jedis实例的时候，是否要进行验证操作，如果赋值true。则得到的jedis实例肯定是可以用的。

    @Value("${redispool.test.return}")
    private Boolean testOnReturn ;//在return一个jedis实例的时候，是否要进行验证操作，如果赋值true。则放回jedispool的jedis实例肯定是可以用的。

    @Value("${redispool.timeout}")
    private  Integer timeout ;

    @Value("${redispool.redis1.ip}")
    private  String redisIp ;

    @Value("${redispool.redis1.port}")
    private  Integer redisPort;

    @PostConstruct
    private  void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);

        config.setBlockWhenExhausted(true);//连接耗尽的时候，是否阻塞，false会抛出异常，true阻塞直到超时。默认为true。

        pool = new JedisPool(config,redisIp,redisPort,timeout);
    }

    public static Jedis getJedis(){
        return pool.getResource();
    }

    /**
     * 每次操作完需要将jedis对象归还到连接池中
     * @param jedis
     */
    public static void returnJedis(Jedis jedis) {
        if(jedis != null){
            jedis.close();
        }
    }
}
