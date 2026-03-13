package com.appsinnova.admin.business.service.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 请求封装
 *
 * @author ice
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpService {
    private HttpComponentsClientHttpRequestFactory requestFactory;

    @PostConstruct
    private void init() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setMaxConnTotal(64) // 设置最大连接数
                .setMaxConnPerRoute(32) // 设置每个路由的最大连接数
                .setConnectionTimeToLive(30, TimeUnit.SECONDS) // 设置连接的存活时间
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(1000) // 设置连接超时时间
                        .setSocketTimeout(30000) // 设置读取超时时间
                        .build())
                .build();
        requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    public String httpGet(String url) {
        try {
            RestTemplate restTemplate = new RestTemplate(requestFactory);

            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("httpGet {} Exception: {}", url, e.getMessage());
        }
        return null;
    }

    public String httpPost(String url, String body) {
        try {
            RestTemplate restTemplate = new RestTemplate(requestFactory);

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("Content-Type","application/json;charset=UTF-8");
            HttpEntity<String> requestEntity = new HttpEntity<>(body, requestHeaders);
            return restTemplate.postForObject(url, requestEntity, String.class);
        } catch (Exception e) {
            log.error("httpPost {} Exception: {}", url, e.getMessage());
        }
        return null;
    }
}