package com.appsinnova.admin.business.redis;

import com.appsinnova.admin.business.redis.operation.StringSingleOperation;
import com.appsinnova.admin.business.redis.operation.single.impl.*;

import java.util.List;

public class JedisSingleTemplate extends JedisTemplate implements StringSingleOperation {
    private StringSingleOperation stringSingleOperation;

    public JedisSingleTemplate(RedisExecute redisExecute) {
        super(new KeyOperationImpl(redisExecute), new StringOperationImpl(redisExecute), new HashOperationImpl(redisExecute), new ListOperationImpl(redisExecute), new SetOperatiomImpl(redisExecute), new SortedSetOperatiomImpl(redisExecute) );
        this.stringSingleOperation = new StringSingleOperationImpl(redisExecute);
    }

    public List<String> mget(String... keys) {
        return this.stringSingleOperation.mget(keys);
    }

    public void mset(String... keysvalues) {
        this.stringSingleOperation.mset(keysvalues);
    }
}
