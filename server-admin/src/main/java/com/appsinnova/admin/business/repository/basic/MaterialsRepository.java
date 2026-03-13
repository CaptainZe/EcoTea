package com.appsinnova.admin.business.repository.basic;

import com.appsinnova.admin.business.domain.basic.MaterialsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MaterialsRepository extends JpaRepository<MaterialsModel, Long>, JpaSpecificationExecutor<MaterialsModel> {

    Integer deleteByIdIn(List<Long> idList);
}