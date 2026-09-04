package com.appsinnova.admin.business.vo.wx;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 客服 config：{"items":[{"wechat_id","qr_image_url"},...]}（JsonUtils SNAKE_CASE）。
 */
@Data
public class WxCustomerServiceConfig {

    private List<WxCustomerServiceItem> items = new ArrayList<>();
}
