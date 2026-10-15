package com.appsinnova.admin.business.domain.platform;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "admin_tenant_scope")
@DynamicInsert
@DynamicUpdate
public class AdminTenantScope implements Serializable {

    /** 平台数据范围：tenant_id = 0 */
    public static final long PLATFORM_TENANT_ID = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    /** 0=平台；>0=tenant.tenant_id */
    private Long tenantId;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private String username;
    @Transient
    private String nickname;
    @Transient
    private String tenantName;
}
