package com.ecotea.api.common.constant;

/**
 * Redis 常量
 */
public interface RedisConstant {

    /** 本项目 key 前缀，与 EcoTea / 其它系统隔离 */
    String KEY_PRE = "ecotea_api_";

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
