package com.appsinnova.admin.business.repository.wx;

import com.appsinnova.admin.business.domain.wx.WxMpMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface WxMpMenuRepository extends JpaRepository<WxMpMenu, Long>, JpaSpecificationExecutor<WxMpMenu> {

    Integer deleteByIdIn(List<Long> idList);

    WxMpMenu findFirstByAppId(String appId);
}
