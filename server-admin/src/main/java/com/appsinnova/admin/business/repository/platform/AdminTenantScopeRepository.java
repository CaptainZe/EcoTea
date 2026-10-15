package com.appsinnova.admin.business.repository.platform;

import com.appsinnova.admin.business.domain.platform.AdminTenantScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AdminTenantScopeRepository extends JpaRepository<AdminTenantScope, Long>,
        JpaSpecificationExecutor<AdminTenantScope> {

    List<AdminTenantScope> findByUserIdOrderByTenantIdAsc(Long userId);

    List<AdminTenantScope> findByUserIdIn(Collection<Long> userIds);

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    @Modifying(clearAutomatically = true)
    @Query("delete from AdminTenantScope s where s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
