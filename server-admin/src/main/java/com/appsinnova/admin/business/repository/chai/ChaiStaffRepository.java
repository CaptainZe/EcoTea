package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChaiStaffRepository extends JpaRepository<ChaiStaff, Long>, JpaSpecificationExecutor<ChaiStaff> {

    void deleteByIdIn(List<Long> idList);

    Optional<ChaiStaff> findFirstByNickName(String nickName);

    boolean existsByNickNameAndIdNot(String nickName, Long id);

    @Query(value = "SELECT COUNT(1) FROM chai_stock_bill WHERE handler_id = :handlerId", nativeQuery = true)
    long countBillsByHandlerId(@Param("handlerId") Long handlerId);
}
