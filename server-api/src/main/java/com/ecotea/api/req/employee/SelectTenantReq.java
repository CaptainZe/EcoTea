package com.ecotea.api.req.employee;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SelectTenantReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务租户 ID */
    @NotNull(message = "店铺必选")
    private Long tenantId;
}
