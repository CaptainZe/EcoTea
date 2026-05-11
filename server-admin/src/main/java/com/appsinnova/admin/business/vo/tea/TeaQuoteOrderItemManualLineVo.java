package com.appsinnova.admin.business.vo.tea;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeaQuoteOrderItemManualLineVo {
    private Long itemId;
    private BigDecimal manualAmount;
    private String manualRemark;
}
