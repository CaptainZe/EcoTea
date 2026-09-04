package com.appsinnova.admin.business.vo.wx;

import lombok.Data;

/**
 * 销售 H5 文案 config：{"notice_title","notice_body","banner_text"}（JsonUtils SNAKE_CASE）。
 */
@Data
public class WxSaleH5CopyConfig {

    /** 进页弹窗标题 */
    private String noticeTitle;
    /** 进页弹窗正文（二次销售等说明） */
    private String noticeBody;
    /** 列表顶栏说明 */
    private String bannerText;
}
