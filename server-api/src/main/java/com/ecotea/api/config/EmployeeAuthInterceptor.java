package com.ecotea.api.config;

import com.ecotea.api.common.constant.ErrorCode;
import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.service.employee.EmployeeSession;
import com.ecotea.api.service.employee.EmployeeSessionHolder;
import com.ecotea.api.service.employee.EmployeeSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 校验 Header {@code X-Employee-Token}，写入 {@link EmployeeSessionHolder}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Employee-Token";

    private final EmployeeSessionService employeeSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "未登录或 token 缺失");
            return false;
        }
        EmployeeSession session = employeeSessionService.getByToken(token.trim());
        if (session == null || session.getEmployeeId() == null) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }
        employeeSessionService.touch(token.trim(), session);
        EmployeeSessionHolder.set(session, token.trim());
        request.setAttribute(EmployeeSessionHolder.REQUEST_ATTR, session);
        request.setAttribute(EmployeeSessionHolder.REQUEST_TOKEN_ATTR, token.trim());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        EmployeeSessionHolder.clear();
    }

    private static void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.writeValueAsString(
                ApiResult.fail(ErrorCode.UNAUTHORIZED, message)));
    }
}
