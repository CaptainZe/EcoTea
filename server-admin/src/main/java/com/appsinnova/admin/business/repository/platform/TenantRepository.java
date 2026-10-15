package com.appsinnova.admin.business.repository.platform;

import com.appsinnova.admin.business.domain.platform.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    Optional<Tenant> findFirstByTenantId(Long tenantId);

    boolean existsByTenantIdAndIdNot(Long tenantId, Long id);
}
