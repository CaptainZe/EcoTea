package com.appsinnova.admin.business.redis.operation;

import java.util.Optional;

public interface StringOperation {
    Optional<String> get(String key);

    void set(String key, String value);

    long incr(String key);

    long incrBy(String key, int increment);

    long decr(String key);

    long decrBy(String key, int increment);

    Optional<String> getSet(String key, String value);

    void setex(String key, String value, int expired);

    boolean setexnx(String key, String value, int expired);

    void append(String key, String value);

    void setbit(String key, long offset, String value);
}
