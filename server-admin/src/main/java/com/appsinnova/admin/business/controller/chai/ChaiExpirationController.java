package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.service.chai.ChaiExpirationService;
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
@RequestMapping("/business/chai/expiration")
@RequiredArgsConstructor
public class ChaiExpirationController {

    private final ChaiExpirationService chaiExpirationService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:expiration:index")
    public String index(Model model, ChaiExpiration queryParam) {
        Page<ChaiExpiration> page = chaiExpirationService.getPageList(queryParam);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        return "/business/chai/expiration/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:chai:expiration:index")
    public String toEdit(@PathVariable(value = "id", required = false) ChaiExpiration editItem, Model model) {
        if (editItem == null) {
            editItem = new ChaiExpiration();
            editItem.setStatus(ChaiStatus.ONLINE.getCode());
            editItem.setOrderNum(0);
            editItem.setMonths(36);
        }
        model.addAttribute("editItem", editItem);
        return "/business/chai/expiration/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:chai:expiration:edit")
    @ResponseBody
    public ResultVo<?> save(ChaiExpiration saveItem) {
        if (saveItem.getId() != null) {
            ChaiExpiration oldEntity = chaiExpirationService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (StringUtils.isBlank(saveItem.getName())) {
            return ResultVoUtil.error("显示名必填");
        }
        saveItem.setName(saveItem.getName().trim());
        if (saveItem.getName().length() > 255) {
            return ResultVoUtil.error("显示名最多255个字符");
        }
        if (chaiExpirationService.isNameTakenByOther(saveItem.getName(), saveItem.getId())) {
            return ResultVoUtil.error("显示名已存在，请更换后重试");
        }
        if (saveItem.getMonths() == null) {
            return ResultVoUtil.error("月数必填");
        }
        if (saveItem.getMonths() < 0) {
            return ResultVoUtil.error("月数不能为负数（0 表示长期）");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (saveItem.getOrderNum() == null) {
            saveItem.setOrderNum(0);
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        chaiExpirationService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:chai:expiration:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        chaiExpirationService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:chai:expiration:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            ChaiExpiration entity = chaiExpirationService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                chaiExpirationService.save(entity);
            }
        }
        return ResultVoUtil.success("操作成功");
    }
}
