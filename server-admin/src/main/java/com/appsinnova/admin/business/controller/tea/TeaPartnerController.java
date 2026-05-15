package com.appsinnova.admin.business.controller.tea;

import com.appsinnova.admin.business.common.BaseConstant;
import com.appsinnova.admin.business.common.enums.tea.TeaPartnerStatus;
import com.appsinnova.admin.business.common.enums.tea.TeaPartnerType;
import com.appsinnova.admin.business.domain.tea.TeaPartner;
import com.appsinnova.admin.business.service.tea.TeaPartnerService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import com.appsinnova.admin.system.service.RoleService;
import com.appsinnova.admin.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/business/tea/teaPartner")
@RequiredArgsConstructor
public class TeaPartnerController {

    private final TeaPartnerService teaPartnerService;
    private final RoleService roleService;
    private final UserService userService;

    @GetMapping("/index")
    @RequiresPermissions("business:tea:teaPartner:index")
    public String index(Model model, TeaPartner queryParam) {
        if (queryParam == null) {
            queryParam = new TeaPartner();
        }

        Page<TeaPartner> page = teaPartnerService.getPageList(queryParam);
        Map<Long, User> userMap = buildRelatedUserMap(page.getContent());
        page.getContent().forEach(item -> {
            item.setLinkedUserShow(formatLinkedUser(item.getUserId(), userMap));
            item.setLiaisonUserShow(formatLinkedUser(item.getLiaisonUserId(), userMap));
        });
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("partnerUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_PARTNER_ROLE_NAME));
        model.addAttribute("salespersonUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_SALESPERSON_ROLE_NAME));
        return "/business/tea/teaPartner/index";
    }

    // 跳转到编辑页面
    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:tea:teaPartner:index")
    public String toEdit(@PathVariable(value = "id", required = false) TeaPartner editItem, Model model) {
        // 初始化（可设置默认值）
        if (editItem == null) {
            editItem = new TeaPartner();
            editItem.setPartnerType(TeaPartnerType.MERCHANT.getCode());
            editItem.setStatus(TeaPartnerStatus.SIGNED.getCode());
            editItem.setUserId(0L);
        }

        model.addAttribute("editItem", editItem);
        model.addAttribute("partnerUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_PARTNER_ROLE_NAME));
        model.addAttribute("salespersonUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_SALESPERSON_ROLE_NAME));
        return "/business/tea/teaPartner/edit";
    }

    // 跳转至拷贝页面
    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:tea:teaPartner:edit")
    public String toCopy(@PathVariable(value = "id") TeaPartner editItem, Model model) {
        editItem.setId(null);
        editItem.setUserId(0L);
        model.addAttribute("editItem", editItem);
        model.addAttribute("partnerUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_PARTNER_ROLE_NAME));
        model.addAttribute("salespersonUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_SALESPERSON_ROLE_NAME));
        return "/business/tea/teaPartner/edit";
    }

    // 保存
    @PostMapping("/save")
    @RequiresPermissions("business:tea:teaPartner:edit")
    @ResponseBody
    public ResultVo<?> save(TeaPartner saveItem) {
        if (saveItem.getId() != null) {
            TeaPartner oldEntity = teaPartnerService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }

            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        // 有效性校验
        if (StringUtils.isBlank(saveItem.getPartnerName())) {
            return ResultVoUtil.error("合作方名称必填");
        }
        if (StringUtils.isBlank(saveItem.getContactName())) {
            return ResultVoUtil.error("联系人姓名必填");
        }
        if (StringUtils.isBlank(saveItem.getContactPhone())) {
            return ResultVoUtil.error("联系电话必填");
        }
        if (StringUtils.isBlank(saveItem.getProvince())) {
            return ResultVoUtil.error("省必填");
        }
        if (StringUtils.isBlank(saveItem.getCity())) {
            return ResultVoUtil.error("市必填");
        }
        if (StringUtils.isBlank(saveItem.getAddress())) {
            return ResultVoUtil.error("详细地址必填");
        }
        if (saveItem.getPartnerType() == null) {
            return ResultVoUtil.error("合作方类型必选");
        }
        if (saveItem.getStatus() == null) {
            return ResultVoUtil.error("状态必选");
        }
        if (saveItem.getRemark() != null && saveItem.getRemark().length() > 500) {
            return ResultVoUtil.error("备注最多500个字符");
        }

        Long liaisonUserId = saveItem.getLiaisonUserId();
        if (liaisonUserId == null || liaisonUserId <= 0) {
            return ResultVoUtil.error("对接客服必选");
        }
        if (!roleService.userHasActiveRoleName(liaisonUserId, BaseConstant.TEA_SALESPERSON_ROLE_NAME)) {
            return ResultVoUtil.error("所选用户不是客服角色");
        }
        if (userService.getById(liaisonUserId) == null) {
            return ResultVoUtil.error("对接客服不存在");
        }

        Long userId = saveItem.getUserId();
        if (userId == null || userId <= 0) {
            saveItem.setUserId(0L);
        } else {
            if (!roleService.userHasActiveRoleName(userId, BaseConstant.TEA_PARTNER_ROLE_NAME)) {
                return ResultVoUtil.error("所选用户不是合作方角色");
            }
            if (userService.getById(userId) == null) {
                return ResultVoUtil.error("关联用户不存在");
            }
            if (teaPartnerService.isUserIdTakenByOther(userId, saveItem.getId())) {
                return ResultVoUtil.error("该用户已关联其他合作方，不能重复添加");
            }
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        teaPartnerService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:tea:teaPartner:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }

        teaPartnerService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }

    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:tea:teaPartner:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }

        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            TeaPartner entity = teaPartnerService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                teaPartnerService.save(entity);
            }
        }
        return ResultVoUtil.success("操作成功");
    }

    private Map<Long, User> buildRelatedUserMap(List<TeaPartner> list) {
        Map<Long, User> map = new HashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            return map;
        }
        for (TeaPartner p : list) {
            putUserIfAbsent(map, p.getUserId());
            putUserIfAbsent(map, p.getLiaisonUserId());
        }
        return map;
    }

    private void putUserIfAbsent(Map<Long, User> map, Long userId) {
        if (userId == null || userId <= 0 || map.containsKey(userId)) {
            return;
        }
        User u = userService.getById(userId);
        if (u != null) {
            map.put(userId, u);
        }
    }

    private static String formatLinkedUser(Long userId, Map<Long, User> userMap) {
        if (userId == null || userId <= 0) {
            return "-";
        }
        User u = userMap.get(userId);
        if (u == null) {
            return String.valueOf(userId);
        }
        return u.getNickname() + "（" + u.getUsername() + "）";
    }
}
