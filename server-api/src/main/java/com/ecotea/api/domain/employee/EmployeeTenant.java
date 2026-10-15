package com.ecotea.api.domain.employee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("employee_tenant")
public class EmployeeTenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    /** tenant.tenant_id */
    private Long tenantId;
    /** 0停用 1启用 */
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
