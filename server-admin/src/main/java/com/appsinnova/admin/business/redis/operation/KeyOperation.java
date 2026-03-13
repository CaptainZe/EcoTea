package com.appsinnova.admin.business.redis.operation;

public interface KeyOperation {
    void del(String key);

    boolean exists(String key);

    void expire(String key, int seconds);
}
