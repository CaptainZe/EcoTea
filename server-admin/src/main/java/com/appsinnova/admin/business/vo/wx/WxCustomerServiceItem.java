package com.appsinnova.admin.business.vo.wx;

import lombok.Data;

/**
 * 单个客服：微信号为二维码说明，二者均必填。
 */
@Data
public class WxCustomerServiceItem {

    /** 微信号（展示在二维码旁的说明） */
    private String wechatId;

    /** 客服二维码图片 URL */
    private String qrImageUrl;
}
