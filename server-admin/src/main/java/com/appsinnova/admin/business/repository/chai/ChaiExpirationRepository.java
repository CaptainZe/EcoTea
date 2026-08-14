package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ChaiExpirationRepository extends JpaRepository<ChaiExpiration, Long>, JpaSpecificationExecutor<ChaiExpiration> {

    void deleteByIdIn(List<Long> idList);

    Optional<ChaiExpiration> findFirstByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
