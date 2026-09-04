package com.appsinnova.admin.business.controller.wx;

import com.appsinnova.admin.business.common.enums.wx.WxGlobalConfigType;
import com.appsinnova.admin.business.common.utils.JsonUtils;
import com.appsinnova.admin.business.common.utils.wx.WxGlobalConfigUtil;
import com.appsinnova.admin.business.domain.wx.WxGlobalConfig;
import com.appsinnova.admin.business.service.wx.WxGlobalConfigService;
import com.appsinnova.admin.business.vo.wx.WxCustomerServiceConfig;
import com.appsinnova.admin.business.vo.wx.WxCustomerServiceItem;
import com.appsinnova.admin.business.vo.wx.WxRecycleDescConfig;
import com.appsinnova.admin.business.vo.wx.WxSaleH5CopyConfig;
import com.appsinnova.admin.business.vo.wx.WxSubscribeWelcomeConfig;
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

/**
 * 微信通用配置（wx_global_config）。按 type 使用 Transient 强类型配置对象。
 */
@Controller
@RequestMapping("/business/wx/globalConfig")
@RequiredArgsConstructor
public class WxGlobalConfigController {

    private final WxGlobalConfigService wxGlobalConfigService;

    @GetMapping("/index")
    @RequiresPermissions("business:wx:globalConfig:index")
    public String index(Model model, WxGlobalConfig queryParam) {
        Page<WxGlobalConfig> list = wxGlobalConfigService.getPageList(queryParam);
        list.forEach(item -> item.setDisplayConfig(WxGlobalConfigUtil.buildConfigDisplay(item)));
        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        return "/business/wx/globalConfig/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:wx:globalConfig:edit")
    public String toEdit(@PathVariable(value = "id", required = false) WxGlobalConfig editItem, Model model) {
        if (editItem == null) {
            editItem = new WxGlobalConfig();
            editItem.setType(WxGlobalConfigType.SUBSCRIBE_WELCOME.getCode());
        }
        this.parseConfigToForm(editItem);
        this.ensureFormObjects(editItem);
        model.addAttribute("editItem", editItem);
        return "/business/wx/globalConfig/edit";
    }

    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:wx:globalConfig:edit")
    public String toCopy(@PathVariable(value = "id") WxGlobalConfig editItem, Model model) {
        editItem.setId(null);
        this.parseConfigToForm(editItem);
        this.ensureFormObjects(editItem);
        model.addAttribute("editItem", editItem);
        return "/business/wx/globalConfig/edit";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:wx:globalConfig:edit")
    @ResponseBody
    public ResultVo<?> save(WxGlobalConfig saveItem) {
        if (saveItem.getId() != null) {
            WxGlobalConfig oldEntity = wxGlobalConfigService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        if (saveItem.getType() == null) {
            return ResultVoUtil.error("配置类型必选");
        }
        if (WxGlobalConfigType.ofCode(saveItem.getType()) == null) {
            return ResultVoUtil.error("未知的配置类型");
        }

        String validateMsg = this.validateConfigByType(saveItem);
        if (StringUtils.isNotBlank(validateMsg)) {
            return ResultVoUtil.error(validateMsg);
        }

        String configJson = this.buildConfigJson(saveItem);
        if (StringUtils.isBlank(configJson)) {
            return ResultVoUtil.error("配置内容序列化失败");
        }
        saveItem.setConfig(configJson);

        WxGlobalConfig typeExistItem = wxGlobalConfigService.getByType(saveItem.getType());
        if (typeExistItem != null && (saveItem.getId() == null || !typeExistItem.getId().equals(saveItem.getId()))) {
            return ResultVoUtil.error("该配置类型已存在，请直接编辑原记录（复制后请改类型或改原记录）");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        wxGlobalConfigService.save(saveItem);
        return ResultVoUtil.SAVE_SUCCESS;
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:wx:globalConfig:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        wxGlobalConfigService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }

    private void ensureFormObjects(WxGlobalConfig item) {
        if (item.getSubscribeWelcomeConfig() == null) {
            item.setSubscribeWelcomeConfig(new WxSubscribeWelcomeConfig());
        }
        if (item.getRecycleDescConfig() == null) {
            item.setRecycleDescConfig(new WxRecycleDescConfig());
        }
        if (item.getCustomerServiceConfig() == null) {
            item.setCustomerServiceConfig(new WxCustomerServiceConfig());
        }
        if (item.getCustomerServiceConfig().getItems() == null) {
            item.getCustomerServiceConfig().setItems(new java.util.ArrayList<>());
        }
        if (item.getCustomerServiceConfig().getItems().isEmpty()) {
            item.getCustomerServiceConfig().getItems().add(new WxCustomerServiceItem());
        }
        if (item.getSaleH5CopyConfig() == null) {
            item.setSaleH5CopyConfig(new WxSaleH5CopyConfig());
        }
    }

    private void parseConfigToForm(WxGlobalConfig item) {
        if (StringUtils.isBlank(item.getConfig()) || item.getType() == null) {
            return;
        }
        WxGlobalConfigType type = WxGlobalConfigType.ofCode(item.getType());
        if (type == null) {
            return;
        }
        switch (type) {
            case SUBSCRIBE_WELCOME:
                item.setSubscribeWelcomeConfig(
                        JsonUtils.readValue(item.getConfig(), WxSubscribeWelcomeConfig.class));
                break;
            case RECYCLE_DESC:
                item.setRecycleDescConfig(
                        JsonUtils.readValue(item.getConfig(), WxRecycleDescConfig.class));
                break;
            case CUSTOMER_SERVICE:
                item.setCustomerServiceConfig(
                        JsonUtils.readValue(item.getConfig(), WxCustomerServiceConfig.class));
                break;
            case SALE_H5_COPY:
                item.setSaleH5CopyConfig(
                        JsonUtils.readValue(item.getConfig(), WxSaleH5CopyConfig.class));
                break;
            default:
                break;
        }
    }

    private String validateConfigByType(WxGlobalConfig saveItem) {
        WxGlobalConfigType type = WxGlobalConfigType.ofCode(saveItem.getType());
        if (type == null) {
            return "未知的配置类型";
        }
        switch (type) {
            case SUBSCRIBE_WELCOME: {
                WxSubscribeWelcomeConfig c = saveItem.getSubscribeWelcomeConfig();
                if (c == null || StringUtils.isBlank(c.getText())) {
                    return "欢迎语正文不能为空";
                }
                return "";
            }
            case RECYCLE_DESC: {
                WxRecycleDescConfig c = saveItem.getRecycleDescConfig();
                if (c == null || StringUtils.isBlank(c.getBody())) {
                    return "回收说明正文不能为空";
                }
                return "";
            }
            case CUSTOMER_SERVICE: {
                WxCustomerServiceConfig c = saveItem.getCustomerServiceConfig();
                if (c == null) {
                    return "请至少添加一名客服";
                }
                cleanCsItems(c);
                if (c.getItems() == null || c.getItems().isEmpty()) {
                    return "请至少添加一名客服";
                }
                for (int i = 0; i < c.getItems().size(); i++) {
                    WxCustomerServiceItem it = c.getItems().get(i);
                    if (StringUtils.isBlank(it.getWechatId())) {
                        return "第" + (i + 1) + "名客服的微信号（说明）必填";
                    }
                    if (StringUtils.isBlank(it.getQrImageUrl())) {
                        return "第" + (i + 1) + "名客服的二维码必填";
                    }
                }
                return "";
            }
            case SALE_H5_COPY: {
                WxSaleH5CopyConfig c = saveItem.getSaleH5CopyConfig();
                if (c == null || (StringUtils.isBlank(c.getNoticeBody())
                        && StringUtils.isBlank(c.getBannerText())
                        && StringUtils.isBlank(c.getNoticeTitle()))) {
                    return "销售 H5 文案至少填写一项";
                }
                return "";
            }
            default:
                return "未知的配置类型";
        }
    }

    private String buildConfigJson(WxGlobalConfig saveItem) {
        WxGlobalConfigType type = WxGlobalConfigType.ofCode(saveItem.getType());
        if (type == null) {
            return null;
        }
        switch (type) {
            case SUBSCRIBE_WELCOME: {
                WxSubscribeWelcomeConfig c = saveItem.getSubscribeWelcomeConfig();
                c.setText(c.getText().trim());
                return JsonUtils.writeValueAsString(c);
            }
            case RECYCLE_DESC: {
                WxRecycleDescConfig c = saveItem.getRecycleDescConfig();
                trimRecycle(c);
                return JsonUtils.writeValueAsString(c);
            }
            case CUSTOMER_SERVICE: {
                WxCustomerServiceConfig c = saveItem.getCustomerServiceConfig();
                trimCs(c);
                return JsonUtils.writeValueAsString(c);
            }
            case SALE_H5_COPY: {
                WxSaleH5CopyConfig c = saveItem.getSaleH5CopyConfig();
                trimSaleH5(c);
                return JsonUtils.writeValueAsString(c);
            }
            default:
                return null;
        }
    }

    private void trimRecycle(WxRecycleDescConfig c) {
        if (c.getTitle() != null) {
            c.setTitle(c.getTitle().trim());
        }
        if (c.getBody() != null) {
            c.setBody(c.getBody().trim());
        }
        if (c.getCtaText() != null) {
            c.setCtaText(c.getCtaText().trim());
        }
        if (c.getCtaUrl() != null) {
            c.setCtaUrl(c.getCtaUrl().trim());
        }
    }

    private void trimCs(WxCustomerServiceConfig c) {
        cleanCsItems(c);
        for (WxCustomerServiceItem item : c.getItems()) {
            item.setWechatId(item.getWechatId().trim());
            item.setQrImageUrl(item.getQrImageUrl().trim());
        }
    }

    /** 去掉完全空白的客服项 */
    private void cleanCsItems(WxCustomerServiceConfig c) {
        if (c.getItems() == null) {
            c.setItems(new java.util.ArrayList<>());
            return;
        }
        List<WxCustomerServiceItem> cleaned = new java.util.ArrayList<>();
        for (WxCustomerServiceItem item : c.getItems()) {
            if (item == null) {
                continue;
            }
            if (StringUtils.isBlank(item.getWechatId()) && StringUtils.isBlank(item.getQrImageUrl())) {
                continue;
            }
            cleaned.add(item);
        }
        c.setItems(cleaned);
    }

    private void trimSaleH5(WxSaleH5CopyConfig c) {
        if (c.getNoticeTitle() != null) {
            c.setNoticeTitle(c.getNoticeTitle().trim());
        }
        if (c.getNoticeBody() != null) {
            c.setNoticeBody(c.getNoticeBody().trim());
        }
        if (c.getBannerText() != null) {
            c.setBannerText(c.getBannerText().trim());
        }
    }
}
