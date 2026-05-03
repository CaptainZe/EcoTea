package com.appsinnova.admin.business.repository.sys;

import com.appsinnova.admin.business.domain.sys.DailySequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DailySequenceRepository extends JpaRepository<DailySequence, Long>, JpaSpecificationExecutor<DailySequence> {

    DailySequence findFirstByBizTypeAndBizDate(Integer bizType, String bizDate);
}
