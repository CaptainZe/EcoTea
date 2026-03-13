package com.appsinnova.admin.business.redis.operation;

import java.util.Optional;
import java.util.Set;

public interface SetOperation {
    void sadd(String key, String... values);

    long scard(String key);

    boolean sismember(String key, String value);

    Set<String> smembers(String key);

    long srem(String key, String... values);

    Optional<String> spop(String key);
}
