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
}
