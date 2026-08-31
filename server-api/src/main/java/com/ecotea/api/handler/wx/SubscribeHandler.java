package com.ecotea.api.handler.wx;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 关注事件：被动回复欢迎语。
 */
@Slf4j
@Component
public class SubscribeHandler implements WxMpMessageHandler {

    private static final String WELCOME = "欢迎关注 EcoTea！后续可在此接收茶叶相关服务通知。";

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context,
                                    WxMpService wxMpService,
                                    WxSessionManager sessionManager) throws WxErrorException {
        log.info("wx mp subscribe, openid={}, createTime={}", wxMessage.getFromUser(), wxMessage.getCreateTime());
        return WxMpXmlOutMessage.TEXT()
                .content(WELCOME)
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
    }
}
