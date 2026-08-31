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
 * 文本消息：简单回声，便于联调。
 */
@Slf4j
@Component
public class TextMsgHandler implements WxMpMessageHandler {

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context,
                                    WxMpService wxMpService,
                                    WxSessionManager sessionManager) throws WxErrorException {
        String content = wxMessage.getContent();
        log.info("wx mp text, openid={}, contentLength={}",
                wxMessage.getFromUser(), content == null ? 0 : content.length());
        String reply = content == null ? "已收到" : ("收到：" + content);
        return WxMpXmlOutMessage.TEXT()
                .content(reply)
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
    }
}
