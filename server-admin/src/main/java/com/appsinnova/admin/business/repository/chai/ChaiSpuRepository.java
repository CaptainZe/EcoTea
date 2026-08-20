package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChaiSpuRepository extends JpaRepository<ChaiSpu, Long>, JpaSpecificationExecutor<ChaiSpu> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChaiSpu s set s.deleted = 1, s.status = :status, s.operator = :operator, "
            + "s.updateTime = :updateTime where s.id = :id and (s.deleted is null or s.deleted = 0)")
    int softDeleteById(@Param("id") Long id,
                       @Param("status") Integer status,
                       @Param("operator") String operator,
                       @Param("updateTime") Long updateTime);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChaiSpu s set s.deleted = 0, s.status = :status, s.operator = :operator, "
            + "s.updateTime = :updateTime where s.id = :id and s.deleted = 1")
    int restoreById(@Param("id") Long id,
                    @Param("status") Integer status,
                    @Param("operator") String operator,
                    @Param("updateTime") Long updateTime);

    java.util.Optional<ChaiSpu> findFirstBySpuCode(String spuCode);

    java.util.Optional<ChaiSpu> findFirstBySpuCodeAndDeleted(String spuCode, Integer deleted);

    List<ChaiSpu> findByIdIn(List<Long> idList);

    boolean existsByBrand(Long brand);

    boolean existsByExpiration(Long expiration);

    boolean existsByBrandAndName(Long brand, String name);

    boolean existsByBrandAndNameAndDeleted(Long brand, String name, Integer deleted);

    boolean existsByBrandAndNameAndIdNot(Long brand, String name, Long id);

    boolean existsByBrandAndNameAndIdNotAndDeleted(Long brand, String name, Long id, Integer deleted);
}
