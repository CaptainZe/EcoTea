package com.ecotea.api.handler.wx;

import com.ecotea.api.service.wx.WxGlobalConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 关注事件：被动回复欢迎语（读 wx_global_config type=1）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscribeHandler implements WxMpMessageHandler {

    /** 无库配置时的兜底（勿含 EcoTea） */
    private static final String FALLBACK_WELCOME =
            "欢迎关注！本号提供茶叶回收与现货选购，请点底部菜单了解，或直接回复品牌/品名查询。";

    private final WxGlobalConfigService wxGlobalConfigService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context,
                                    WxMpService wxMpService,
                                    WxSessionManager sessionManager) throws WxErrorException {
        log.info("wx mp subscribe, openid={}, createTime={}", wxMessage.getFromUser(), wxMessage.getCreateTime());

        String welcome = wxGlobalConfigService.getSubscribeWelcomeText();
        if (!StringUtils.hasText(welcome)) {
            log.warn("wx mp subscribe welcome missing, use fallback");
            welcome = FALLBACK_WELCOME;
        }

        return WxMpXmlOutMessage.TEXT()
                .content(welcome)
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
    }
}
