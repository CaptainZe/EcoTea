package com.appsinnova.admin.business.controller.sys;

import com.appsinnova.admin.business.domain.sys.AppNotice;
import com.appsinnova.admin.business.service.sys.AppNoticeService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/business/sys/appNotice")
@RequiredArgsConstructor
public class AppNoticeController {

    private final AppNoticeService appNoticeService;

    @GetMapping("/index")
    @RequiresPermissions("business:sys:appNotice:index")
    public String index(Model model, AppNotice queryParam) {
        Page<AppNotice> list = appNoticeService.getPageList(queryParam);
        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        return "/business/sys/appNotice/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:sys:appNotice:edit")
    public String toEdit(@PathVariable(value = "id", required = false) AppNotice editItem, Model model) {
        if (editItem == null) {
            editItem = new AppNotice();
        }
        model.addAttribute("editItem", editItem);
        return "/business/sys/appNotice/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:sys:appNotice:edit")
    @ResponseBody
    public ResultVo<?> save(AppNotice saveItem) {
        if (saveItem.getId() != null) {
            AppNotice oldEntity = appNoticeService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (saveItem.getType() == null) {
            return ResultVoUtil.error("公告类型必选");
        }
        if (StringUtils.isBlank(saveItem.getContent())) {
            return ResultVoUtil.error("公告内容不能为空");
        }

        // 类型唯一：同一种公告类型只允许一条记录
        AppNotice typeExistItem = appNoticeService.getByType(saveItem.getType());
        if (typeExistItem != null && (saveItem.getId() == null || !typeExistItem.getId().equals(saveItem.getId()))) {
            return ResultVoUtil.error("该公告类型已存在，请直接编辑原记录");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        appNoticeService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:sys:appNotice:edit")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        appNoticeService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }
}
