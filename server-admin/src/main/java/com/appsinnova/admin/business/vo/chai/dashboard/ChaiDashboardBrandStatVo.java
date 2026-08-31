package com.appsinnova.admin.business.vo.chai.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 茶品牌 · SPU 数量统计项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChaiDashboardBrandStatVo {

    private Long brandId;
    private String brandName;
    private Long count;
}
