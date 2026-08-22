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
@Table(name = "chai_stock_bill")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiStockBill implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String billNo;
    private Integer billType;
    private Integer status;
    private Integer reason;
    private Long handlerId;
    private String handlerName;
    private Long fromWhId;
    private Long toWhId;
    private Integer totalQty;
    private BigDecimal totalAmount;
    private String remark;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private String fromWhName;
    @Transient
    private String toWhName;
    @Transient
    private List<ChaiStockBillItem> itemList;
}
