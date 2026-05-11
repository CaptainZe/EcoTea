package com.appsinnova.admin.business.repository.tea;

import com.appsinnova.admin.business.domain.tea.TeaQuoteOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface TeaQuoteOrderItemRepository extends JpaRepository<TeaQuoteOrderItem, Long>, JpaSpecificationExecutor<TeaQuoteOrderItem> {

    List<TeaQuoteOrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    List<TeaQuoteOrderItem> findByOrderIdIn(Collection<Long> orderIds);
}