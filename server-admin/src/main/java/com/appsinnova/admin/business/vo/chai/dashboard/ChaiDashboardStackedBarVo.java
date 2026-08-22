package com.appsinnova.admin.business.vo.chai.dashboard;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChaiDashboardStackedBarVo {
    private List<String> categories = new ArrayList<>();
    private List<ChaiDashboardStackedSeriesVo> series = new ArrayList<>();
}
