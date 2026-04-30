package com.appsinnova.admin.business.service.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeiShuWebhookService {

    private final RestTemplate restTemplate;

    /**
     * 发送飞书群机器人文本消息
     *
     * @param webhookUrl 飞书群机器人 Webhook 地址
     * @param content    发送内容（纯文本）
     */
    public void sendText(String webhookUrl, String content) {

        // 1. 构造请求体
        Map<String, Object> body = new HashMap<>();
        body.put("msg_type", "text");

        Map<String, String> text = new HashMap<>();
        text.put("text", content);
        body.put("content", text);

        // 2. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                new MediaType("application", "json", StandardCharsets.UTF_8)
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        // 3. 发送请求
        restTemplate.postForEntity(webhookUrl, request, String.class);
    }

    /**
     * 发送飞书卡片消息（Markdown）
     *
     * @param webhookUrl     飞书群机器人 Webhook
     * @param title          卡片标题
     * @param markdownContent Markdown 内容（飞书 Markdown）
     */
    public void sendMarkdownCard(String webhookUrl,
                                 String title,
                                 String markdownContent) {

        // ---------- card.header ----------
        Map<String, Object> header = new HashMap<>();
        header.put("template", "blue"); // blue / green / orange / red / purple

        Map<String, Object> titleMap = new HashMap<>();
        titleMap.put("tag", "plain_text");
        titleMap.put("content", title);
        header.put("title", titleMap);

        // ---------- card.elements ----------
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("tag", "markdown");
        markdown.put("content", markdownContent);

        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(markdown);

        // ---------- card ----------
        Map<String, Object> card = new HashMap<>();
        card.put("header", header);
        card.put("elements", elements);

        // ---------- request body ----------
        Map<String, Object> body = new HashMap<>();
        body.put("msg_type", "interactive");
        body.put("card", card);

        // ---------- headers ----------
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                new MediaType("application", "json", StandardCharsets.UTF_8)
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        // ---------- send ----------
        restTemplate.postForEntity(webhookUrl, request, String.class);
    }

    /**
     * 发送飞书卡片消息（Markdown + 可选多图）
     *
     * @param webhookUrl 飞书群机器人 Webhook
     * @param title 卡片标题
     * @param markdownContent Markdown 内容
     * @param imageUrls 图片 imageUrls（可为空、可多张）
     */
    public void sendMarkdownCardWithImages(String webhookUrl,
                                           String title,
                                           String markdownContent,
                                           List<String> imageUrls) {

        try {
            // ---------- card.header ----------
            Map<String, Object> header = new HashMap<>();
            Map<String, Object> titleMap = new HashMap<>();
            titleMap.put("tag", "plain_text");
            titleMap.put("content", title);
            header.put("title", titleMap);

            // ---------- card.elements ----------
            List<Map<String, Object>> elements = new ArrayList<>();

            // 1️⃣ 如果有图片
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (String imageUrl : imageUrls) {
                    Map<String, Object> img = new HashMap<>();
                    img.put("tag", "img");
                    img.put("img_url", "![图片]("+imageUrl+")");
                    elements.add(img);
                }
            }

            // 2️⃣ Markdown 内容
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("tag", "markdown");
            markdown.put("content", markdownContent);
            elements.add(markdown);

            // ---------- card ----------
            Map<String, Object> card = new HashMap<>();
            card.put("header", header);
            card.put("elements", elements);

            // ---------- request body ----------
            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "interactive");
            body.put("card", card);

            // ---------- headers ----------
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    new MediaType("application", "json", StandardCharsets.UTF_8)
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            // ---------- send ----------
            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.info("发送飞书卡片消息成功");
        } catch (Exception e) {
            log.error("发送飞书卡片消息失败", e);
        }
    }
}