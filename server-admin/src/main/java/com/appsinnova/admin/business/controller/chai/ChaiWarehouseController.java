package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.pca.PcaCodeService;
import com.appsinnova.admin.business.domain.chai.ChaiWarehouse;
import com.appsinnova.admin.business.service.chai.ChaiWarehouseService;
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
@RequestMapping("/business/chai/warehouse")
@RequiredArgsConstructor
public class ChaiWarehouseController {

    private final ChaiWarehouseService chaiWarehouseService;
    private final PcaCodeService pcaCodeService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:warehouse:index")
    public String index(Model model, ChaiWarehouse queryParam) {
        Page<ChaiWarehouse> page = chaiWarehouseService.getPageList(queryParam);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        return "/business/chai/warehouse/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:chai:warehouse:index")
    public String toEdit(@PathVariable(value = "id", required = false) ChaiWarehouse editItem, Model model) {
        if (editItem == null) {
            editItem = new ChaiWarehouse();
            editItem.setStatus(ChaiStatus.ONLINE.getCode());
            editItem.setOrderNum(0);
            editItem.setProvince("");
            editItem.setCity("");
            editItem.setDistrict("");
            editItem.setAddress("");
        } else {
            chaiWarehouseService.fillRegionNames(editItem);
        }
        model.addAttribute("editItem", editItem);
        return "/business/chai/warehouse/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:chai:warehouse:edit")
    @ResponseBody
    public ResultVo<?> save(ChaiWarehouse saveItem) {
        if (saveItem.getId() != null) {
            ChaiWarehouse oldEntity = chaiWarehouseService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (StringUtils.isBlank(saveItem.getName())) {
            return ResultVoUtil.error("仓库名称必填");
        }
        saveItem.setName(saveItem.getName().trim());
        if (saveItem.getName().length() > 255) {
            return ResultVoUtil.error("仓库名称最多255个字符");
        }
        if (chaiWarehouseService.isNameTakenByOther(saveItem.getName(), saveItem.getId())) {
            return ResultVoUtil.error("仓库名称已存在，请更换后重试");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (saveItem.getOrderNum() == null) {
            saveItem.setOrderNum(0);
        }

        String province = StringUtils.defaultString(saveItem.getProvince()).trim();
        String city = StringUtils.defaultString(saveItem.getCity()).trim();
        String district = StringUtils.defaultString(saveItem.getDistrict()).trim();
        boolean anyRegion = StringUtils.isNotBlank(province)
                || StringUtils.isNotBlank(city)
                || StringUtils.isNotBlank(district);
        if (anyRegion) {
            String regionError = pcaCodeService.validateRegion(province, city, district);
            if (regionError != null) {
                return ResultVoUtil.error(regionError);
            }
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

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        chaiWarehouseService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:chai:warehouse:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        try {
            List<String> blocked = chaiWarehouseService.deleteByIdIn(ids);
            if (blocked.isEmpty()) {
                return ResultVoUtil.success("删除成功");
            }
            return ResultVoUtil.success("以下已被库存引用，未删除：" + String.join("、", blocked));
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:chai:warehouse:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            ChaiWarehouse entity = chaiWarehouseService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                chaiWarehouseService.save(entity);
            }
        }
        return ResultVoUtil.success("操作成功");
    }
}
