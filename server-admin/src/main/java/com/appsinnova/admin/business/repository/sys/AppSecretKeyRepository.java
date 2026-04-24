package com.appsinnova.admin.business.repository.sys;

import com.appsinnova.admin.business.domain.sys.AppSecretKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AppSecretKeyRepository extends JpaRepository<AppSecretKey, Long>, JpaSpecificationExecutor<AppSecretKey> {

    Integer deleteByIdIn(List<Long> idList);
}