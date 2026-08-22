package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStockWh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChaiStockWhRepository extends JpaRepository<ChaiStockWh, Long> {

    List<ChaiStockWh> findByStockIdOrderByQtyDescIdAsc(Long stockId);

    Optional<ChaiStockWh> findFirstByStockIdAndWhId(Long stockId, Long whId);

    boolean existsByWhId(Long whId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChaiStockWh w set w.qty = w.qty + :delta, w.version = w.version + 1 "
            + "where w.id = :id and w.version = :version and w.qty + :delta >= 0")
    int applyQtyDelta(@Param("id") Long id,
                      @Param("delta") int delta,
                      @Param("version") int version);
}
