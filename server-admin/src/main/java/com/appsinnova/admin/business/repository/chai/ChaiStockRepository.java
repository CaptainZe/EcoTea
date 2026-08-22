package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChaiStockRepository extends JpaRepository<ChaiStock, Long>, JpaSpecificationExecutor<ChaiStock> {

    Optional<ChaiStock> findFirstBySkuId(Long skuId);

    boolean existsBySkuIdAndTotalQtyGreaterThan(Long skuId, Integer totalQty);

    @Query("SELECT COALESCE(SUM(s.totalQty), 0) FROM ChaiStock s")
    Long sumTotalQty();

    long countByTotalQtyGreaterThan(Integer totalQty);
}
