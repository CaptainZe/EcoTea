package com.ecotea.api.common.enums.wx;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * wx_global_config.type（与 admin WxGlobalConfigType、PRD_WX_MP 对齐）。
 */
@Getter
@AllArgsConstructor
public enum WxGlobalConfigType {

    /** 关注欢迎语：{"text":"..."} */
    SUBSCRIBE_WELCOME(1, "关注欢迎语"),

    /** 回收说明：{"title":"...","body":"...","ctaText":"...","ctaUrl":"..."} */
    RECYCLE_DESC(2, "回收说明"),

    /** 客服：{"items":[{"wechat_id","qr_image_url"},...]} */
    CUSTOMER_SERVICE(3, "客服"),

    /** 销售 H5 文案：{"noticeTitle":"...","noticeBody":"...","bannerText":"..."} */
    SALE_H5_COPY(4, "销售H5文案"),
    ;

    private final Integer code;
    private final String message;

    public static WxGlobalConfigType ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WxGlobalConfigType item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
