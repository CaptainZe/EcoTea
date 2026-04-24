package com.appsinnova.admin.business.controller.sys;

import com.appsinnova.admin.business.domain.sys.AppSecretKey;
import com.appsinnova.admin.business.service.sys.AppSecretKeyService;
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
@RequestMapping("/business/sys/appSecretKey")
@RequiredArgsConstructor
public class AppSecretKeyController {

    private final AppSecretKeyService appSecretKeyService;

    // 列表页面
    @GetMapping("/index")
    @RequiresPermissions("business:sys:appSecretKey:index")
    public String index(Model model, AppSecretKey queryParam) {
        Page<AppSecretKey> list = appSecretKeyService.getPageList(queryParam);

        // 封装数据
        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        return "/business/sys/appSecretKey/index";
    }

    // 跳转到编辑页面
    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:sys:appSecretKey:edit")
    public String toEdit(@PathVariable(value = "id", required = false) AppSecretKey editItem, Model model) {
        // 初始化（可设置默认值）
        if (editItem == null) {
            editItem = new AppSecretKey();
        }

        model.addAttribute("editItem", editItem);
        return "/business/sys/appSecretKey/edit";
    }

    // 跳转至拷贝页面
    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:sys:appSecretKey:edit")
    public String toCopy(@PathVariable(value = "id") AppSecretKey editItem, Model model) {
        editItem.setId(null);
        model.addAttribute("editItem", editItem);
        return "/business/sys/appSecretKey/edit";
    }

    // 保存
    @PostMapping("/save")
    @RequiresPermissions("business:sys:appSecretKey:edit")
    @ResponseBody
    public ResultVo<?> save(AppSecretKey saveItem) {
        if (saveItem.getId() != null) {
            AppSecretKey oldEntity = appSecretKeyService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }

            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        // 有效性校验
        if (saveItem.getType() == null) {
            return ResultVoUtil.error("类型必选");
        }
        if (StringUtils.isBlank(saveItem.getAccessSecret())) {
            return ResultVoUtil.error("密钥必填");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        appSecretKeyService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    // 删除
    @RequestMapping("/delete")
    @RequiresPermissions("business:sys:appSecretKey:edit")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }

        appSecretKeyService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }
}