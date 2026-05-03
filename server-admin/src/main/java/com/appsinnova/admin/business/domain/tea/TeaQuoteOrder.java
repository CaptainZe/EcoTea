package com.appsinnova.admin.business.domain.tea;

import com.appsinnova.admin.business.vo.base.PayInfoVo;
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
import javax.persistence.Transient;
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
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private Integer totalQuantity;

    private Integer expressCompany;
    private String expressNo;
    private Integer payMethod;
    private String payInfo;
    @Transient
    private PayInfoVo payInfoVo;

    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;
}