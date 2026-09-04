package com.ecotea.api.vo.wx;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 客服 config（type=3）：{"items":[{"wechat_id","qr_image_url"},...]}。
 */
@Data
public class WxCustomerServiceConfig {

    private List<WxCustomerServiceItem> items = new ArrayList<>();
}
