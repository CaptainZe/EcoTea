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

    /** 按省（PCA 编码）统计合作方数量 */
    private List<RegionStatRow> provinceStatList = new ArrayList<>();

    /** 对接客服负责的合作方数量排行 */
    private List<LiaisonRankRow> liaisonPartnerRankList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionStatRow {
        /** 省/市 PCA 编码 */
        private String regionCode;
        /** 展示名称（由编码解析） */
        private String regionName;
        private Long partnerCount;
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
