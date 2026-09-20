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

    /** 查询：过账日起 yyyy-MM-dd */
    @Transient
    private String queryCreateDateStart;
    /** 查询：过账日止 yyyy-MM-dd */
    @Transient
    private String queryCreateDateEnd;
    /** 查询：过账时间起（毫秒，含当日 00:00） */
    @Transient
    private Long queryCreateTimeStart;
    /** 查询：过账时间止（毫秒，含当日 23:59:59.999） */
    @Transient
    private Long queryCreateTimeEnd;
}
