package com.ecotea.api.service.wx;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.ecotea.api.common.constant.ErrorCode;
import com.ecotea.api.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 小程序登录凭证校验（WxJava {@link WxMaService}）。
 * <p>供员工微信绑定 / 一键登录调用；不直接暴露 HTTP。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMaCodeSessionService {

    private final WxMaService wxMaService;

    /**
     * {@code wx.login} 得到的 code → openid（及可选 unionid）。
     */
    public WxMaCodeSession exchangeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "微信登录码不能为空");
        }
        try {
            WxMaJscode2SessionResult result = wxMaService.getUserService().getSessionInfo(code.trim());
            if (result == null || !StringUtils.hasText(result.getOpenid())) {
                throw new BizException("微信登录失败，请重试");
            }
            WxMaCodeSession session = new WxMaCodeSession();
            session.setOpenid(result.getOpenid());
            session.setUnionid(result.getUnionid());
            return session;
        } catch (WxErrorException e) {
            log.warn("wx miniapp jscode2session fail: errCode={}, msg={}",
                    e.getError() != null ? e.getError().getErrorCode() : null,
                    e.getMessage());
            throw new BizException("微信登录失败，请重试");
        }
    }
}
