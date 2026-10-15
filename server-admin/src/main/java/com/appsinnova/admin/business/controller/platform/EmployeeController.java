package com.appsinnova.admin.business.controller.platform;

import com.appsinnova.admin.business.common.enums.platform.EmployeePermCode;
import com.appsinnova.admin.business.common.enums.platform.EmployeeStatus;
import com.appsinnova.admin.business.domain.platform.Employee;
import com.appsinnova.admin.business.domain.platform.EmployeeTenant;
import com.appsinnova.admin.business.domain.platform.Tenant;
import com.appsinnova.admin.business.service.platform.EmployeeService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/business/platform/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/index")
    @RequiresPermissions("business:platform:employee:index")
    public String index(Model model, Employee queryParam) {
        Page<Employee> page = employeeService.getPageList(queryParam);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("tenants", employeeService.listAllTenants());
        return "/business/platform/employee/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:platform:employee:index")
    public String toEdit(@PathVariable(value = "id", required = false) Employee editItem, Model model) {
        boolean isNew = editItem == null;
        if (isNew) {
            editItem = new Employee();
            editItem.setStatus(EmployeeStatus.ENABLED.getCode());
            editItem.setRealName("");
            editItem.setMobile("");
        }
        model.addAttribute("editItem", editItem);
        model.addAttribute("isNew", isNew);
        model.addAttribute("tenants", employeeService.listAllTenants());
        model.addAttribute("permCodes", EmployeePermCode.values());
        model.addAttribute("permOptions", permOptions());
        model.addAttribute("managerPermCodes", EmployeePermCode.managerTemplateCodes());
        return "/business/platform/employee/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:platform:employee:edit")
    @ResponseBody
    public ResultVo<?> save(
            Employee saveItem,
            @RequestParam(value = "plainPassword", required = false) String plainPassword,
            @RequestParam(value = "bindTenantId", required = false) Long bindTenantId,
            @RequestParam(value = "bindStatus", required = false) Integer bindStatus,
            @RequestParam(value = "bindPermCodes", required = false) List<String> bindPermCodes) {
        boolean isNew = saveItem.getId() == null;
        if (!isNew) {
            Employee old = employeeService.getById(saveItem.getId());
            if (old == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
        } else {
            if (StringUtils.isBlank(saveItem.getUsername())) {
                return ResultVoUtil.error("登录名必填");
            }
            saveItem.setUsername(saveItem.getUsername().trim());
            if (saveItem.getUsername().length() > 64) {
                return ResultVoUtil.error("登录名最多64个字符");
            }
            if (employeeService.isUsernameTakenByOther(saveItem.getUsername(), null)) {
                return ResultVoUtil.error("登录名已存在");
            }
            if (StringUtils.isBlank(plainPassword)) {
                return ResultVoUtil.error("密码必填");
            }
            if (plainPassword.length() < 6 || plainPassword.length() > 32) {
                return ResultVoUtil.error("密码长度 6～32 位");
            }
            if (bindTenantId == null || bindTenantId <= 0) {
                return ResultVoUtil.error("新建须选择挂靠店铺");
            }
        }

        if (!isNew && StringUtils.isNotBlank(plainPassword)) {
            if (plainPassword.length() < 6 || plainPassword.length() > 32) {
                return ResultVoUtil.error("密码长度 6～32 位");
            }
        }

        if (StringUtils.isBlank(saveItem.getRealName())) {
            return ResultVoUtil.error("真实姓名必填");
        }
        saveItem.setRealName(saveItem.getRealName().trim());
        if (saveItem.getRealName().length() > 64) {
            return ResultVoUtil.error("真实姓名最多64个字符");
        }
        saveItem.setMobile(StringUtils.defaultString(saveItem.getMobile()).trim());
        if (saveItem.getMobile().length() > 32) {
            return ResultVoUtil.error("手机最多32个字符");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (!EmployeeStatus.ENABLED.getCode().equals(saveItem.getStatus())
                && !EmployeeStatus.DISABLED.getCode().equals(saveItem.getStatus())) {
            return ResultVoUtil.error("状态参数无效");
        }

        EmployeeService.ShopBindItem createBind = null;
        if (isNew) {
            createBind = new EmployeeService.ShopBindItem();
            createBind.setTenantId(bindTenantId);
            createBind.setStatus(bindStatus != null ? bindStatus : EmployeeStatus.ENABLED.getCode());
            createBind.setPermCodes(bindPermCodes);
        }

        User user = ShiroUtil.getSubject();
        try {
            employeeService.saveAccount(saveItem, plainPassword, createBind, user.getNickname());
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:platform:employee:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        if (!EmployeeStatus.ENABLED.getCode().equals(status)
                && !EmployeeStatus.DISABLED.getCode().equals(status)) {
            return ResultVoUtil.error("状态参数无效");
        }
        User user = ShiroUtil.getSubject();
        try {
            for (Long id : ids) {
                employeeService.updateStatus(id, status, user.getNickname());
            }
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
        return ResultVoUtil.success("操作成功");
    }

    @GetMapping("/shops/{id}")
    @RequiresPermissions("business:platform:employee:index")
    public String toShops(@PathVariable("id") Long id, Model model) {
        Employee emp = employeeService.getById(id);
        if (emp == null) {
            model.addAttribute("error", "员工不存在");
            return "/business/platform/employee/shops";
        }
        List<EmployeeTenant> binds = employeeService.listBindsWithPerms(id);
        List<Tenant> tenants = employeeService.listAllTenants();
        model.addAttribute("employee", emp);
        model.addAttribute("binds", binds);
        model.addAttribute("tenants", tenants);
        model.addAttribute("permOptions", permOptions());
        model.addAttribute("managerPermCodes", EmployeePermCode.managerTemplateCodes());
        return "/business/platform/employee/shops";
    }

    private List<Map<String, String>> permOptions() {
        List<Map<String, String>> list = new ArrayList<>();
        for (EmployeePermCode p : EmployeePermCode.values()) {
            Map<String, String> row = new HashMap<>();
            row.put("code", p.getCode());
            row.put("message", p.getMessage());
            list.add(row);
        }
        return list;
    }

    @PostMapping("/shops/save")
    @RequiresPermissions("business:platform:employee:edit")
    @ResponseBody
    public ResultVo<?> saveShops(@RequestBody ShopBindRequest req) {
        if (req == null || req.getEmployeeId() == null) {
            return ResultVoUtil.error("员工必选");
        }
        User user = ShiroUtil.getSubject();
        List<EmployeeService.ShopBindItem> items = null;
        if (req.getBinds() != null) {
            items = req.getBinds().stream().map(b -> {
                EmployeeService.ShopBindItem item = new EmployeeService.ShopBindItem();
                item.setTenantId(b.getTenantId());
                item.setStatus(b.getStatus());
                item.setPermCodes(b.getPermCodes());
                return item;
            }).collect(Collectors.toList());
        }
        try {
            employeeService.replaceBinds(req.getEmployeeId(), items, user.getNickname());
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @GetMapping("/quota")
    @RequiresPermissions("business:platform:employee:index")
    @ResponseBody
    public ResultVo<?> quota(@RequestParam("tenantId") Long tenantId) {
        Tenant t = employeeService.listAllTenants().stream()
                .filter(x -> x.getTenantId().equals(tenantId))
                .findFirst().orElse(null);
        if (t == null) {
            return ResultVoUtil.error("店铺不存在");
        }
        long occupied = employeeService.countOccupied(tenantId);
        return ResultVoUtil.success(Arrays.asList(occupied, t.getEmployeeQuota() == null ? 0 : t.getEmployeeQuota()));
    }

    @Data
    public static class ShopBindRequest {
        private Long employeeId;
        private List<ShopBindBody> binds;
    }

    @Data
    public static class ShopBindBody {
        private Long tenantId;
        private Integer status;
        private List<String> permCodes;
    }
}
