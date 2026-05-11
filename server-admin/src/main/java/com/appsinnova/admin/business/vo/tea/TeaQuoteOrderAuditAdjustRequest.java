package com.appsinnova.admin.business.vo.tea;

import lombok.Data;

import java.util.List;

@Data
public class TeaQuoteOrderAuditAdjustRequest {
    private List<TeaQuoteOrderItemManualLineVo> lines;
}
