package com.appsinnova.admin.business.domain.tea;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tea_quote_order_item")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class TeaQuoteOrderItem implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private String skuBrand;
    private String skuSpec;
    private String skuProductionBatch;

    private Integer appearanceCondition;
    private Integer hasBag;
    private BigDecimal baseRecyclePrice;
    private BigDecimal amount;
    private Integer quantity;

    private Long updateTime;
    private Long createTime;
}