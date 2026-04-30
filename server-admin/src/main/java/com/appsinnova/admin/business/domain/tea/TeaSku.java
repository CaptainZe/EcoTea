package com.appsinnova.admin.business.domain.tea;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tea_sku")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class TeaSku implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String skuCode;
    private String name;
    private Integer brand;
    private Integer type;
    private Integer grade;
    private String spec;
    private Integer year;
    private String productionBatch;
    private Integer expiration;
    private String barcode;
    private String imageUrls;
    private String realImageUrls;
    private BigDecimal officialPrice;
    private BigDecimal salePrice;
    private BigDecimal recyclePrice;
    private Integer recyclePriceReducePer;
    private BigDecimal recyclePriceReduceNoBag;
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private Integer imageConfigured;
    @Transient
    private Integer realImageConfigured;
    @Transient
    private String imageShow;
    @Transient
    private String realImageShow;
    @Transient
    private String salePriceShow;
    @Transient
    private String recycleReduceAmountShow;
}