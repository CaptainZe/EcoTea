package com.appsinnova.admin.business.redis.operation;

import redis.clients.jedis.Tuple;

import java.util.Set;

public interface SortedSetOperation {
    void zadd(String key, double score, String member);

    long zcard(String key);

    long zcount(String key, double min, double max);

    Set<String> zrange(String key, long start, long end);

    Set<Tuple> zrangeWithScores(String key, long start, long end);

    Set<String> zrangeByScore(String key, double min, double max);

    Set<String> zrangeByScore(String key, double min, double max, int offset, int count);

    long zrank(String key, String member);

    long zrem(String key, String... members);

    long zremrangeByScore(String key, double min, double max);

    long zremrangeByRank(String key, long start, long end);

    Set<String> zrevrange(String key, long start, long end);

    Set<Tuple> zrevrangeWithScores(String key, long start, long end);

    Set<String> zrevrangeByScore(String key, double min, double max);

    Set<String> zrevrangeByScore(String key, double min, double max, int offset, int count);

    long zrevrank(String key, String member);

    long zinterstore(String dstkey, String... sets);

    long zunionstore(String dstkey, String... sets);

    double zscore(String key, String member);

    double zincrby(String key, double score, String member);
}
