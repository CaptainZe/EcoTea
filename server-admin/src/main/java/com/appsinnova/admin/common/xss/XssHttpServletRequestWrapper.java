package com.appsinnova.admin.common.xss;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Xss防护过滤处理。
 * Jsoup.clean 会折叠纯文本换行，清洗前用占位符保留，清洗后再还原。
 *
 * @author ccc
 * @date 2018/12/9
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /** 不可能出现在用户正文中的换行占位（避免与业务文案冲突） */
    private static final String NL_PLACEHOLDER = "\uE000\uE001NL\uE001\uE000";

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value == null ? null : cleanPreservingNewlines(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        Whitelist whitelist = Whitelist.relaxed();

        String[] params = super.getParameterValues(name);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                params[i] = cleanPreservingNewlines(params[i], whitelist);
            }
        }
        return params;
    }

    private static String cleanPreservingNewlines(String raw) {
        return cleanPreservingNewlines(raw, Whitelist.relaxed());
    }

    private static String cleanPreservingNewlines(String raw, Whitelist whitelist) {
        if (raw == null) {
            return null;
        }
        // 统一为 \n，再换成占位，避免 Jsoup 把换行折叠成空格
        String withPlaceholder = raw
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", NL_PLACEHOLDER);
        String cleaned = Jsoup.clean(withPlaceholder, whitelist).trim();
        return cleaned.replace(NL_PLACEHOLDER, "\n");
    }
}
