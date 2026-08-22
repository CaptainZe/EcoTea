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
@Table(name = "chai_stock_bill_item")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiStockBillItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long billId;
    private Long skuId;
    private String skuCode;
    private String name;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    /** 行备注：破损、无提袋等 */
    private String remark;
    @Column(columnDefinition = "text")
    private String skuSnap;

    @Transient
    private String brandName;
    @Transient
    private Integer year;
    @Transient
    private Integer prodBatch;
    @Transient
    private String prodBatchName;
    @Transient
    private String specShow;
    @Transient
    private String halfYearShow;
    @Transient
    private List<String> showImageList;
}
