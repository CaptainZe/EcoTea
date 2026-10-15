package com.ecotea.api.service.employee;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecotea.api.common.constant.ErrorCode;
import com.ecotea.api.common.enums.employee.EmployeeStatus;
import com.ecotea.api.common.enums.tenant.TenantStatus;
import com.ecotea.api.common.exception.BizException;
import com.ecotea.api.common.utils.EmployeePasswordUtil;
import com.ecotea.api.domain.employee.Employee;
import com.ecotea.api.domain.employee.EmployeeTenant;
import com.ecotea.api.domain.employee.EmployeeTenantPerm;
import com.ecotea.api.domain.tenant.Tenant;
import com.ecotea.api.mapper.employee.EmployeeMapper;
import com.ecotea.api.mapper.employee.EmployeeTenantMapper;
import com.ecotea.api.mapper.employee.EmployeeTenantPermMapper;
import com.ecotea.api.service.tenant.TenantService;
import com.ecotea.api.service.wx.WxMaCodeSession;
import com.ecotea.api.service.wx.WxMaCodeSessionService;
import com.ecotea.api.vo.employee.EmployeeLoginVO;
import com.ecotea.api.vo.employee.EmployeeSessionVO;
import com.ecotea.api.vo.tenant.TenantOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeAuthService {

    private final EmployeeMapper employeeMapper;
    private final EmployeeTenantMapper employeeTenantMapper;
    private final EmployeeTenantPermMapper employeeTenantPermMapper;
    private final TenantService tenantService;
    private final EmployeeSessionService employeeSessionService;
    private final WxMaCodeSessionService wxMaCodeSessionService;

    public EmployeeLoginVO login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "登录名或密码不能为空");
        }
        Employee emp = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getUsername, username.trim())
                .last("limit 1"));
        if (emp == null || !EmployeePasswordUtil.matches(password, emp.getSalt(), emp.getPassword())) {
            throw new BizException("登录名或密码错误");
        }
        if (!EmployeeStatus.isEnabled(emp.getStatus())) {
            throw new BizException("账号已停用");
        }
        return issueLogin(emp);
    }

    /**
     * 微信一键登录：code → openid 查已绑定员工，发会话（须再选店）。
     * <p>未绑定则提示先账密登录后在「我的」绑定。
     */
    public EmployeeLoginVO wxLogin(String code) {
        WxMaCodeSession wx = wxMaCodeSessionService.exchangeCode(code);
        Employee emp = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getWxOpenid, wx.getOpenid())
                .last("limit 1"));
        if (emp == null) {
            throw new BizException("该微信尚未绑定员工，请先用账号密码登录后在「我的」中绑定");
        }
        if (!EmployeeStatus.isEnabled(emp.getStatus())) {
            throw new BizException("账号已停用");
        }
        // 可选回写 unionid（微信侧后续才给到时）
        if (StringUtils.hasText(wx.getUnionid()) && !wx.getUnionid().equals(emp.getWxUnionid())) {
            emp.setWxUnionid(wx.getUnionid());
            emp.setUpdateTime(System.currentTimeMillis());
            employeeMapper.updateById(emp);
        }
        return issueLogin(emp);
    }

    private EmployeeLoginVO issueLogin(Employee emp) {
        List<TenantOptionVO> tenants = listSelectableTenants(emp.getId());
        if (tenants.isEmpty()) {
            throw new BizException("未挂靠可用店铺，请联系平台");
        }
        EmployeeSession session = new EmployeeSession();
        session.setEmployeeId(emp.getId());
        session.setUsername(emp.getUsername());
        session.setRealName(emp.getRealName());
        session.setTenantId(null);
        session.setTenantName(null);
        session.setPermCodes(new ArrayList<>());
        String token = employeeSessionService.create(session);

        EmployeeLoginVO vo = new EmployeeLoginVO();
        vo.setToken(token);
        vo.setEmployeeId(emp.getId());
        vo.setUsername(emp.getUsername());
        vo.setRealName(emp.getRealName());
        vo.setTenantSelected(false);
        vo.setTenants(tenants);
        return vo;
    }

    public void logout(String token) {
        employeeSessionService.remove(token);
    }

    /**
     * 员工自助改密：校验原密码后换 salt 写新密，并踢掉全部会话（须重新登录）。
     */
    public void changePassword(Long employeeId, String oldPassword, String newPassword) {
        if (employeeId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "原密码与新密码均必填");
        }
        String newPlain = newPassword.trim();
        if (newPlain.length() < 6 || newPlain.length() > 32) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新密码长度须为 6～32 位");
        }
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null || !EmployeeStatus.isEnabled(emp.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不可用，请重新登录");
        }
        if (!EmployeePasswordUtil.matches(oldPassword, emp.getSalt(), emp.getPassword())) {
            throw new BizException("原密码错误");
        }
        if (oldPassword.equals(newPlain)
                || EmployeePasswordUtil.matches(newPlain, emp.getSalt(), emp.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        String salt = EmployeePasswordUtil.randomSalt();
        emp.setSalt(salt);
        emp.setPassword(EmployeePasswordUtil.encrypt(newPlain, salt));
        emp.setUpdateTime(System.currentTimeMillis());
        emp.setOperator(emp.getUsername());
        employeeMapper.updateById(emp);
        employeeSessionService.invalidateByEmployeeId(employeeId);
    }

    /**
     * 已登录员工绑定当前微信（小程序 openid）。
     * <p>不作自助注册；openid 已被其他员工占用则失败。
     */
    public EmployeeSessionVO bindWx(EmployeeSession session, String code) {
        if (session == null || session.getEmployeeId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Employee emp = employeeMapper.selectById(session.getEmployeeId());
        if (emp == null || !EmployeeStatus.isEnabled(emp.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不可用，请重新登录");
        }
        WxMaCodeSession wx = wxMaCodeSessionService.exchangeCode(code);
        String openid = wx.getOpenid();

        Employee other = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getWxOpenid, openid)
                .last("limit 1"));
        if (other != null && !other.getId().equals(emp.getId())) {
            throw new BizException("该微信已绑定其他员工账号");
        }
        if (StringUtils.hasText(emp.getWxOpenid()) && !openid.equals(emp.getWxOpenid())) {
            throw new BizException("当前账号已绑定其他微信，请先解绑后再绑定");
        }
        // 已绑同一 openid：幂等成功
        if (!openid.equals(emp.getWxOpenid())) {
            emp.setWxOpenid(openid);
            if (StringUtils.hasText(wx.getUnionid())) {
                emp.setWxUnionid(wx.getUnionid());
            }
            emp.setWxBindTime(System.currentTimeMillis());
            emp.setUpdateTime(System.currentTimeMillis());
            emp.setOperator(emp.getUsername());
            try {
                employeeMapper.updateById(emp);
            } catch (DuplicateKeyException e) {
                throw new BizException("该微信已绑定其他员工账号");
            }
        }
        return sessionInfo(session);
    }

    /** 解绑当前员工微信；解绑后不可再一键登录，直至重新绑定 */
    public EmployeeSessionVO unbindWx(EmployeeSession session) {
        if (session == null || session.getEmployeeId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Employee emp = employeeMapper.selectById(session.getEmployeeId());
        if (emp == null || !EmployeeStatus.isEnabled(emp.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不可用，请重新登录");
        }
        if (!StringUtils.hasText(emp.getWxOpenid())) {
            return sessionInfo(session);
        }
        long now = System.currentTimeMillis();
        employeeMapper.update(null, new LambdaUpdateWrapper<Employee>()
                .eq(Employee::getId, emp.getId())
                .set(Employee::getWxOpenid, null)
                .set(Employee::getWxUnionid, null)
                .set(Employee::getWxBindTime, 0L)
                .set(Employee::getUpdateTime, now)
                .set(Employee::getOperator, emp.getUsername()));
        return sessionInfo(session);
    }

    public EmployeeSessionVO sessionInfo(EmployeeSession session) {
        EmployeeSessionVO vo = new EmployeeSessionVO();
        vo.setEmployeeId(session.getEmployeeId());
        vo.setUsername(session.getUsername());
        vo.setRealName(session.getRealName());
        boolean selected = session.getTenantId() != null && session.getTenantId() > 0;
        vo.setTenantSelected(selected);
        vo.setTenantId(session.getTenantId());
        vo.setTenantName(session.getTenantName());
        vo.setPermCodes(session.getPermCodes() == null ? new ArrayList<>() : session.getPermCodes());
        if (session.getEmployeeId() != null) {
            Employee emp = employeeMapper.selectById(session.getEmployeeId());
            vo.setWxBound(emp != null && StringUtils.hasText(emp.getWxOpenid()));
        }
        return vo;
    }

    public List<TenantOptionVO> listSelectableTenants(Long employeeId) {
        List<EmployeeTenant> binds = employeeTenantMapper.selectList(new LambdaQueryWrapper<EmployeeTenant>()
                .eq(EmployeeTenant::getEmployeeId, employeeId)
                .eq(EmployeeTenant::getStatus, EmployeeStatus.ENABLED.getCode())
                .orderByAsc(EmployeeTenant::getTenantId));
        if (binds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tenantIds = binds.stream().map(EmployeeTenant::getTenantId).collect(Collectors.toList());
        Map<Long, Tenant> tenantMap = tenantService.mapByBizTenantIds(tenantIds);
        List<TenantOptionVO> result = new ArrayList<>();
        for (EmployeeTenant bind : binds) {
            Tenant tenant = tenantMap.get(bind.getTenantId());
            if (tenant == null || !TenantStatus.isEnabled(tenant.getStatus())) {
                continue;
            }
            TenantOptionVO opt = new TenantOptionVO();
            opt.setTenantId(tenant.getTenantId());
            opt.setName(tenant.getName());
            opt.setStatus(tenant.getStatus());
            result.add(opt);
        }
        return result;
    }

    public EmployeeSessionVO selectTenant(EmployeeSession session, String token, Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "店铺必选");
        }
        Employee emp = employeeMapper.selectById(session.getEmployeeId());
        if (emp == null || !EmployeeStatus.isEnabled(emp.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不可用，请重新登录");
        }

        EmployeeTenant bind = employeeTenantMapper.selectOne(new LambdaQueryWrapper<EmployeeTenant>()
                .eq(EmployeeTenant::getEmployeeId, session.getEmployeeId())
                .eq(EmployeeTenant::getTenantId, tenantId)
                .last("limit 1"));
        if (bind == null || !EmployeeStatus.isEnabled(bind.getStatus())) {
            throw new BizException("无权进入该店铺");
        }
        Tenant tenant = tenantService.getByBizTenantId(tenantId);
        if (tenant == null || !TenantStatus.isEnabled(tenant.getStatus())) {
            throw new BizException("店铺不可用");
        }

        List<String> perms = employeeTenantPermMapper.selectList(new LambdaQueryWrapper<EmployeeTenantPerm>()
                        .eq(EmployeeTenantPerm::getEmployeeId, session.getEmployeeId())
                        .eq(EmployeeTenantPerm::getTenantId, tenantId))
                .stream()
                .map(EmployeeTenantPerm::getPermCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        session.setTenantId(tenant.getTenantId());
        session.setTenantName(tenant.getName());
        session.setPermCodes(perms);
        // 刷新姓名（档案可能变更）
        session.setRealName(emp.getRealName());
        session.setUsername(emp.getUsername());
        employeeSessionService.save(token, session);
        return sessionInfo(session);
    }
}
