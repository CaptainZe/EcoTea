package com.appsinnova.admin.business.repository.platform;

import com.appsinnova.admin.business.domain.platform.EmployeeTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeTenantRepository extends JpaRepository<EmployeeTenant, Long> {

    List<EmployeeTenant> findByEmployeeIdOrderByTenantIdAsc(Long employeeId);

    List<EmployeeTenant> findByEmployeeIdIn(Collection<Long> employeeIds);

    Optional<EmployeeTenant> findFirstByEmployeeIdAndTenantId(Long employeeId, Long tenantId);

    @Query("select count(distinct et.employeeId) from EmployeeTenant et, Employee e "
            + "where et.employeeId = e.id and et.tenantId = :tenantId "
            + "and et.status = 1 and e.status = 1")
    long countOccupied(@Param("tenantId") Long tenantId);

    @Modifying(clearAutomatically = true)
    @Query("delete from EmployeeTenant et where et.employeeId = :employeeId")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);

    @Modifying(clearAutomatically = true)
    @Query("delete from EmployeeTenant et where et.employeeId = :employeeId and et.tenantId = :tenantId")
    int deleteByEmployeeIdAndTenantId(@Param("employeeId") Long employeeId, @Param("tenantId") Long tenantId);
}
