package com.appsinnova.admin.business.redis.operation;

import java.util.List;
import java.util.Optional;

public interface ListOperation {
    Optional<String> blpop(String key, int timeout);

    Optional<String> rpop(String key);

    Optional<String> lpop(String key);

    Optional<String> brpop(String key, int timeout);

    long llen(String key);

    List<String> lrange(String key, long start, long end);

    void lset(String key, long index, String value);

    void rpush(String key, String... value);

    void lpush(String key, String... value);

    Optional<String> lindex(String key, long index);
}
