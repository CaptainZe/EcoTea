package com.ecotea.api.mapper.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecotea.api.domain.tenant.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
