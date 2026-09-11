package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStockBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface ChaiStockBillRepository extends JpaRepository<ChaiStockBill, Long>, JpaSpecificationExecutor<ChaiStockBill> {

    long countByStatusInAndBillTypeAndCreateTimeBetween(Collection<Integer> statuses, Integer billType,
                                                        Long createTimeStart, Long createTimeEnd);

    List<ChaiStockBill> findByStatusInAndCreateTimeGreaterThanEqual(Collection<Integer> statuses, Long createTime);

    ChaiStockBill findFirstByBillNoStartingWithOrderByBillNoDesc(String billNoPrefix);

    boolean existsByBillNo(String billNo);
}
