package com.appsinnova.admin.business.domain.chai;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "chai_sku")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiSku implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long spuId;
    private String skuCode;
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
    private BigDecimal salePrice;
    private BigDecimal recyclePrice;
    private Integer recyclePriceReducePer;
    private BigDecimal recyclePriceReduceNoBag;
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;

    /** 规格表单拆分 */
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
    private String salePriceShow;
    @Transient
    private String recyclePriceShow;
    @Transient
    private String recycleReduceAmountShow;
    @Transient
    private String recyclePriceReduceNoBagShow;
    @Transient
    private java.util.List<String> showImageList;
    @Transient
    private java.util.List<String> realImageList;

    /** 查询：按父 SPU 编码 */
    @Transient
    private String querySpuCode;
}
