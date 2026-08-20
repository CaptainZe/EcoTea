package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
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
@RequestMapping("/business/chai/brand")
@RequiredArgsConstructor
public class ChaiBrandController {

    private final ChaiBrandService chaiBrandService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:brand:index")
    public String index(Model model, ChaiBrand queryParam) {
        Page<ChaiBrand> page = chaiBrandService.getPageList(queryParam);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        return "/business/chai/brand/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:chai:brand:index")
    public String toEdit(@PathVariable(value = "id", required = false) ChaiBrand editItem, Model model) {
        if (editItem == null) {
            editItem = new ChaiBrand();
            editItem.setStatus(ChaiStatus.ONLINE.getCode());
            editItem.setOrderNum(0);
            editItem.setLogo("");
        }
        model.addAttribute("editItem", editItem);
        return "/business/chai/brand/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:chai:brand:edit")
    @ResponseBody
    public ResultVo<?> save(ChaiBrand saveItem) {
        if (saveItem.getId() != null) {
            ChaiBrand oldEntity = chaiBrandService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (StringUtils.isBlank(saveItem.getName())) {
            return ResultVoUtil.error("品牌名称必填");
        }
        saveItem.setName(saveItem.getName().trim());
        if (saveItem.getName().length() > 255) {
            return ResultVoUtil.error("品牌名称最多255个字符");
        }
        if (chaiBrandService.isNameTakenByOther(saveItem.getName(), saveItem.getId())) {
            return ResultVoUtil.error("品牌名称已存在，请更换后重试");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (saveItem.getOrderNum() == null) {
            saveItem.setOrderNum(0);
        }
        // logo 非必填
        if (StringUtils.isBlank(saveItem.getLogo())) {
            saveItem.setLogo("");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        chaiBrandService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:chai:brand:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        try {
            List<String> blocked = chaiBrandService.deleteByIdIn(ids);
            if (blocked.isEmpty()) {
                return ResultVoUtil.success("删除成功");
            }
            return ResultVoUtil.success("以下已被商品引用，未删除：" + String.join("、", blocked));
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:chai:brand:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            ChaiBrand entity = chaiBrandService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                chaiBrandService.save(entity);
            }
        }
        return ResultVoUtil.success("操作成功");
    }
}
