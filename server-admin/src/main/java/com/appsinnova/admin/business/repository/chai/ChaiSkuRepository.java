package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChaiSkuRepository extends JpaRepository<ChaiSku, Long>, JpaSpecificationExecutor<ChaiSku> {

    List<ChaiSku> findBySpuIdOrderByYearDescProdBatchDesc(Long spuId);

    List<ChaiSku> findBySpuIdAndDeletedOrderByYearDescProdBatchDesc(Long spuId, Integer deleted);

    java.util.Optional<ChaiSku> findFirstBySpuIdAndYearAndProdBatch(Long spuId, Integer year, Integer prodBatch);

    boolean existsBySpuIdAndYearAndProdBatchAndDeleted(Long spuId, Integer year, Integer prodBatch, Integer deleted);

    long countBySpuId(Long spuId);

    long countBySpuIdAndDeleted(Long spuId, Integer deleted);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChaiSku s set s.status = :status, s.operator = :operator, s.updateTime = :updateTime "
            + "where s.spuId = :spuId and s.deleted = 0")
    int updateStatusBySpuId(@Param("spuId") Long spuId,
                            @Param("status") Integer status,
                            @Param("operator") String operator,
                            @Param("updateTime") Long updateTime);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChaiSku s set s.deleted = 1, s.status = :status, s.operator = :operator, "
            + "s.updateTime = :updateTime where s.spuId = :spuId and (s.deleted is null or s.deleted = 0)")
    int softDeleteBySpuId(@Param("spuId") Long spuId,
                          @Param("status") Integer status,
                          @Param("operator") String operator,
                          @Param("updateTime") Long updateTime);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChaiSku s set s.deleted = 0, s.status = :status, s.operator = :operator, "
            + "s.updateTime = :updateTime where s.spuId = :spuId and s.deleted = 1")
    int restoreBySpuId(@Param("spuId") Long spuId,
                       @Param("status") Integer status,
                       @Param("operator") String operator,
                       @Param("updateTime") Long updateTime);
}
