package com.appsinnova.admin.business.vo.chai.dashboard;

import lombok.Data;

@Data
public class ChaiDashboardVo {
    private Integer statDays;
    private String scopeTip;

    private Long spuCount;
    private Long skuCount;
    private Long onlineSkuCount;
    private Long offlineSkuCount;

    private Long stockTotalQty;
    private Long stockSkuCount;
    private Long positiveStockSkuCount;
    private Long warehouseCount;

    private Long billInCount;
    private Long billOutCount;
    private Long billTransferCount;
    private Long billTotalCount;

    private ChaiDashboardStackedBarVo billTrendChart = new ChaiDashboardStackedBarVo();
}
