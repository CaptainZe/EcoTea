package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ChaiBrandRepository extends JpaRepository<ChaiBrand, Long>, JpaSpecificationExecutor<ChaiBrand> {

    void deleteByIdIn(List<Long> idList);

    Optional<ChaiBrand> findFirstByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
