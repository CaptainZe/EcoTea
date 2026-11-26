package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSpuBarcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChaiSpuBarcodeRepository extends JpaRepository<ChaiSpuBarcode, Long>,
        JpaSpecificationExecutor<ChaiSpuBarcode> {

    List<ChaiSpuBarcode> findBySpuIdOrderByIdAsc(Long spuId);

    List<ChaiSpuBarcode> findBySpuIdIn(Collection<Long> spuIds);

    List<ChaiSpuBarcode> findByBarcode(String barcode);

    List<ChaiSpuBarcode> findByBarcodeStartingWith(String barcodePrefix);

    Optional<ChaiSpuBarcode> findFirstBySpuIdAndBarcode(Long spuId, String barcode);

    boolean existsBySpuIdAndBarcode(Long spuId, String barcode);

    boolean existsBySpuIdAndBarcodeStartingWith(Long spuId, String barcodePrefix);

    long countBySpuId(Long spuId);

    long countByBarcodeStartingWith(String barcodePrefix);

    @Query("select count(distinct b.spuId) from ChaiSpuBarcode b where b.barcode like :prefix")
    long countDistinctSpuIdByBarcodeLike(@Param("prefix") String prefix);

    @Query("select distinct b.spuId from ChaiSpuBarcode b")
    List<Long> findDistinctSpuIds();

    @Query("select distinct b.spuId from ChaiSpuBarcode b where b.barcode like :prefix")
    List<Long> findDistinctSpuIdsByBarcodeLike(@Param("prefix") String prefix);

    /** 系统码按品牌：Object[]{ brandId, count } */
    @Query("select s.brand, count(b) from ChaiSpuBarcode b, ChaiSpu s "
            + "where b.spuId = s.id and s.deleted = :deleted and b.barcode like :prefix "
            + "group by s.brand order by count(b) desc")
    List<Object[]> countGroupByBrandAndBarcodeLike(@Param("deleted") Integer deleted,
                                                   @Param("prefix") String prefix);

    void deleteBySpuIdAndId(Long spuId, Long id);

    void deleteBySpuId(Long spuId);
}
