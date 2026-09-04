package com.ecotea.api.controller.wx;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.wx.WxMpMenuService;
import com.ecotea.api.vo.wx.WxMpMenuPublishReq;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信自定义菜单（管理写操作，需 X-EcoTea-Api-Key）。
 */
@RestController
@RequestMapping("/wx/mp/menu")
@RequiredArgsConstructor
public class WxMpMenuController {

    private final WxMpMenuService wxMpMenuService;

    /**
     * 按 appId 或 id 读取 wx_mp_menu.config 并 menuCreate。
     */
    @PostMapping("/publish")
    public ApiResult<Void> publish(@RequestBody WxMpMenuPublishReq req) {
        if (req == null) {
            req = new WxMpMenuPublishReq();
        }
        wxMpMenuService.publish(req.getAppId(), req.getId());
        return ApiResult.ok();
    }
}
