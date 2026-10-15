package com.ecotea.api.service.employee;

/**
 * 当前请求员工会话（由拦截器写入）。
 */
public final class EmployeeSessionHolder {

    public static final String REQUEST_ATTR = "ECOTEA_EMPLOYEE_SESSION";
    public static final String REQUEST_TOKEN_ATTR = "ECOTEA_EMPLOYEE_TOKEN";

    private static final ThreadLocal<EmployeeSession> LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_LOCAL = new ThreadLocal<>();

    private EmployeeSessionHolder() {
    }

    public static void set(EmployeeSession session, String token) {
        LOCAL.set(session);
        TOKEN_LOCAL.set(token);
    }

    public static EmployeeSession get() {
        return LOCAL.get();
    }

    public static String getToken() {
        return TOKEN_LOCAL.get();
    }

    public static void clear() {
        LOCAL.remove();
        TOKEN_LOCAL.remove();
    }

    public static EmployeeSession require() {
        EmployeeSession session = get();
        if (session == null) {
            throw new IllegalStateException("employee session missing");
        }
        return session;
    }
}
