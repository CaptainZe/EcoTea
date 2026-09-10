package com.appsinnova.admin.business.common.constant;

/**
 * Redis 常量（与 server-api 共用 KEY_PRE，便于 admin 删/换缓存后 api 立即生效）
 */
public interface RedisConstant {

    /** EcoTea 共用 key 前缀（admin / api 必须一致） */
    String KEY_PRE = "ecotea_";

    /* ***** 常用过期时间（秒） ***** */
    int NO_EXPIRE = 0;
    int MINUTE_EXPIRE = 60;
    int FIVE_MINUTE_EXPIRE = 300;
    int TEN_MINUTE_EXPIRE = 600;
    int HALF_HOUR_EXPIRE = 1800;
    int HOUR_EXPIRE = 3600;
    int DAY_EXPIRE = 86400;

    /** 默认 Redis 实例名（多 Redis 时扩展其它名称） */
    String DEFAULT_REDIS = "default";

    /* ***** 日序号（Redis INCR，基础设施） ***** */
    /** 序号 key 存活：3 天 */
    int SEQ_TTL_SEC = DAY_EXPIRE * 3;
    /** key：{@code ecotea_seq_{biz}_{yyyyMMdd}}，biz 见 {@link SeqBizConstant} */
    String SEQ_KEY_INFIX = "seq_";
}
