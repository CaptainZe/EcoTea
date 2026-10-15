package com.appsinnova.admin.business.controller.platform;

import com.appsinnova.admin.business.domain.platform.AdminTenantScope;
import com.appsinnova.admin.business.domain.platform.Tenant;
import com.appsinnova.admin.business.service.platform.AdminTenantScopeService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import com.appsinnova.admin.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/business/platform/adminScope")
@RequiredArgsConstructor
public class AdminTenantScopeController {

    private final AdminTenantScopeService adminTenantScopeService;
    private final UserService userService;

    @GetMapping("/index")
    @RequiresPermissions("business:platform:adminScope:index")
    public String index(Model model, User queryParam) {
        if (queryParam == null) {
            queryParam = new User();
        }
        Page<User> page = userService.getPageList(queryParam);
        List<User> users = page.getContent();
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<AdminTenantScope>> scopeMap = adminTenantScopeService.mapByUserIds(userIds);
        Map<Long, String> tenantNames = adminTenantScopeService.loadTenantNameMap();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (User user : users) {
            List<AdminTenantScope> scopes = scopeMap.getOrDefault(user.getId(), Collections.emptyList());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("user", user);
            row.put("platform", adminTenantScopeService.isPlatform(scopes));
            row.put("scopeSummary", adminTenantScopeService.buildSummary(scopes, tenantNames));
            row.put("scopeCount", scopes.size());
            rows.add(row);
        }

        model.addAttribute("list", rows);
        model.addAttribute("page", page);
        return "/business/platform/adminScope/index";
    }

    @GetMapping("/edit")
    @RequiresPermissions("business:platform:adminScope:index")
    public String toEdit(@RequestParam("userId") Long userId, Model model) {
        User user = userService.getById(userId);
        if (user == null) {
            model.addAttribute("error", "用户不存在");
            return "/business/platform/adminScope/edit";
        }
        List<AdminTenantScope> scopes = adminTenantScopeService.listByUserId(userId);
        boolean platform = adminTenantScopeService.isPlatform(scopes);
        Set<Long> selectedTenantIds = scopes.stream()
                .map(AdminTenantScope::getTenantId)
                .filter(tid -> tid != null && tid > 0)
                .collect(Collectors.toSet());
        List<Tenant> tenants = adminTenantScopeService.listSelectableTenants();

        model.addAttribute("user", user);
        model.addAttribute("platform", platform);
        model.addAttribute("selectedTenantIds", selectedTenantIds);
        model.addAttribute("tenants", tenants);
        return "/business/platform/adminScope/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:platform:adminScope:edit")
    @ResponseBody
    public ResultVo<?> save(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "scopeMode", required = false) String scopeMode,
            @RequestParam(value = "tenantIds", required = false) List<Long> tenantIds) {
        User user = userService.getById(userId);
        if (user == null) {
            return ResultVoUtil.error("用户不存在");
        }
        boolean platform = "platform".equalsIgnoreCase(StringUtils.trimWhitespace(scopeMode));
        User operator = ShiroUtil.getSubject();
        try {
            adminTenantScopeService.replaceScopes(
                    userId, platform, tenantIds, operator != null ? operator.getNickname() : "");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @PostMapping("/clear")
    @RequiresPermissions("business:platform:adminScope:edit")
    @ResponseBody
    public ResultVo<?> clear(@RequestParam("userId") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return ResultVoUtil.error("用户不存在");
        }
        adminTenantScopeService.clearScopes(userId);
        return ResultVoUtil.success("已清空数据范围");
    }
}
