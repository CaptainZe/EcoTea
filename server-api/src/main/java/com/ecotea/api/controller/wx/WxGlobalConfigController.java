package com.ecotea.api.controller.wx;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.wx.WxGlobalConfigService;
import com.ecotea.api.vo.wx.WxCustomerServiceConfig;
import com.ecotea.api.vo.wx.WxRecycleDescConfig;
import com.ecotea.api.vo.wx.WxSaleH5CopyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信通用配置只读（H5 公开）。
 */
@RestController
@RequestMapping("/wx/globalConfig")
@RequiredArgsConstructor
public class WxGlobalConfigController {

    private final WxGlobalConfigService wxGlobalConfigService;

    /**
     * 销售 H5 文案（弹窗 + 顶栏）。无配置时 data 为 null。
     */
    @GetMapping("/saleH5Copy")
    public ApiResult<WxSaleH5CopyConfig> saleH5Copy() {
        return ApiResult.ok(wxGlobalConfigService.getSaleH5Copy());
    }

    /**
     * 回收说明。无配置时 data 为 null。
     */
    @GetMapping("/recycleDesc")
    public ApiResult<WxRecycleDescConfig> recycleDesc() {
        return ApiResult.ok(wxGlobalConfigService.getRecycleDesc());
    }

    /**
     * 客服列表（微信号 + 二维码）。无配置时 data 为 null。
     */
    @GetMapping("/customerService")
    public ApiResult<WxCustomerServiceConfig> customerService() {
        return ApiResult.ok(wxGlobalConfigService.getCustomerService());
    }
}
