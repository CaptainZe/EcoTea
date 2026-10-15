package com.appsinnova.admin.business.service.platform;

import com.appsinnova.admin.business.common.enums.platform.TenantStatus;
import com.appsinnova.admin.business.domain.platform.AdminTenantScope;
import com.appsinnova.admin.business.domain.platform.Tenant;
import com.appsinnova.admin.business.repository.platform.AdminTenantScopeRepository;
import com.appsinnova.admin.business.repository.platform.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTenantScopeService {

    private final AdminTenantScopeRepository scopeRepository;
    private final TenantRepository tenantRepository;

    public List<AdminTenantScope> listByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return scopeRepository.findByUserIdOrderByTenantIdAsc(userId);
    }

    public Map<Long, List<AdminTenantScope>> mapByUserIds(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return scopeRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(AdminTenantScope::getUserId));
    }

    public boolean isPlatform(List<AdminTenantScope> scopes) {
        if (CollectionUtils.isEmpty(scopes)) {
            return false;
        }
        return scopes.stream().anyMatch(s -> AdminTenantScope.PLATFORM_TENANT_ID == s.getTenantId());
    }

    public String buildSummary(List<AdminTenantScope> scopes, Map<Long, String> tenantNameByBizId) {
        if (CollectionUtils.isEmpty(scopes)) {
            return "未配置";
        }
        if (isPlatform(scopes)) {
            return "平台";
        }
        List<String> names = new ArrayList<>();
        for (AdminTenantScope scope : scopes) {
            String name = tenantNameByBizId.get(scope.getTenantId());
            if (name != null) {
                names.add(name + "(" + scope.getTenantId() + ")");
            } else {
                names.add(String.valueOf(scope.getTenantId()));
            }
        }
        return String.join("、", names);
    }

    public Map<Long, String> loadTenantNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (Tenant t : tenantRepository.findAll()) {
            map.put(t.getTenantId(), t.getName());
        }
        return map;
    }

    public List<Tenant> listSelectableTenants() {
        return tenantRepository.findAll().stream()
                .filter(t -> TenantStatus.ENABLED.getCode().equals(t.getStatus()))
                .sorted(Comparator.comparing(Tenant::getTenantId))
                .collect(Collectors.toList());
    }

    /**
     * 整用户覆盖保存。platform=true 时只保留 (userId,0)；否则只保留指定店铺行。
     */
    @Transactional
    public void replaceScopes(Long userId, boolean platform, List<Long> tenantIds, String operator) {
        if (userId == null) {
            throw new IllegalArgumentException("用户必选");
        }
        long now = System.currentTimeMillis();
        scopeRepository.deleteByUserId(userId);

        if (platform) {
            AdminTenantScope row = newAdminRow(userId, AdminTenantScope.PLATFORM_TENANT_ID, operator, now);
            scopeRepository.save(row);
            return;
        }

        if (CollectionUtils.isEmpty(tenantIds)) {
            throw new IllegalArgumentException("请至少选择一个店铺，或选择平台范围");
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long tid : tenantIds) {
            if (tid == null || tid <= 0) {
                throw new IllegalArgumentException("店铺租户ID无效");
            }
            unique.add(tid);
        }
        for (Long tid : unique) {
            if (!tenantRepository.findFirstByTenantId(tid).isPresent()) {
                throw new IllegalArgumentException("店铺不存在: " + tid);
            }
            scopeRepository.save(newAdminRow(userId, tid, operator, now));
        }
    }

    @Transactional
    public void clearScopes(Long userId) {
        if (userId == null) {
            return;
        }
        scopeRepository.deleteByUserId(userId);
    }

    private AdminTenantScope newAdminRow(Long userId, Long tenantId, String operator, long now) {
        AdminTenantScope row = new AdminTenantScope();
        row.setUserId(userId);
        row.setTenantId(tenantId);
        row.setOperator(operator == null ? "" : operator);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        return row;
    }
}
