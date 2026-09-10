package com.appsinnova.admin.business.common.utils;

import com.appsinnova.admin.business.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 按日业务序号（Redis INCR），只返回序号，不拼业务单号。
 * <p>
 * key：{@code ecotea_seq_{biz}_{yyyyMMdd}}；TTL 默认 3 天。<br>
 * key 不存在时可用 {@link MaxSeqLoader} 从业务表最大号 SETNX 初始化，再 INCR，避免 Redis 丢 key 后从 1 撞号。<br>
 * 单号格式由各业务工具类自行拼接（如 {@code ChaiStockBillNoUtil}）。
 * </p>
 * <p>后续替代 {@code DailySequenceService}；调用方保存遇唯一键冲突时可再取号重试。</p>
 */
@Slf4j
public final class RedisSeqUtils {

    private RedisSeqUtils() {
    }

    /**
     * 加载某业务日已占用的最大序号；无数据返回 0。
     */
    @FunctionalInterface
    public interface MaxSeqLoader {
        long loadMaxSeq(String bizDateYyyyMmDd);
    }

    /**
     * 组装日序号 key。
     */
    public static String dailyKey(String biz, String bizDateYyyyMmDd) {
        return RedisConstant.KEY_PRE + RedisConstant.SEQ_KEY_INFIX + biz + "_" + bizDateYyyyMmDd;
    }

    /**
     * 当前中国业务日取号（无库兜底，从 0 起 INCR）。
     */
    public static long nextDaily(String biz) {
        return nextDaily(biz, System.currentTimeMillis(), null);
    }

    /**
     * 指定时间戳对应业务日取号；{@code maxLoader} 可为 null。
     *
     * @return 自增后的序号（从 1 起）
     */
    public static long nextDaily(String biz, long timestamp, MaxSeqLoader maxLoader) {
        if (!StringUtils.hasText(biz)) {
            throw new IllegalArgumentException("biz 不能为空");
        }
        String bizDate = TimeUtils.getDateYYMMdd(timestamp);
        return nextDailyOnDate(biz, bizDate, maxLoader);
    }

    /**
     * 指定业务日取号。
     */
    public static long nextDailyOnDate(String biz, String bizDateYyyyMmDd, MaxSeqLoader maxLoader) {
        if (!StringUtils.hasText(biz)) {
            throw new IllegalArgumentException("biz 不能为空");
        }
        if (!StringUtils.hasText(bizDateYyyyMmDd)) {
            throw new IllegalArgumentException("bizDate 不能为空");
        }
        StringRedisTemplate redis = RedisUtils.defaultRedis();
        if (redis == null) {
            throw new IllegalStateException("Redis 未就绪，无法发号");
        }
        String key = dailyKey(biz.trim(), bizDateYyyyMmDd.trim());
        ensureInitialized(redis, key, bizDateYyyyMmDd.trim(), maxLoader);
        Long seq = RedisUtils.increment(redis, key);
        if (seq == null) {
            throw new IllegalStateException("Redis INCR 失败: " + key);
        }
        ensureTtl(redis, key);
        return seq;
    }

    private static void ensureInitialized(StringRedisTemplate redis, String key,
                                          String bizDate, MaxSeqLoader maxLoader) {
        if (RedisUtils.exists(redis, key)) {
            return;
        }
        long max = 0L;
        if (maxLoader != null) {
            try {
                max = maxLoader.loadMaxSeq(bizDate);
            } catch (Exception e) {
                log.error("RedisSeqUtils maxLoader error. key={}, date={}, err={}",
                        key, bizDate, e.getMessage());
            }
            if (max < 0L) {
                max = 0L;
            }
        }
        // SETNX：并发下仅一方写入初始值，随后各方 INCR
        RedisUtils.setIfAbsent(redis, key, String.valueOf(max), RedisConstant.SEQ_TTL_SEC);
    }

    private static void ensureTtl(StringRedisTemplate redis, String key) {
        Long ttl = RedisUtils.getExpireSec(redis, key);
        // -1 无过期；null 查询失败时也尝试设一次
        if (ttl == null || ttl < 0L) {
            RedisUtils.expire(redis, key, RedisConstant.SEQ_TTL_SEC);
        }
    }
}
