package com.appsinnova.admin.business.service.platform;

import com.appsinnova.admin.business.common.constant.RedisConstant;
import com.appsinnova.admin.business.common.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 踢小程序员工会话（与 api {@code EmployeeSessionService#invalidateByEmployeeId} 同一 Redis key）。
 */
@Slf4j
@Service
public class EmployeeSessionCacheService {

    public void invalidateByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        try {
            String uidKey = String.format(RedisConstant.EMP_UID_TOKEN_KEY, employeeId);
            String oldToken = RedisUtils.get(RedisUtils.defaultRedis(), uidKey);
            if (StringUtils.hasText(oldToken)) {
                RedisUtils.delete(RedisUtils.defaultRedis(),
                        String.format(RedisConstant.EMP_SESSION_KEY, oldToken.trim()));
            }
            RedisUtils.delete(RedisUtils.defaultRedis(), uidKey);
        } catch (Exception e) {
            log.warn("invalidate employee session failed, employeeId={}: {}", employeeId, e.getMessage());
        }
    }
}
