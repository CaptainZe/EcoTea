package com.ecotea.api.service.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.tenant.Tenant;
import com.ecotea.api.mapper.tenant.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;

    /**
     * 按业务键查店铺：Redis → miss → MySQL → SET（DAY_EXPIRE；库无则 {@code "null"} + 5 分钟）。
     */
    public Tenant getByBizTenantId(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        String key = String.format(RedisConstant.TENANT_KEY, tenantId);
        String cached = RedisUtils.get(RedisUtils.defaultRedis(), key);
        if (cached != null) {
            return JsonUtils.readValueForCache(cached, Tenant.class);
        }

        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantId, tenantId)
                .last("limit 1"));
        if (tenant == null) {
            RedisUtils.set(RedisUtils.defaultRedis(), key, "null", RedisConstant.FIVE_MINUTE_EXPIRE);
            return null;
        }
        String json = JsonUtils.writeValueAsString(tenant);
        if (StringUtils.hasText(json)) {
            RedisUtils.set(RedisUtils.defaultRedis(), key, json, RedisConstant.DAY_EXPIRE);
        }
        return tenant;
    }

    public Map<Long, Tenant> mapByBizTenantIds(List<Long> tenantIds) {
        if (CollectionUtils.isEmpty(tenantIds)) {
            return Collections.emptyMap();
        }
        Set<Long> unique = new HashSet<>(tenantIds);
        Map<Long, Tenant> result = new HashMap<>();
        for (Long tid : unique) {
            if (tid == null) {
                continue;
            }
            Tenant tenant = getByBizTenantId(tid);
            if (tenant != null) {
                result.put(tid, tenant);
            }
        }
        return result;
    }
}
