package com.appsinnova.admin.business.vo.tea;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeaSkuStatVo {

    private Long totalCount = 0L;
    private Long onlineCount = 0L;
    private Long offlineCount = 0L;
    private List<StarLevelStatItem> starLevelStatList = new ArrayList<>();
    private List<BrandStatItem> brandStatList = new ArrayList<>();

    @Data
    public static class StarLevelStatItem {
        private Integer starLevel;
        private Long count;
        private String label;

        public StarLevelStatItem(Integer starLevel, Long count, String label) {
            this.starLevel = starLevel;
            this.count = count;
            this.label = label;
        }
    }

    @Data
    public static class BrandStatItem {
        private Integer brand;
        private Long count;

        public BrandStatItem(Integer brand, Long count) {
            this.brand = brand;
            this.count = count;
        }
    }
}
