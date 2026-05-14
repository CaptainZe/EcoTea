package com.appsinnova.admin.business.controller.tea;

import com.appsinnova.admin.business.common.enums.tea.TeaQuoteTrendGranularity;
import com.appsinnova.admin.business.service.tea.TeaQuoteOrderDashboardService;
import com.appsinnova.admin.business.service.tea.TeaSkuDashboardService;
import com.appsinnova.admin.business.vo.tea.TeaQuoteDashboardVo;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 茶品相关仪表盘（SKU、报价单等）
 */
@Controller
@RequestMapping("/business/tea/teaOverview")
@RequiredArgsConstructor
public class TeaOverviewController {

    private final TeaSkuDashboardService teaSkuDashboardService;
    private final TeaQuoteOrderDashboardService teaQuoteOrderDashboardService;

    @GetMapping("/sku")
    @RequiresPermissions("business:tea:teaOverview:sku")
    public String skuDashboard(Model model) {
        model.addAttribute("statVo", teaSkuDashboardService.buildStatOverview());
        return "/business/tea/teaOverview/skuDashboard";
    }

    @GetMapping("/quote")
    @RequiresPermissions("business:tea:teaOverview:quote")
    public String quoteDashboard(Model model) {
        TeaQuoteDashboardVo dash = teaQuoteOrderDashboardService.buildFullDashboard();
        model.addAttribute("quoteDash", dash);
        model.addAttribute("quoteTrendJson", JsonUtils.writeValueAsString(dash.getTrendSeries()));
        model.addAttribute("quoteStatusJson", JsonUtils.writeValueAsString(dash.getStatusDistribution()));
        model.addAttribute("quoteBrandQtyJson", JsonUtils.writeValueAsString(dash.getBrandQuoteQtyTopList()));
        model.addAttribute("quoteBrandAmtJson", JsonUtils.writeValueAsString(dash.getBrandQuoteManualAmtTopList()));
        return "/business/tea/teaOverview/quoteDashboard";
    }

    @GetMapping("/quote/trend")
    @RequiresPermissions("business:tea:teaOverview:quote")
    @ResponseBody
    public ResultVo<?> quoteTrend(@RequestParam(value = "granularity", required = false, defaultValue = "day") String granularity) {
        TeaQuoteTrendGranularity g = TeaQuoteTrendGranularity.fromParam(granularity);
        return ResultVoUtil.success(teaQuoteOrderDashboardService.loadTrendSeries(g));
    }
}
