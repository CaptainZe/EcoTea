package com.ecotea.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对外站点地址（H5 / 被动回复链接）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ecotea.site")
public class EcoTeaSiteProperties {

    /**
     * 对外 HTTPS 根地址，无尾斜杠。
     * 默认生产域名；本地可在 env 中覆盖。
     */
    private String publicBaseUrl = "https://api.ecotea.cn";
}
