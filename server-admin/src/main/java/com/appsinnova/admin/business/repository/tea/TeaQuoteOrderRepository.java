package com.appsinnova.admin.business.repository.tea;

import com.appsinnova.admin.business.domain.tea.TeaQuoteOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeaQuoteOrderRepository extends JpaRepository<TeaQuoteOrder, Long>, JpaSpecificationExecutor<TeaQuoteOrder> {
}