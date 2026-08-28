package com.ecotea.api.controller;

import com.ecotea.api.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "UP");
        info.put("mysql", checkMysql());
        info.put("redis", checkRedis());
        return ApiResult.ok(info);
    }

    private String checkMysql() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            stringRedisTemplate.hasKey("__health_check__");
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
