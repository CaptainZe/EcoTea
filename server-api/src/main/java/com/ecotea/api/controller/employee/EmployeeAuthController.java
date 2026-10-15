package com.ecotea.api.controller.employee;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.req.employee.ChangePasswordReq;
import com.ecotea.api.req.employee.EmployeeLoginReq;
import com.ecotea.api.req.employee.SelectTenantReq;
import com.ecotea.api.req.employee.WxCodeReq;
import com.ecotea.api.service.employee.EmployeeAuthService;
import com.ecotea.api.service.employee.EmployeeSession;
import com.ecotea.api.service.employee.EmployeeSessionHolder;
import com.ecotea.api.vo.employee.EmployeeLoginVO;
import com.ecotea.api.vo.employee.EmployeeSessionVO;
import com.ecotea.api.vo.tenant.TenantOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序员工：登录 / 会话 / 选店。
 * <p>Header：{@code X-Employee-Token}（{@code /login}、{@code /wx-login} 除外）
 */
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeAuthController {

    private final EmployeeAuthService employeeAuthService;

    @PostMapping("/login")
    public ApiResult<EmployeeLoginVO> login(@Validated @RequestBody EmployeeLoginReq req) {
        return ApiResult.ok(employeeAuthService.login(req.getUsername(), req.getPassword()));
    }

    /** 微信一键登录（无 token）；未绑定须先账密登录后绑定 */
    @PostMapping("/wx-login")
    public ApiResult<EmployeeLoginVO> wxLogin(@Validated @RequestBody WxCodeReq req) {
        return ApiResult.ok(employeeAuthService.wxLogin(req.getCode()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        employeeAuthService.logout(EmployeeSessionHolder.getToken());
        return ApiResult.ok();
    }

    @GetMapping("/session")
    public ApiResult<EmployeeSessionVO> session() {
        return ApiResult.ok(employeeAuthService.sessionInfo(EmployeeSessionHolder.require()));
    }

    @GetMapping("/tenants")
    public ApiResult<List<TenantOptionVO>> tenants() {
        EmployeeSession session = EmployeeSessionHolder.require();
        return ApiResult.ok(employeeAuthService.listSelectableTenants(session.getEmployeeId()));
    }

    @PostMapping("/select-tenant")
    public ApiResult<EmployeeSessionVO> selectTenant(@Validated @RequestBody SelectTenantReq req) {
        EmployeeSession session = EmployeeSessionHolder.require();
        return ApiResult.ok(employeeAuthService.selectTenant(
                session, EmployeeSessionHolder.getToken(), req.getTenantId()));
    }

    /** 自助改密；成功后会话失效，客户端须重新登录 */
    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(@Validated @RequestBody ChangePasswordReq req) {
        EmployeeSession session = EmployeeSessionHolder.require();
        employeeAuthService.changePassword(
                session.getEmployeeId(), req.getOldPassword(), req.getNewPassword());
        return ApiResult.ok();
    }

    /**
     * 绑定当前微信到已登录员工（body：{@code code} 来自小程序 {@code wx.login}）。
     * <p>需 token；返回会话信息（含 {@code wxBound}）。
     */
    @PostMapping("/wx-bind")
    public ApiResult<EmployeeSessionVO> wxBind(@Validated @RequestBody WxCodeReq req) {
        EmployeeSession session = EmployeeSessionHolder.require();
        return ApiResult.ok(employeeAuthService.bindWx(session, req.getCode()));
    }

    /** 解绑微信；需 token */
    @PostMapping("/wx-unbind")
    public ApiResult<EmployeeSessionVO> wxUnbind() {
        EmployeeSession session = EmployeeSessionHolder.require();
        return ApiResult.ok(employeeAuthService.unbindWx(session));
    }
}
