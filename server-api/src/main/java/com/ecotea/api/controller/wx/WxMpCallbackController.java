package com.ecotea.api.controller.wx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信公众号（订阅号）服务器回调。
 * <p>
 * 正式环境 URL 规划：{@code https://api.ecotea.cn/wx/mp/callback}
 * （备案与证书就绪后再在公众平台配置。）
 */
@Slf4j
@RestController
@RequestMapping("/wx/mp")
@RequiredArgsConstructor
public class WxMpCallbackController {

    private final WxMpService wxMpService;
    private final WxMpMessageRouter wxMpMessageRouter;

    /**
     * 公众平台「服务器配置」提交时的 GET 校验：验签通过后原样返回 echostr。
     */
    @GetMapping(value = "/callback", produces = "text/plain;charset=utf-8")
    public String authGet(@RequestParam(name = "signature", required = false) String signature,
                          @RequestParam(name = "timestamp", required = false) String timestamp,
                          @RequestParam(name = "nonce", required = false) String nonce,
                          @RequestParam(name = "echostr", required = false) String echostr) {
        log.info("wx mp GET callback, signature={}, timestamp={}, nonce={}, echostrPresent={}",
                signature, timestamp, nonce, StringUtils.hasText(echostr));

        if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(echostr)) {
            log.warn("wx mp GET callback rejected: missing required query params");
            return "非法请求";
        }

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            log.warn("wx mp GET callback signature check failed, timestamp={}, nonce={}", timestamp, nonce);
            return "非法请求";
        }

        log.info("wx mp GET callback signature ok, echo echostr");
        return echostr;
    }

    /**
     * 接收微信推送的消息/事件（明文或安全模式 AES）。
     */
    @PostMapping(value = "/callback", produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam(name = "openid", required = false) String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        log.info("wx mp POST callback, openid={}, encType={}, timestamp={}, nonce={}, bodyLength={}",
                openid, encType, timestamp, nonce, requestBody == null ? 0 : requestBody.length());

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            log.warn("wx mp POST callback signature check failed, openid={}, timestamp={}, nonce={}",
                    openid, timestamp, nonce);
            return "非法请求";
        }

        if (!StringUtils.hasText(requestBody)) {
            log.warn("wx mp POST empty body, openid={}", openid);
            return "";
        }

        try {
            WxMpXmlMessage inMessage;
            if (!StringUtils.hasText(encType)) {
                inMessage = WxMpXmlMessage.fromXml(requestBody);
            } else if ("aes".equalsIgnoreCase(encType)) {
                inMessage = WxMpXmlMessage.fromEncryptedXml(
                        requestBody, wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
                log.info("wx mp POST decrypted, msgType={}, event={}, fromUser={}",
                        inMessage.getMsgType(), inMessage.getEvent(), inMessage.getFromUser());
            } else {
                log.warn("wx mp POST unknown encrypt_type={}", encType);
                return "不可识别的加密类型";
            }

            log.info("wx mp POST routed, msgType={}, event={}, fromUser={}, msgId={}",
                    inMessage.getMsgType(), inMessage.getEvent(), inMessage.getFromUser(), inMessage.getMsgId());

            WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
            if (outMessage == null) {
                log.info("wx mp POST no out message (empty reply), msgType={}, event={}",
                        inMessage.getMsgType(), inMessage.getEvent());
                return "";
            }

            String out;
            if (!StringUtils.hasText(encType)) {
                out = outMessage.toXml();
            } else {
                out = outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
            }
            log.info("wx mp POST reply ok, outLength={}", out.length());
            return out;
        } catch (Exception e) {
            log.error("wx mp POST handle error, openid={}, encType={}", openid, encType, e);
            return "";
        }
    }
}
