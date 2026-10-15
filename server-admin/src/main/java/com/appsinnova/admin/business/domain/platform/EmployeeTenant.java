package com.appsinnova.admin.business.domain.platform;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "employee_tenant")
@DynamicInsert
@DynamicUpdate
public class EmployeeTenant implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    /** tenant.tenant_id */
    private Long tenantId;
    /** 0停用 1启用 */
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private String tenantName;
    @Transient
    private List<String> permCodes = new ArrayList<>();
}
