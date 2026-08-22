package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChaiWarehouseRepository extends JpaRepository<ChaiWarehouse, Long>, JpaSpecificationExecutor<ChaiWarehouse> {

    void deleteByIdIn(List<Long> idList);

    Optional<ChaiWarehouse> findFirstByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query(value = "SELECT COUNT(1) FROM chai_stock_wh WHERE wh_id = :whId", nativeQuery = true)
    long countStockWhByWhId(@Param("whId") Long whId);

    @Query(value = "SELECT COUNT(1) FROM chai_stock_bill WHERE from_wh_id = :whId OR to_wh_id = :whId", nativeQuery = true)
    long countBillsByWhId(@Param("whId") Long whId);
}
