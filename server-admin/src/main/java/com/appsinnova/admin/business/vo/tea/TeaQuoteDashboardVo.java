package com.appsinnova.admin.business.vo.tea;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 报价单仪表盘聚合数据（由 TeaQuoteOrderDashboardService 填充）
 */
@Data
public class TeaQuoteDashboardVo {

    private Long orderCount = 0L;
    private BigDecimal totalAmountSum = BigDecimal.ZERO;
    private BigDecimal totalManualAmountSum = BigDecimal.ZERO;
    private Long totalQuantitySum = 0L;

    private List<StatusSlice> statusDistribution = new ArrayList<>();
    private List<TrendPoint> trendSeries = new ArrayList<>();
    private List<SkuTopRow> skuTopList = new ArrayList<>();
    /** 报价明细按品牌汇总数量 Top20 */
    private List<BrandQuoteQtyRow> brandQuoteQtyTopList = new ArrayList<>();
    /** 报价明细按品牌汇总人工调整后金额 Top20 */
    private List<BrandQuoteManualAmtRow> brandQuoteManualAmtTopList = new ArrayList<>();
    private List<UserRankRow> userRankList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusSlice {
        private Integer status;
        private Long count;
        /** 字典 TEA_QUOTE_ORDER_STATUS */
        private String statusLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String bucketLabel;
        private Long orderCount;
        private BigDecimal totalAmount;
        private BigDecimal totalManualAmount;
        private Long totalQuantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuTopRow {
        private Long skuId;
        private String skuCode;
        /** 明细快照，可能为字典编码或文本 */
        private String skuBrand;
        private String skuName;
        private String skuSpec;
        private String skuProductionBatch;
        private Long quantitySum;
        private BigDecimal amountSum;
        private Long orderDistinctCount;
        /** 编码 / 品牌 / 名称 / 规格 / 生产批次 组合文案 */
        private String productInfoSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandQuoteQtyRow {
        private String brandLabel;
        private Long quantitySum;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandQuoteManualAmtRow {
        private String brandLabel;
        private BigDecimal manualAmountSum;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRankRow {
        private Long userId;
        private String userName;
        private Long orderCount;
        private BigDecimal amountSum;
    }
}
