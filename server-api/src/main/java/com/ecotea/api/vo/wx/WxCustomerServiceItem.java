package com.ecotea.api.vo.wx;

import lombok.Data;

/**
 * 单个客服：微信号为二维码说明。
 */
@Data
public class WxCustomerServiceItem {

    private String wechatId;
    private String qrImageUrl;
}
