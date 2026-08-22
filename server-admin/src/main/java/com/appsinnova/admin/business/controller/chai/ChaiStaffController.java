package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.domain.chai.ChaiStaff;
import com.appsinnova.admin.business.service.chai.ChaiStaffService;
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
@RequestMapping("/business/chai/staff")
@RequiredArgsConstructor
public class ChaiStaffController {

    private final ChaiStaffService chaiStaffService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:staff:index")
    public String index(Model model, ChaiStaff queryParam) {
        Page<ChaiStaff> page = chaiStaffService.getPageList(queryParam);
        page.getContent().forEach(chaiStaffService::fillDisplayName);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        return "/business/chai/staff/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:chai:staff:index")
    public String toEdit(@PathVariable(value = "id", required = false) ChaiStaff editItem, Model model) {
        if (editItem == null) {
            editItem = new ChaiStaff();
            editItem.setStatus(ChaiStatus.ONLINE.getCode());
            editItem.setOrderNum(0);
            editItem.setMobile("");
        }
        model.addAttribute("editItem", editItem);
        return "/business/chai/staff/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:chai:staff:edit")
    @ResponseBody
    public ResultVo<?> save(ChaiStaff saveItem) {
        if (saveItem.getId() != null) {
            ChaiStaff oldEntity = chaiStaffService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (StringUtils.isBlank(saveItem.getNickName())) {
            return ResultVoUtil.error("花名必填");
        }
        saveItem.setNickName(saveItem.getNickName().trim());
        if (saveItem.getNickName().length() > 255) {
            return ResultVoUtil.error("花名最多255个字符");
        }
        if (StringUtils.isBlank(saveItem.getRealName())) {
            return ResultVoUtil.error("真实姓名必填");
        }
        saveItem.setRealName(saveItem.getRealName().trim());
        if (saveItem.getRealName().length() > 255) {
            return ResultVoUtil.error("真实姓名最多255个字符");
        }
        if (chaiStaffService.isNickNameTakenByOther(saveItem.getNickName(), saveItem.getId())) {
            return ResultVoUtil.error("花名已存在，请更换后重试");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (saveItem.getOrderNum() == null) {
            saveItem.setOrderNum(0);
        }
        if (StringUtils.isBlank(saveItem.getMobile())) {
            saveItem.setMobile("");
        } else {
            saveItem.setMobile(saveItem.getMobile().trim());
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        chaiStaffService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:chai:staff:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        try {
            List<String> blocked = chaiStaffService.deleteByIdIn(ids);
            if (blocked.isEmpty()) {
                return ResultVoUtil.success("删除成功");
            }
            return ResultVoUtil.success("以下已被库存单据引用，未删除：" + String.join("、", blocked));
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:chai:staff:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            ChaiStaff entity = chaiStaffService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                chaiStaffService.save(entity);
            }
        }
        return ResultVoUtil.success("操作成功");
    }
}
