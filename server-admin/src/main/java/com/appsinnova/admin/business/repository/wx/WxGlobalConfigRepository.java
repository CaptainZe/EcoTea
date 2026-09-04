package com.appsinnova.admin.business.repository.wx;

import com.appsinnova.admin.business.domain.wx.WxGlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface WxGlobalConfigRepository extends JpaRepository<WxGlobalConfig, Long>, JpaSpecificationExecutor<WxGlobalConfig> {

    Integer deleteByIdIn(List<Long> idList);

    WxGlobalConfig findFirstByType(Integer type);
}
