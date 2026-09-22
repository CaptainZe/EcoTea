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

    /**
     * 完整：只动总数；三例外合计 &lt;= 结果 qty。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChaiStockWh w set w.qty = w.qty + :delta, w.version = w.version + 1 "
            + "where w.id = :id and w.version = :version "
            + "and w.qty + :delta >= 0 "
            + "and coalesce(w.qtyNoBag, 0) + coalesce(w.qtyDamaged, 0) + coalesce(w.qtyDamagedNoBag, 0) "
            + "<= w.qty + :delta")
    int applyIntactQtyDelta(@Param("id") Long id,
                            @Param("delta") int delta,
                            @Param("version") int version);

    /**
     * 外观完整无提袋：总数与 qtyNoBag 同步增减。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChaiStockWh w set w.qty = w.qty + :delta, "
            + "w.qtyNoBag = coalesce(w.qtyNoBag, 0) + :delta, "
            + "w.version = w.version + 1 "
            + "where w.id = :id and w.version = :version "
            + "and w.qty + :delta >= 0 "
            + "and coalesce(w.qtyNoBag, 0) + :delta >= 0 "
            + "and coalesce(w.qtyNoBag, 0) + :delta + coalesce(w.qtyDamaged, 0) "
            + "+ coalesce(w.qtyDamagedNoBag, 0) <= w.qty + :delta")
    int applyNoBagQtyDelta(@Param("id") Long id,
                           @Param("delta") int delta,
                           @Param("version") int version);

    /**
     * 外观破损有提袋：总数与 qtyDamaged 同步增减。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChaiStockWh w set w.qty = w.qty + :delta, "
            + "w.qtyDamaged = coalesce(w.qtyDamaged, 0) + :delta, "
            + "w.version = w.version + 1 "
            + "where w.id = :id and w.version = :version "
            + "and w.qty + :delta >= 0 "
            + "and coalesce(w.qtyDamaged, 0) + :delta >= 0 "
            + "and coalesce(w.qtyNoBag, 0) + coalesce(w.qtyDamaged, 0) + :delta "
            + "+ coalesce(w.qtyDamagedNoBag, 0) <= w.qty + :delta")
    int applyDamagedQtyDelta(@Param("id") Long id,
                             @Param("delta") int delta,
                             @Param("version") int version);

    /**
     * 外观破损无提袋：总数与 qtyDamagedNoBag 同步增减。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChaiStockWh w set w.qty = w.qty + :delta, "
            + "w.qtyDamagedNoBag = coalesce(w.qtyDamagedNoBag, 0) + :delta, "
            + "w.version = w.version + 1 "
            + "where w.id = :id and w.version = :version "
            + "and w.qty + :delta >= 0 "
            + "and coalesce(w.qtyDamagedNoBag, 0) + :delta >= 0 "
            + "and coalesce(w.qtyNoBag, 0) + coalesce(w.qtyDamaged, 0) "
            + "+ coalesce(w.qtyDamagedNoBag, 0) + :delta <= w.qty + :delta")
    int applyDamagedNoBagQtyDelta(@Param("id") Long id,
                                  @Param("delta") int delta,
                                  @Param("version") int version);
}
