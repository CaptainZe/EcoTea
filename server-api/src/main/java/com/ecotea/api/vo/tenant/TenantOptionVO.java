package com.ecotea.api.vo.tenant;

import lombok.Data;

import java.io.Serializable;

/** 选店列表轻量出参 */
@Data
public class TenantOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private String name;
    private Integer status;
}
