package com.appsinnova.admin.business.service.wx;

import com.appsinnova.admin.business.common.config.EcoTeaApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务端调用 api 发布微信菜单（Header X-EcoTea-Api-Key）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMpMenuPublishClient {

    private static final String HEADER_API_KEY = "X-EcoTea-Api-Key";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final EcoTeaApiProperties ecoTeaApiProperties;

    /**
     * @return 失败时返回错误文案；成功返回 null
     */
    public String publishById(Long id) {
        if (id == null) {
            return "菜单 id 不能为空";
        }
        String base = StringUtils.trimToEmpty(ecoTeaApiProperties.getBaseUrl());
        if (StringUtils.isBlank(base)) {
            return "未配置 ecotea.api.base-url";
        }
        if (StringUtils.isBlank(ecoTeaApiProperties.getApiKey())) {
            return "未配置 ecotea.api.api-key";
        }
        String url = StringUtils.removeEnd(base, "/") + "/wx/mp/menu/publish";

        Map<String, Object> body = new HashMap<>();
        body.put("id", id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.set(HEADER_API_KEY, ecoTeaApiProperties.getApiKey().trim());

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            return parseApiResult(resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("publish menu http error, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            String parsed = parseApiResult(e.getResponseBodyAsString());
            if (parsed != null) {
                return parsed;
            }
            return "发布失败：HTTP " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("publish menu failed, id={}", id, e);
            return "发布失败：" + e.getMessage();
        }
    }

    private static String parseApiResult(String json) {
        if (StringUtils.isBlank(json)) {
            return "api 无响应";
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            int code = node.path("code").asInt(-1);
            if (code == 0) {
                return null;
            }
            String message = node.path("message").asText(null);
            if (StringUtils.isBlank(message)) {
                message = node.path("msg").asText("发布失败");
            }
            return message;
        } catch (Exception e) {
            return "api 响应无法解析";
        }
    }
}
