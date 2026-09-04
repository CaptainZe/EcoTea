package com.ecotea.api.vo.wx;

import lombok.Data;

/**
 * 回收说明 config（type=2）：title / body / cta_text / cta_url。
 */
@Data
public class WxRecycleDescConfig {

    private String title;
    private String body;
    private String ctaText;
    private String ctaUrl;
}
