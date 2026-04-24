package com.appsinnova.admin.business.repository.sys;

import com.appsinnova.admin.business.domain.sys.AppNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AppNoticeRepository extends JpaRepository<AppNotice, Long>, JpaSpecificationExecutor<AppNotice> {

    Integer deleteByIdIn(List<Long> idList);

    AppNotice findFirstByType(Integer type);
}
