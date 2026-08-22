package com.appsinnova.admin.business.common.utils;

/**
 * 请求参数解析：空或非法统一返回 null（表示查询不限）。
 */
public final class RequestParamUtil {

    private RequestParamUtil() {
    }

    public static Long parseLong(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(param.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer parseInteger(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
