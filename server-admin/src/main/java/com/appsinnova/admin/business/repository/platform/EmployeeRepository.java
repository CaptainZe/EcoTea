package com.appsinnova.admin.business.repository.platform;

import com.appsinnova.admin.business.domain.platform.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findFirstByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
