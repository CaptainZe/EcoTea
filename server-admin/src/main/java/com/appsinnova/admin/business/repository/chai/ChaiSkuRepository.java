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

    void deleteByIdIn(List<Long> idList);

    void deleteBySpuId(Long spuId);

    long countBySpuId(Long spuId);

    @Modifying(clearAutomatically = true)
    @Query("update ChaiSku s set s.status = :status, s.operator = :operator, s.updateTime = :updateTime where s.spuId = :spuId")
    int updateStatusBySpuId(@Param("spuId") Long spuId,
                            @Param("status") Integer status,
                            @Param("operator") String operator,
                            @Param("updateTime") Long updateTime);
}
