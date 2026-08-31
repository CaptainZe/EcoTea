package com.appsinnova.admin.business.vo.chai.dashboard;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    /** 茶品牌 SPU 数量分布（未删除；与近 N 天单据无关） */
    private List<ChaiDashboardBrandStatVo> spuBrandStatList = new ArrayList<>();

    private ChaiDashboardStackedBarVo billTrendChart = new ChaiDashboardStackedBarVo();
}
