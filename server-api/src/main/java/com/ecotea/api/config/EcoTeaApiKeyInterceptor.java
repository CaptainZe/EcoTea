package com.ecotea.api.config;

import com.ecotea.api.common.constant.ErrorCode;
import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.common.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 校验 Header {@code X-EcoTea-Api-Key}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcoTeaApiKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-EcoTea-Api-Key";

    private final EcoTeaSecurityProperties ecoTeaSecurityProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String configured = ecoTeaSecurityProperties.getApiKey();
        if (!StringUtils.hasText(configured)) {
            log.warn("ecotea.security.api-key not configured, reject {}", request.getRequestURI());
            writeUnauthorized(response, "服务未配置 Api-Key");
            return false;
        }
        String provided = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(provided) || !constantTimeEquals(configured.trim(), provided.trim())) {
            log.warn("api key check failed, uri={}", request.getRequestURI());
            writeUnauthorized(response, "Api-Key 无效或缺失");
            return false;
        }
        return true;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(x, y);
    }

    private static void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.writeValueAsString(
                ApiResult.fail(ErrorCode.UNAUTHORIZED, message)));
    }
}
