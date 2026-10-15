package com.appsinnova.admin.business.domain.platform;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "employee")
@DynamicInsert
@DynamicUpdate
public class Employee implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String salt;
    private String realName;
    private String mobile;
    private String wxOpenid;
    private String wxUnionid;
    private Long wxBindTime;
    /** 0停用 1启用 */
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;

    /** 列表筛选：按业务租户 */
    @Transient
    private Long filterTenantId;
    /** 列表展示：挂店摘要 */
    @Transient
    private String tenantSummary;
    /** 新建时明文密码（不落库） */
    @Transient
    private String plainPassword;
}
