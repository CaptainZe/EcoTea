package com.appsinnova.admin.business.repository.platform;

import com.appsinnova.admin.business.domain.platform.EmployeeTenantPerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EmployeeTenantPermRepository extends JpaRepository<EmployeeTenantPerm, Long> {

    List<EmployeeTenantPerm> findByEmployeeId(Long employeeId);

    List<EmployeeTenantPerm> findByEmployeeIdAndTenantId(Long employeeId, Long tenantId);

    List<EmployeeTenantPerm> findByEmployeeIdIn(Collection<Long> employeeIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from EmployeeTenantPerm p where p.employeeId = :employeeId")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);

    @Modifying(clearAutomatically = true)
    @Query("delete from EmployeeTenantPerm p where p.employeeId = :employeeId and p.tenantId = :tenantId")
    int deleteByEmployeeIdAndTenantId(@Param("employeeId") Long employeeId, @Param("tenantId") Long tenantId);
}
