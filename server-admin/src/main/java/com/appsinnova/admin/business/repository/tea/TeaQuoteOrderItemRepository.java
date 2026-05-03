package com.appsinnova.admin.business.repository.tea;

import com.appsinnova.admin.business.domain.tea.TeaQuoteOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeaQuoteOrderItemRepository extends JpaRepository<TeaQuoteOrderItem, Long>, JpaSpecificationExecutor<TeaQuoteOrderItem> {

    java.util.List<TeaQuoteOrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}