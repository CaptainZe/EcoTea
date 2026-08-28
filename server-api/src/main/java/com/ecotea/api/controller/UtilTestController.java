package com.ecotea.api.controller;

import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.common.utils.DictUtils;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.LockUtils;
import com.ecotea.api.common.utils.RedisUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚手架工具类验证接口（后续可删除）
 */
@RestController
@RequestMapping("/test/util")
public class UtilTestController {

    private static final String TEST_REDIS_KEY = RedisConstant.KEY_PRE + "test_util";
    private static final String TEST_LOCK_KEY = RedisConstant.KEY_PRE + "test_lock";

    @GetMapping("/redis")
    public ApiResult<Map<String, Object>> testRedis() {
        StringRedisTemplate redis = RedisUtils.defaultRedis();
        Map<String, Object> result = new LinkedHashMap<>();

        String value = "hello";
        RedisUtils.set(redis, TEST_REDIS_KEY, value, RedisConstant.MINUTE_EXPIRE);
        String got = RedisUtils.get(redis, TEST_REDIS_KEY);
        boolean exists = RedisUtils.exists(redis, TEST_REDIS_KEY);
        RedisUtils.expire(redis, TEST_REDIS_KEY, RedisConstant.FIVE_MINUTE_EXPIRE);
        RedisUtils.delete(redis, TEST_REDIS_KEY);
        boolean existsAfterDelete = RedisUtils.exists(redis, TEST_REDIS_KEY);

        result.put("setValue", value);
        result.put("getValue", got);
        result.put("existsAfterSet", exists);
        result.put("existsAfterDelete", existsAfterDelete);
        result.put("jsonSample", JsonUtils.writeValueAsString(result));
        result.put("ok", value.equals(got) && exists && !existsAfterDelete);
        return ApiResult.ok(result);
    }

    @GetMapping("/lock")
    public ApiResult<Map<String, Object>> testLock() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 先清掉残留锁
        LockUtils.unLock(TEST_LOCK_KEY);

        boolean first = LockUtils.lock(TEST_LOCK_KEY, RedisConstant.MINUTE_EXPIRE);
        boolean second = LockUtils.lock(TEST_LOCK_KEY, RedisConstant.MINUTE_EXPIRE);
        LockUtils.unLock(TEST_LOCK_KEY);
        boolean third = LockUtils.lock(TEST_LOCK_KEY, RedisConstant.MINUTE_EXPIRE);
        LockUtils.unLock(TEST_LOCK_KEY);

        // first/third=false 表示拿到锁；second=true 表示已被锁
        result.put("firstLockBlocked", first);
        result.put("secondLockBlocked", second);
        result.put("thirdLockBlocked", third);
        result.put("ok", !first && second && !third);
        return ApiResult.ok(result);
    }

    @GetMapping("/dict")
    public ApiResult<Map<String, Object>> testDict(
            @RequestParam String label,
            @RequestParam(required = false) String code) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> valueMap = DictUtils.value(label);
        result.put("label", label);
        result.put("value", valueMap);
        if (StringUtils.hasText(code)) {
            result.put("code", code);
            result.put("keyValue", DictUtils.keyValue(label, code));
        }
        result.put("ok", true);
        return ApiResult.ok(result);
    }
}
