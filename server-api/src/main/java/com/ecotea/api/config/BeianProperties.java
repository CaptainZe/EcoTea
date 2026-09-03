package com.ecotea.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网站备案号（ICP / 公网安备），供 H5 页脚展示。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ecotea.beian")
public class BeianProperties {

    /** 如：京ICP备xxxxxxxx号；空则不展示 */
    private String icpText = "";

    private String icpUrl = "https://beian.miit.gov.cn";

    /** 如：京公网安备11010102000001号；空则不展示 */
    private String mpsText = "";

    /** 公安备案查询链接（含 code）；mpsText 为空时可空 */
    private String mpsUrl = "";
}
