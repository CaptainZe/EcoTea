package com.appsinnova.admin.business.controller.wx;

import com.appsinnova.admin.business.common.utils.wx.WxMpMenuDefaultConfig;
import com.appsinnova.admin.business.domain.wx.WxMpMenu;
import com.appsinnova.admin.business.service.wx.WxMpMenuPublishClient;
import com.appsinnova.admin.business.service.wx.WxMpMenuService;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import lombok.RequiredArgsConstructor;
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

/**
 * 微信自定义菜单（wx_mp_menu）。
 * 保存只落库；「发布到微信」才经 api 鉴权 menuCreate。
 */
@Controller
@RequestMapping("/business/wx/mpMenu")
@RequiredArgsConstructor
public class WxMpMenuController {

    private final WxMpMenuService wxMpMenuService;
    private final WxMpMenuPublishClient wxMpMenuPublishClient;

    @GetMapping("/index")
    @RequiresPermissions("business:wx:mpMenu:index")
    public String index(Model model, WxMpMenu queryParam) {
        Page<WxMpMenu> list = wxMpMenuService.getPageList(queryParam);
        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        return "/business/wx/mpMenu/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:wx:mpMenu:edit")
    public String toEdit(@PathVariable(value = "id", required = false) WxMpMenu editItem, Model model) {
        if (editItem == null) {
            editItem = new WxMpMenu();
            editItem.setConfig(WxMpMenuDefaultConfig.defaultMenuJson());
        }
        model.addAttribute("editItem", editItem);
        return "/business/wx/mpMenu/edit";
    }

    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:wx:mpMenu:edit")
    public String toCopy(@PathVariable(value = "id") WxMpMenu editItem, Model model) {
        editItem.setId(null);
        model.addAttribute("editItem", editItem);
        return "/business/wx/mpMenu/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:wx:mpMenu:edit")
    @ResponseBody
    public ResultVo<?> save(WxMpMenu saveItem) {
        if (saveItem.getId() != null) {
            WxMpMenu oldEntity = wxMpMenuService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (StringUtils.isBlank(saveItem.getAppId())) {
            return ResultVoUtil.error("AppID 必填");
        }
        if (StringUtils.isBlank(saveItem.getAppName())) {
            return ResultVoUtil.error("订阅号名必填");
        }
        String config = StringUtils.trimToEmpty(saveItem.getConfig());
        if (StringUtils.isBlank(config)) {
            return ResultVoUtil.error("菜单 config 必填");
        }
        if (!config.startsWith("{") || !config.contains("button")) {
            return ResultVoUtil.error("config 须为含 button 的 JSON 对象");
        }
        saveItem.setAppId(saveItem.getAppId().trim());
        saveItem.setAppName(saveItem.getAppName().trim());
        saveItem.setConfig(config);

        WxMpMenu exist = wxMpMenuService.getByAppId(saveItem.getAppId());
        if (exist != null && (saveItem.getId() == null || !exist.getId().equals(saveItem.getId()))) {
            return ResultVoUtil.error("该 AppID 已存在菜单配置，请直接编辑原记录");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        wxMpMenuService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:wx:mpMenu:delete")
    @ResponseBody
    public ResultVo<?> delete(@RequestParam("ids") List<Long> idList) {
        wxMpMenuService.deleteByIdIn(idList);
        return ResultVoUtil.success("删除成功");
    }

    /**
     * 发布到微信：admin → api（Api-Key）→ menuCreate。
     */
    @RequestMapping("/publish")
    @RequiresPermissions("business:wx:mpMenu:publish")
    @ResponseBody
    public ResultVo<?> publish(@RequestParam("id") Long id) {
        WxMpMenu row = wxMpMenuService.getById(id);
        if (row == null) {
            return ResultVoUtil.error("菜单配置不存在");
        }
        String err = wxMpMenuPublishClient.publishById(id);
        if (err != null) {
            return ResultVoUtil.error(err);
        }
        return ResultVoUtil.success("已发布到微信");
    }
}
