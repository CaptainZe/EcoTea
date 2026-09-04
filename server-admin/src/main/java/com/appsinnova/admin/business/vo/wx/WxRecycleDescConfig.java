package com.appsinnova.admin.business.vo.wx;

import lombok.Data;

/**
 * 回收说明 config：{"title","body","cta_text","cta_url"}（JsonUtils SNAKE_CASE）。
 */
@Data
public class WxRecycleDescConfig {

    private String title;
    private String body;
    private String ctaText;
    private String ctaUrl;
}
