package com.ecotea.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理端调用鉴权（Header X-EcoTea-Api-Key）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ecotea.security")
public class EcoTeaSecurityProperties {

    /**
     * 与 admin {@code ecotea.api.api-key} 一致；未配置则拒绝所有受保护写接口。
     */
    private String apiKey = "";
}
