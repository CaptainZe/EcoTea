package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStockBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ChaiStockBillRepository extends JpaRepository<ChaiStockBill, Long>, JpaSpecificationExecutor<ChaiStockBill> {

    long countByStatusAndBillTypeAndCreateTimeBetween(Integer status, Integer billType, Long createTimeStart, Long createTimeEnd);

    List<ChaiStockBill> findByStatusAndCreateTimeGreaterThanEqual(Integer status, Long createTime);
}
