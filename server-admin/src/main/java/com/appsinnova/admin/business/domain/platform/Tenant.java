package com.appsinnova.admin.business.domain.platform;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "tenant")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class Tenant implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务租户 ID，创建后不可改 */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private String name;
    /** 0停用 1启用 */
    private Integer status;
    private Integer employeeQuota;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contactName;
    private String contactMobile;
    private String remark;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private String provinceName;
    @Transient
    private String cityName;
    @Transient
    private String districtName;
}
