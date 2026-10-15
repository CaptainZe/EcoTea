package com.ecotea.api.vo.employee;

import com.ecotea.api.vo.tenant.TenantOptionVO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeeLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private Long employeeId;
    private String username;
    private String realName;
    /** 是否已选工作店铺（登录后一般为 false） */
    private boolean tenantSelected;
    private Long tenantId;
    private String tenantName;
    private List<String> permCodes = new ArrayList<>();
    private List<TenantOptionVO> tenants = new ArrayList<>();
}
