package com.appsinnova.admin.business.domain.chai;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "chai_warehouse")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiWarehouse implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    /** 列表/筛选展示用简称，必填且唯一 */
    @Column(name = "short_name", nullable = false, length = 16)
    private String shortName;
    private String province;
    private String city;
    private String district;
    private String address;
    private Integer orderNum;
    private Integer status;
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
