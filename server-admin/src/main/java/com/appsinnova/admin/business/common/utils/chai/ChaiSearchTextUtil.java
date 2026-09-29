package com.appsinnova.admin.business.common.utils.chai;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 商品搜索：关键词规范化与 search_text 拼接。
 * <p>SKU 关键词继承 SPU，与品牌/茶类一致。</p>
 */
public final class ChaiSearchTextUtil {

    /** 关键词最多条数（chip） */
    public static final int MAX_KEYWORD_COUNT = 20;
    /** 单条关键词最大字符数 */
    public static final int MAX_KEYWORD_LENGTH = 20;
    /** keywords 字段最大长度 */
    public static final int MAX_KEYWORDS_FIELD_LENGTH = 512;
    /** search_text 字段最大长度 */
    public static final int MAX_SEARCH_TEXT_LENGTH = 1024;

    private ChaiSearchTextUtil() {
    }

    /**
     * 规范化关键词：兼容空格 / 英文逗号 / 中文逗号；去空、去重（保序）；截断条数与总长。
     */
    public static String normalizeKeywords(String raw) {
        List<String> tokens = splitKeywords(raw);
        if (tokens.isEmpty()) {
            return "";
        }
        String joined = String.join(" ", tokens);
        if (joined.length() > MAX_KEYWORDS_FIELD_LENGTH) {
            joined = joined.substring(0, MAX_KEYWORDS_FIELD_LENGTH).trim();
        }
        return joined;
    }

    /**
     * 拆成关键词列表（已规范化、去重、限条数/单长）。
     */
    public static List<String> splitKeywords(String raw) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return result;
        }
        String normalized = raw.trim()
                .replace('，', ' ')
                .replace(',', ' ');
        Set<String> seen = new LinkedHashSet<>();
        for (String part : normalized.split("\\s+")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String token = part.trim();
            if (token.length() > MAX_KEYWORD_LENGTH) {
                token = token.substring(0, MAX_KEYWORD_LENGTH);
            }
            String key = token.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            result.add(token);
            if (result.size() >= MAX_KEYWORD_COUNT) {
                break;
            }
        }
        return result;
    }

    /**
     * 拼接检索文本：品牌名 + 商品名 + 关键词（空段丢掉，空格分隔）。
     */
    public static String buildSearchText(String brandName, String productName, String keywords) {
        List<String> parts = new ArrayList<>(3);
        appendPart(parts, brandName);
        appendPart(parts, productName);
        String kw = normalizeKeywords(keywords);
        if (StringUtils.hasText(kw)) {
            parts.add(kw);
        }
        String text = String.join(" ", parts);
        if (text.length() > MAX_SEARCH_TEXT_LENGTH) {
            text = text.substring(0, MAX_SEARCH_TEXT_LENGTH).trim();
        }
        return text;
    }

    private static void appendPart(List<String> parts, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String t = value.trim();
        if (!t.isEmpty()) {
            parts.add(t);
        }
    }
}
