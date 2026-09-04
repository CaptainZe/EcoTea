package com.appsinnova.admin.business.common.utils.wx;

import com.appsinnova.admin.business.common.enums.wx.WxGlobalConfigType;
import com.appsinnova.admin.business.common.utils.JsonUtils;
import com.appsinnova.admin.business.domain.wx.WxGlobalConfig;
import com.appsinnova.admin.business.vo.wx.WxCustomerServiceConfig;
import com.appsinnova.admin.business.vo.wx.WxCustomerServiceItem;
import com.appsinnova.admin.business.vo.wx.WxRecycleDescConfig;
import com.appsinnova.admin.business.vo.wx.WxSaleH5CopyConfig;
import com.appsinnova.admin.business.vo.wx.WxSubscribeWelcomeConfig;
import org.apache.commons.lang3.StringUtils;

/**
 * 微信通用配置：列表展示摘要等。
 */
public final class WxGlobalConfigUtil {

    private WxGlobalConfigUtil() {
    }

    public static String buildConfigDisplay(WxGlobalConfig item) {
        if (item == null || StringUtils.isBlank(item.getConfig()) || item.getType() == null) {
            return "";
        }
        WxGlobalConfigType type = WxGlobalConfigType.ofCode(item.getType());
        if (type == null) {
            return item.getConfig();
        }
        switch (type) {
            case SUBSCRIBE_WELCOME:
                return abbreviate(parseText(item.getConfig()));
            case RECYCLE_DESC:
                return abbreviate(parseRecycle(item.getConfig()), 200);
            case CUSTOMER_SERVICE:
                return abbreviate(parseCs(item.getConfig()));
            case SALE_H5_COPY:
                return abbreviate(parseSaleH5(item.getConfig()), 200);
            default:
                return item.getConfig();
        }
    }

    private static String parseText(String configJson) {
        WxSubscribeWelcomeConfig content = JsonUtils.readValue(configJson, WxSubscribeWelcomeConfig.class);
        return content == null ? null : content.getText();
    }

    private static String parseRecycle(String configJson) {
        WxRecycleDescConfig content = JsonUtils.readValue(configJson, WxRecycleDescConfig.class);
        if (content == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(content.getTitle())) {
            sb.append("【标题】").append(content.getTitle().trim());
        }
        if (StringUtils.isNotBlank(content.getBody())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(content.getBody().trim().replace('\n', ' '));
        }
        if (StringUtils.isNotBlank(content.getCtaText()) || StringUtils.isNotBlank(content.getCtaUrl())) {
            if (sb.length() > 0) {
                sb.append(" ｜ ");
            }
            sb.append("【按钮】");
            if (StringUtils.isNotBlank(content.getCtaText())) {
                sb.append(content.getCtaText().trim());
            }
            if (StringUtils.isNotBlank(content.getCtaUrl())) {
                if (StringUtils.isNotBlank(content.getCtaText())) {
                    sb.append(' ');
                }
                sb.append(content.getCtaUrl().trim());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String parseCs(String configJson) {
        WxCustomerServiceConfig content = JsonUtils.readValue(configJson, WxCustomerServiceConfig.class);
        if (content == null || content.getItems() == null || content.getItems().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.getItems().size(); i++) {
            WxCustomerServiceItem it = content.getItems().get(i);
            if (it == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(it.getWechatId() != null ? it.getWechatId().trim() : "");
            if (StringUtils.isNotBlank(it.getQrImageUrl())) {
                sb.append("（有码）");
            }
        }
        return sb.length() == 0 ? null : ("客服" + content.getItems().size() + "人：" + sb);
    }

    private static String parseSaleH5(String configJson) {
        WxSaleH5CopyConfig content = JsonUtils.readValue(configJson, WxSaleH5CopyConfig.class);
        if (content == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(content.getNoticeTitle())) {
            sb.append("【弹窗】").append(content.getNoticeTitle().trim());
        }
        if (StringUtils.isNotBlank(content.getNoticeBody())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(content.getNoticeBody().trim().replace('\n', ' '));
        }
        if (StringUtils.isNotBlank(content.getBannerText())) {
            if (sb.length() > 0) {
                sb.append(" ｜ ");
            }
            sb.append("【顶栏】").append(content.getBannerText().trim().replace('\n', ' '));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String abbreviate(String text) {
        return abbreviate(text, 120);
    }

    private static String abbreviate(String text, int maxLen) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String t = text.trim();
        if (t.length() > maxLen) {
            return t.substring(0, maxLen) + "…";
        }
        return t;
    }
}
