package com.appsinnova.admin.business.common.utils.wx;

/**
 * 默认自定义菜单 JSON（三个 view，与 PRD §4 一致）。
 */
public final class WxMpMenuDefaultConfig {

    private WxMpMenuDefaultConfig() {
    }

    public static String defaultMenuJson() {
        return "{\n"
                + "  \"button\": [\n"
                + "    {\n"
                + "      \"type\": \"view\",\n"
                + "      \"name\": \"在售价目\",\n"
                + "      \"url\": \"https://api.ecotea.cn/h5/chai/sale.html\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"type\": \"view\",\n"
                + "      \"name\": \"茶叶回收\",\n"
                + "      \"url\": \"https://api.ecotea.cn/h5/chai/recycle.html\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"type\": \"view\",\n"
                + "      \"name\": \"联系我们\",\n"
                + "      \"url\": \"https://api.ecotea.cn/h5/about.html\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
    }
}
