package com.appsinnova.admin.business.controller.platform;

import com.appsinnova.admin.business.common.enums.platform.TenantStatus;
import com.appsinnova.admin.business.common.pca.PcaCodeService;
import com.appsinnova.admin.business.domain.platform.Tenant;
import com.appsinnova.admin.business.service.platform.TenantService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/business/platform/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final PcaCodeService pcaCodeService;

    @GetMapping("/index")
    @RequiresPermissions("business:platform:tenant:index")
    public String index(Model model, Tenant queryParam) {
        Page<Tenant> page = tenantService.getPageList(queryParam);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        return "/business/platform/tenant/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:platform:tenant:index")
    public String toEdit(@PathVariable(value = "id", required = false) Tenant editItem, Model model) {
        if (editItem == null) {
            editItem = new Tenant();
            editItem.setStatus(TenantStatus.ENABLED.getCode());
            editItem.setEmployeeQuota(0);
            editItem.setProvince("");
            editItem.setCity("");
            editItem.setDistrict("");
            editItem.setAddress("");
            editItem.setContactName("");
            editItem.setContactMobile("");
            editItem.setRemark("");
        } else {
            tenantService.fillRegionNames(editItem);
        }
        model.addAttribute("editItem", editItem);
        return "/business/platform/tenant/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:platform:tenant:edit")
    @ResponseBody
    public ResultVo<?> save(Tenant saveItem) {
        boolean isNew = saveItem.getId() == null;
        if (!isNew) {
            Tenant oldEntity = tenantService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            // 业务 tenant_id 创建后不可改
            saveItem.setTenantId(oldEntity.getTenantId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        } else {
            if (saveItem.getTenantId() == null || saveItem.getTenantId() <= 0) {
                return ResultVoUtil.error("业务租户ID必填且须大于0");
            }
            if (tenantService.isTenantIdTakenByOther(saveItem.getTenantId(), null)) {
                return ResultVoUtil.error("业务租户ID已存在，请更换后重试");
            }
        }

        if (StringUtils.isBlank(saveItem.getName())) {
            return ResultVoUtil.error("店铺名称必填");
        }
        saveItem.setName(saveItem.getName().trim());
        if (saveItem.getName().length() > 255) {
            return ResultVoUtil.error("店铺名称最多255个字符");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (saveItem.getEmployeeQuota() == null) {
            saveItem.setEmployeeQuota(0);
        }
        if (saveItem.getEmployeeQuota() < 0) {
            return ResultVoUtil.error("员工名额不能为负数");
        }

        String province = StringUtils.defaultString(saveItem.getProvince()).trim();
        String city = StringUtils.defaultString(saveItem.getCity()).trim();
        String district = StringUtils.defaultString(saveItem.getDistrict()).trim();
        String regionError = pcaCodeService.validateProvinceCity(province, city, district);
        if (regionError != null) {
            return ResultVoUtil.error(regionError);
        }
        saveItem.setProvince(province);
        saveItem.setCity(city);
        saveItem.setDistrict(district);

        if (StringUtils.isBlank(saveItem.getAddress())) {
            saveItem.setAddress("");
        } else {
            saveItem.setAddress(saveItem.getAddress().trim());
            if (saveItem.getAddress().length() > 500) {
                return ResultVoUtil.error("详细地址最多500个字符");
            }
        }

        saveItem.setContactName(StringUtils.defaultString(saveItem.getContactName()).trim());
        if (saveItem.getContactName().length() > 64) {
            return ResultVoUtil.error("负责人姓名最多64个字符");
        }
        saveItem.setContactMobile(StringUtils.defaultString(saveItem.getContactMobile()).trim());
        if (saveItem.getContactMobile().length() > 32) {
            return ResultVoUtil.error("负责人手机最多32个字符");
        }
        saveItem.setRemark(StringUtils.defaultString(saveItem.getRemark()).trim());
        if (saveItem.getRemark().length() > 500) {
            return ResultVoUtil.error("备注最多500个字符");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        tenantService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:platform:tenant:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        if (!TenantStatus.ENABLED.getCode().equals(status)
                && !TenantStatus.DISABLED.getCode().equals(status)) {
            return ResultVoUtil.error("状态参数无效");
        }
        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            Tenant entity = tenantService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                tenantService.save(entity);
            }
        }
        return ResultVoUtil.success("操作成功");
    }
}
