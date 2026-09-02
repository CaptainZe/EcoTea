package com.ecotea.api.common.utils.chai;

import com.ecotea.api.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * chai 图片 URL 列表（库内 JSON 数组字符串）。
 */
public final class ChaiUrlListUtil {

    private ChaiUrlListUtil() {
    }

    public static List<String> parseUrlList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        List<String> list = JsonUtils.readValue(json, new TypeReference<List<String>>() {});
        return list != null ? list : new ArrayList<>();
    }

    public static String firstUrl(String json) {
        List<String> list = parseUrlList(json);
        for (String url : list) {
            if (StringUtils.hasText(url)) {
                return url.trim();
            }
        }
        return null;
    }
}
