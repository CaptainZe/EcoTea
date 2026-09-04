package com.ecotea.api.handler.wx;

import com.ecotea.api.service.wx.WxKeywordReplyService;
import lombok.RequiredArgsConstructor;
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
 * 文本消息：关键词客服 / 品牌品名查价。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextMsgHandler implements WxMpMessageHandler {

    private final WxKeywordReplyService wxKeywordReplyService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context,
                                    WxMpService wxMpService,
                                    WxSessionManager sessionManager) throws WxErrorException {
        String content = wxMessage.getContent();
        log.info("wx mp text, openid={}, contentLength={}",
                wxMessage.getFromUser(), content == null ? 0 : content.length());
        String reply = wxKeywordReplyService.reply(content);
        return WxMpXmlOutMessage.TEXT()
                .content(reply)
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
    }
}
