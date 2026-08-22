package com.appsinnova.admin.business.vo.chai.dashboard;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChaiDashboardStackedSeriesVo {
    private String name;
    private List<Long> data = new ArrayList<>();
}
