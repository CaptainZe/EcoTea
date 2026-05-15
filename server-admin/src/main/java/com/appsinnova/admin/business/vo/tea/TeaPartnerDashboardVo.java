package com.appsinnova.admin.business.vo.tea;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 合作方仪表盘聚合数据（由 {@link com.appsinnova.admin.business.service.tea.TeaPartnerDashboardService} 填充）
 */
@Data
public class TeaPartnerDashboardVo {

    private Long totalCount = 0L;
    private Long signedCount = 0L;
    private Long terminatedCount = 0L;

    /** 按月新增合作方（create_time） */
    private List<MonthTrendPoint> growthTrendByMonth = new ArrayList<>();

    private List<StatusSlice> statusDistribution = new ArrayList<>();

    /** 对接客服负责的合作方数量排行 */
    private List<LiaisonRankRow> liaisonPartnerRankList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthTrendPoint {
        private String monthLabel;
        private Long newPartnerCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusSlice {
        private Integer status;
        private Long count;
        /** 字典 TEA_PARTNER_STATUS */
        private String statusLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiaisonRankRow {
        private Long liaisonUserId;
        private String displayName;
        private Long partnerCount;
    }
}
