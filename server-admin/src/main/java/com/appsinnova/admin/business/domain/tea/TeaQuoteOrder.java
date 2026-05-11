package com.appsinnova.admin.business.domain.tea;

import com.appsinnova.admin.business.vo.base.PayInfoVo;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tea_quote_order")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class TeaQuoteOrder implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNo;
    private Integer type;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private BigDecimal totalManualAmount;

    private Integer expressCompany;
    private String expressNo;
    private Integer payMethod;
    private String payInfo;

    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private PayInfoVo payInfoVo;
    @Transient
    private String itemBrandSummary;

    /** 列表查询：字典 YES_OR_NO，1=有改价（totalManualAmount ≠ totalAmount），0=无改价 */
    @Transient
    private Integer manualPriceAdjust;

    /**
     * 订单级是否有人工改价（用于列表/详情展示）。
     */
    public boolean isTotalManualAdjusted() {
        if (totalAmount == null || totalManualAmount == null) {
            return false;
        }
        return totalManualAmount.compareTo(totalAmount) != 0;
    }
}