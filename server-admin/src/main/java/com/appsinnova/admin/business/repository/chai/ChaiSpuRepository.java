package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ChaiSpuRepository extends JpaRepository<ChaiSpu, Long>, JpaSpecificationExecutor<ChaiSpu> {

    void deleteByIdIn(List<Long> idList);

    java.util.Optional<ChaiSpu> findFirstBySpuCode(String spuCode);

    List<ChaiSpu> findByIdIn(List<Long> idList);

    boolean existsByBrandAndName(Long brand, String name);

    boolean existsByBrandAndNameAndIdNot(Long brand, String name, Long id);
}
