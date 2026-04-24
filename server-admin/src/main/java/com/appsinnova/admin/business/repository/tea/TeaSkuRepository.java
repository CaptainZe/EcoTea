package com.appsinnova.admin.business.repository.tea;

import com.appsinnova.admin.business.domain.tea.TeaSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TeaSkuRepository extends JpaRepository<TeaSku, Long>, JpaSpecificationExecutor<TeaSku> {

    Integer deleteByIdIn(List<Long> idList);
}