package com.appsinnova.admin.business.repository.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStockBillItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChaiStockBillItemRepository extends JpaRepository<ChaiStockBillItem, Long> {

    List<ChaiStockBillItem> findByBillIdOrderByIdAsc(Long billId);
}
