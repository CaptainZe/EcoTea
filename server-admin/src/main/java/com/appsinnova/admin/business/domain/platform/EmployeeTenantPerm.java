package com.appsinnova.admin.business.domain.platform;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "employee_tenant_perm")
@DynamicInsert
@DynamicUpdate
public class EmployeeTenantPerm implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private Long tenantId;
    private String permCode;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
