package com.appsinnova.admin.business.redis;

import com.appsinnova.admin.business.redis.operation.*;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JedisTemplate implements HashOperation, StringOperation, KeyOperation, ListOperation, SetOperation, SortedSetOperation {

    private JedisCluster jedisCluster;
    private KeyOperation keyOperation;
    private StringOperation stringOperation;
    private HashOperation hashOperation;
    private ListOperation listOperation;
    private SetOperation setOperation;
    private SortedSetOperation sortedSetOperation;

    public JedisTemplate(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public JedisTemplate(KeyOperation keyOperation, StringOperation stringOperation, HashOperation hashOperation, ListOperation listOperation, SetOperation setOperation, SortedSetOperation sortedSetOperation) {
        this.keyOperation = keyOperation;
        this.stringOperation = stringOperation;
        this.hashOperation = hashOperation;
        this.listOperation = listOperation;
        this.setOperation = setOperation;
        this.sortedSetOperation = sortedSetOperation;
    }

    public Optional<String> get(String key) {
        return this.stringOperation.get(key);
    }

    public void set(String key, String value) {
        this.stringOperation.set(key, value);
    }

    public void del(String key) {
        this.keyOperation.del(key);
    }

    public long incr(String key) {
        return this.stringOperation.incr(key);
    }

    public long incrBy(String key, int increment) {
        return this.stringOperation.incrBy(key, increment);
    }

    public long decr(String key) {
        return this.stringOperation.decr(key);
    }

    public long decrby(String key, int increment) {
        return this.stringOperation.decrBy(key, increment);
    }

    public boolean exists(String key) {
        return this.keyOperation.exists(key);
    }

    public void hdel(String key, String... fields) {
        this.hashOperation.hdel(key, fields);
    }

    public boolean hexists(String key, String field) {
        return this.hashOperation.hexists(key, field);
    }

    public Optional<String> hget(String key, String field) {
        return this.hashOperation.hget(key, field);
    }

    public Map<String, String> hgetall(String key) {
        return this.hashOperation.hgetall(key);
    }

    public long hincrby(String key, String field, long increment) {
        return this.hashOperation.hincrby(key, field, increment);
    }

    public Set<String> hkeys(String key) {
        return this.hashOperation.hkeys(key);
    }

    public long hlen(String key) {
        return this.hashOperation.hlen(key);
    }

    public void hset(String key, String field, String value) {
        this.hashOperation.hset(key, field, value);
    }

    public long hsetnx(String key, String field, String value) {
        return this.hashOperation.hsetnx(key, field, value);
    }

    public List<String> hvals(String key) {
        return this.hashOperation.hvals(key);
    }

    public void hmset(String key, Map<String, String> values) {
        this.hashOperation.hmset(key, values);
    }

    public List<String> hmget(String key, String... fields) {
        return this.hashOperation.hmget(key, fields);
    }

    public long decrBy(String key, int increment) {
        return this.stringOperation.decrBy(key, increment);
    }

    public Optional<String> getSet(String key, String value) {
        return this.stringOperation.getSet(key, value);
    }

    public void setex(String key, String value, int expired) {
        this.stringOperation.setex(key, value, expired);
    }

    public boolean setexnx(String key, String value, int expired) {
        return this.stringOperation.setexnx(key, value, expired);
    }

    public void append(String key, String value) {
        this.stringOperation.append(key, value);
    }

    public void setbit(String key, long offset, String value) {
        this.stringOperation.setbit(key, offset, value);
    }

    public void expire(String key, int seconds) {
        this.keyOperation.expire(key, seconds);
    }

    public Optional<String> blpop(String key, int timeout) {
        return this.listOperation.blpop(key, timeout);
    }

    public Optional<String> rpop(String key) {
        return this.listOperation.rpop(key);
    }

    public Optional<String> lpop(String key) {
        return this.listOperation.lpop(key);
    }

    public Optional<String> brpop(String key, int timeout) {
        return this.listOperation.brpop(key, timeout);
    }

    public long llen(String key) {
        return this.listOperation.llen(key);
    }

    public List<String> lrange(String key, long start, long end) {
        return this.listOperation.lrange(key, start, end);
    }

    public void lset(String key, long index, String value) {
        this.listOperation.lset(key, index, value);
    }

    public void rpush(String key, String... value) {
        this.listOperation.rpush(key, value);
    }

    public void lpush(String key, String... value) {
        this.listOperation.lpush(key, value);
    }

    public Optional<String> lindex(String key, long index) {
        return this.listOperation.lindex(key, index);
    }

    public void sadd(String key, String... values) {
        this.setOperation.sadd(key, values);
    }

    public long scard(String key) {
        return this.setOperation.scard(key);
    }

    public boolean sismember(String key, String value) {
        return this.setOperation.sismember(key, value);
    }

    public Set<String> smembers(String key) {
        return this.setOperation.smembers(key);
    }

    public long srem(String key, String... values) {
        return this.setOperation.srem(key, values);
    }

    public Optional<String> spop(String key) {
        return this.setOperation.spop(key);
    }

    public void zadd(String key, double score, String member) {
        this.sortedSetOperation.zadd(key, score, member);
    }

    public long zcard(String key) {
        return this.sortedSetOperation.zcard(key);
    }

    public long zcount(String key, double min, double max) {
        try{
            return this.sortedSetOperation.zcount(key, min, max);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public Set<String> zrange(String key, long start, long end) {
        try {
            return this.sortedSetOperation.zrange(key, start, end);
        }
        catch (Exception e){
            return null;
        }
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        try {
            return this.sortedSetOperation.zrangeWithScores(key, start, end);
        }
        catch (Exception e){
            return null;
        }
    }

    public Set<String> zrangeByScore(String key, double min, double max) {
        try {
            return this.sortedSetOperation.zrangeByScore(key, min, max);
        }
        catch (Exception e){
            return null;
        }
    }

    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        try {
            return this.sortedSetOperation.zrangeByScore(key, min, max, offset, count);
        }
        catch (Exception e){
            return null;
        }
    }

    public long zrank(String key, String member) {
        try {
            return this.sortedSetOperation.zrank(key, member);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public long zrem(String key, String... members) {
        try {
            return this.sortedSetOperation.zrem(key, members);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public long zremrangeByScore(String key, double min, double max) {
        try {
            return this.sortedSetOperation.zremrangeByScore(key, min, max);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public long zremrangeByRank(String key, long start, long end) {
        try {
            return this.sortedSetOperation.zremrangeByRank(key, start, end);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public Set<String> zrevrange(String key, long start, long end) {
        try {
            return this.sortedSetOperation.zrevrange(key, start, end);
        }
        catch (Exception e){
            return null;
        }
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        try {
            return this.sortedSetOperation.zrevrangeWithScores(key, start, end);
        }
        catch (Exception e){
            return null;
        }
    }

    public Set<String> zrevrangeByScore(String key, double min, double max) {
        try {
            return this.sortedSetOperation.zrevrangeByScore(key, min, max);
        }
        catch (Exception e){
            return null;
        }
    }

    public Set<String> zrevrangeByScore(String key, double min, double max, int offset, int count) {
        try {
            return this.sortedSetOperation.zrevrangeByScore(key, min, max, offset, count);
        }
        catch (Exception e){
            return null;
        }
    }

    public long zrevrank(String key, String member) {
        try {
            return this.sortedSetOperation.zrevrank(key, member);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public long zinterstore(String dstkey, String... sets) {
        try {
            return this.sortedSetOperation.zinterstore(dstkey, sets);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public long zunionstore(String dstkey, String... sets) {
        try {
            return this.sortedSetOperation.zunionstore(dstkey, sets);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public double zscore(String key, String member) {
        try {
            return this.sortedSetOperation.zscore(key, member);
        }
        catch (Exception e){
            return 0L;
        }
    }

    public double zincrby(String key, double score, String member) {
        try {
            return this.sortedSetOperation.zincrby(key, score, member);
        }
        catch (Exception e){
            return 0L;
        }
    }
}
