package com.appsinnova.admin.business.redis.operation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface HashOperation {
    void hdel(String key, String... fields);

    boolean hexists(String key, String field);

    Optional<String> hget(String key, String field);

    Map<String, String> hgetall(String key);

    long hincrby(String key, String field, long increment);

    Set<String> hkeys(String key);

    long hlen(String key);

    void hset(String key, String field, String value);

    long hsetnx(String key, String field, String value);

    List<String> hvals(String key);

    void hmset(String key, Map<String, String> values);

    List<String> hmget(String key, String... fields);
}