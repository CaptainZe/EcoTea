package com.appsinnova.admin.business.domain.chai;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "chai_spu")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiSpu implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String spuCode;
    private Integer starLevel;
    private String name;
    private Long brand;
    private Long expiration;
    private Integer type;
    private Integer grade;
    private Integer year;
    private Integer prodBatch;
    private String spec;
    private String showImageUrls;
    private String realImageUrls;
    private BigDecimal officialPrice;
    private Integer status;
    /** 0有效 1已删除；不与 status（上下架）混用 */
    private Integer deleted;
    private String operator;
    private Long updateTime;
    private Long createTime;

    /** 规格表单拆分字段（不入库） */
    @Transient
    private BigDecimal totalNetWeight;
    @Transient
    private BigDecimal unitWeight;
    @Transient
    private Integer unitCount;
    @Transient
    private Integer unitLabel;

    /** 列表展示 */
    @Transient
    private String brandName;
    @Transient
    private String expirationName;
    @Transient
    private String specShow;
    @Transient
    private String officialPriceShow;
    @Transient
    private List<String> showImageList;
    @Transient
    private List<String> realImageList;
    /** 列表：下属 SKU 数量 */
    @Transient
    private Long skuCount;
}
