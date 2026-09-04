package com.ecotea.api.vo.wx;

import lombok.Data;

/**
 * 销售 H5 文案（type=4）：notice_title / notice_body / banner_text。
 */
@Data
public class WxSaleH5CopyConfig {

    private String noticeTitle;
    private String noticeBody;
    private String bannerText;
}
