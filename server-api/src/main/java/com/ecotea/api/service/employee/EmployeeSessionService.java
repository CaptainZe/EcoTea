package com.ecotea.api.service.employee;

import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class EmployeeSessionService {

    public EmployeeSession getByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String json = RedisUtils.get(redis(), sessionKey(token.trim()));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JsonUtils.readValueForCache(json, EmployeeSession.class);
    }

    public String create(EmployeeSession session) {
        if (session == null || session.getEmployeeId() == null) {
            throw new IllegalArgumentException("session invalid");
        }
        invalidateByEmployeeId(session.getEmployeeId());
        String token = UUID.randomUUID().toString().replace("-", "");
        save(token, session);
        RedisUtils.set(redis(), uidKey(session.getEmployeeId()), token, RedisConstant.EMP_SESSION_EXPIRE);
        return token;
    }

    public void save(String token, EmployeeSession session) {
        String json = JsonUtils.writeValueAsString(session);
        RedisUtils.set(redis(), sessionKey(token), json, RedisConstant.EMP_SESSION_EXPIRE);
        if (session.getEmployeeId() != null) {
            RedisUtils.set(redis(), uidKey(session.getEmployeeId()), token, RedisConstant.EMP_SESSION_EXPIRE);
        }
    }

    public void touch(String token, EmployeeSession session) {
        if (!StringUtils.hasText(token) || session == null) {
            return;
        }
        save(token, session);
    }

    public void remove(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        EmployeeSession session = getByToken(token);
        RedisUtils.delete(redis(), sessionKey(token.trim()));
        if (session != null && session.getEmployeeId() != null) {
            String uidToken = RedisUtils.get(redis(), uidKey(session.getEmployeeId()));
            if (token.trim().equals(uidToken)) {
                RedisUtils.delete(redis(), uidKey(session.getEmployeeId()));
            }
        }
    }

    public void invalidateByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        String oldToken = RedisUtils.get(redis(), uidKey(employeeId));
        if (StringUtils.hasText(oldToken)) {
            RedisUtils.delete(redis(), sessionKey(oldToken));
        }
        RedisUtils.delete(redis(), uidKey(employeeId));
    }

    private static String sessionKey(String token) {
        return String.format(RedisConstant.EMP_SESSION_KEY, token);
    }

    private static String uidKey(Long employeeId) {
        return String.format(RedisConstant.EMP_UID_TOKEN_KEY, employeeId);
    }

    private static StringRedisTemplate redis() {
        return RedisUtils.defaultRedis();
    }
}
