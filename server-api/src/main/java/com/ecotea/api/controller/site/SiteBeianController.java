package com.ecotea.api.controller.site;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.config.BeianProperties;
import com.ecotea.api.vo.site.BeianVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点备案信息（H5 通用页脚）。
 */
@RestController
@RequestMapping("/site")
@RequiredArgsConstructor
public class SiteBeianController {

    private final BeianProperties beianProperties;

    @GetMapping("/beian")
    public ApiResult<BeianVO> beian() {
        BeianVO vo = new BeianVO();
        vo.setIcpText(trimToEmpty(beianProperties.getIcpText()));
        vo.setIcpUrl(trimToEmpty(beianProperties.getIcpUrl()));
        vo.setMpsText(trimToEmpty(beianProperties.getMpsText()));
        vo.setMpsUrl(trimToEmpty(beianProperties.getMpsUrl()));
        return ApiResult.ok(vo);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
