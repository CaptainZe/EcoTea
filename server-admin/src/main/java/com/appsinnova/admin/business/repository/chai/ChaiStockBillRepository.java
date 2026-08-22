package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStockBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChaiStockBillRepository extends JpaRepository<ChaiStockBill, Long>, JpaSpecificationExecutor<ChaiStockBill> {
}
