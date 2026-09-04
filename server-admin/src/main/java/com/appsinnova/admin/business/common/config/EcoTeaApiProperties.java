package com.appsinnova.admin.business.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 调用 server-api 的基址与鉴权（发布菜单等）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ecotea.api")
public class EcoTeaApiProperties {

    /** 如 http://127.0.0.1:8081 或 https://api.ecotea.cn，无尾斜杠 */
    private String baseUrl = "http://127.0.0.1:8081";

    /** 与 api {@code ecotea.security.api-key} 一致 */
    private String apiKey = "";
}
