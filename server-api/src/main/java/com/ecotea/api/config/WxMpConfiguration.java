package com.ecotea.api.config;

import com.ecotea.api.handler.wx.SubscribeHandler;
import com.ecotea.api.handler.wx.TextMsgHandler;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号消息路由（同步处理，便于被动回复）。
 */
@Configuration
@RequiredArgsConstructor
public class WxMpConfiguration {

    private final SubscribeHandler subscribeHandler;
    private final TextMsgHandler textMsgHandler;

    @Bean
    public WxMpMessageRouter wxMpMessageRouter(WxMpService wxMpService) {
        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);
        // 关注
        router.rule().async(false)
                .msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.EventType.SUBSCRIBE)
                .handler(subscribeHandler)
                .end();
        // 文本
        router.rule().async(false)
                .msgType(WxConsts.XmlMsgType.TEXT)
                .handler(textMsgHandler)
                .end();
        return router;
    }
}
