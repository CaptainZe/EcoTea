package com.ecotea.api.domain.employee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("employee_tenant_perm")
public class EmployeeTenantPerm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long tenantId;
    private String permCode;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
