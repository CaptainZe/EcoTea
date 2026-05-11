package com.appsinnova.admin.business.controller.tea;

import com.appsinnova.admin.business.common.BaseConstant;
import com.appsinnova.admin.business.common.enums.tea.AppearanceCondition;
import com.appsinnova.admin.business.common.enums.tea.HasBag;
import com.appsinnova.admin.business.common.enums.tea.TeaQuoteOrderType;
import com.appsinnova.admin.business.domain.tea.TeaQuoteOrder;
import com.appsinnova.admin.business.domain.tea.TeaQuoteOrderItem;
import com.appsinnova.admin.business.service.tea.TeaQuoteOrderService;
import com.appsinnova.admin.business.vo.base.PayInfoVo;
import com.appsinnova.admin.business.vo.tea.TeaQuoteOrderAuditAdjustRequest;
import com.appsinnova.admin.business.vo.tea.TeaQuoteOrderSubmitVo;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import com.appsinnova.admin.system.service.RoleService;
import com.appsinnova.admin.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 茶叶报价单审核（内部审核者）
 */
@Controller
@RequestMapping("/business/tea/teaQuotationOrder")
@RequiredArgsConstructor
public class TeaQuotationOrderController {

    private final TeaQuoteOrderService teaQuoteOrderService;
    private final RoleService roleService;
    private final UserService userService;

    @GetMapping("/index")
    @RequiresPermissions("business:tea:teaQuotationOrder:index")
    public String index(Model model, TeaQuoteOrder queryParam) {
        if (queryParam == null) {
            queryParam = new TeaQuoteOrder();
        }
        Page<TeaQuoteOrder> page = teaQuoteOrderService.getPageList(queryParam);
        teaQuoteOrderService.attachBrandQuantitySummary(page.getContent());
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("queryParam", queryParam);
        model.addAttribute("partnerUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_PARTNER_ROLE_NAME));
        return "/business/tea/teaQuotationOrder/orderList";
    }

    @GetMapping("/detail/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:index")
    public String detail(@PathVariable("id") Long id, Model model) {
        TeaQuoteOrder order = teaQuoteOrderService.getById(id);
        if (order == null) {
            model.addAttribute("errorMsg", "报价单不存在");
            return "/business/tea/teaQuotationOrder/orderDetail";
        }
        PayInfoVo payInfoVo = teaQuoteOrderService.parsePayInfo(order.getPayInfo());
        List<TeaQuoteOrderItem> itemList = teaQuoteOrderService.getItemListByOrderId(order.getId());
        model.addAttribute("order", order);
        model.addAttribute("payInfoVo", payInfoVo);
        model.addAttribute("itemList", itemList);
        return "/business/tea/teaQuotationOrder/orderDetail";
    }

    // 代建报价
    @GetMapping("/quote")
    @RequiresPermissions("business:tea:teaQuotationOrder:quote")
    public String proxyQuote(Model model) {
        model.addAttribute("selectedSkuList", Collections.emptyList());
        model.addAttribute("selectedSkuBrandMap", Collections.emptyMap());
        Map<String, String> appearanceDict = DictUtils.value("TEA_APPEARANCE_CONDITION");
        Map<String, String> hasBagDict = DictUtils.value("TEA_HAS_BAG");
        model.addAttribute("appearanceDictJson", JsonUtils.writeValueAsString(appearanceDict != null ? appearanceDict : Collections.emptyMap()));
        model.addAttribute("hasBagDictJson", JsonUtils.writeValueAsString(hasBagDict != null ? hasBagDict : Collections.emptyMap()));
        model.addAttribute("defaultAppearanceCode", AppearanceCondition.COMPLETE.getCode());
        model.addAttribute("defaultHasBagCode", HasBag.YES.getCode());
        model.addAttribute("partnerUserList", roleService.listActiveUsersByRoleName(BaseConstant.TEA_PARTNER_ROLE_NAME));
        return "/business/tea/teaQuotationOrder/proxyQuote";
    }

    @PostMapping("/quote/save")
    @RequiresPermissions("business:tea:teaQuotationOrder:quote")
    @ResponseBody
    public ResultVo<?> saveProxyQuote(@RequestBody TeaQuoteOrderSubmitVo submitVo) {
        if (submitVo == null) {
            return ResultVoUtil.error("参数不能为空");
        }
        if (CollectionUtils.isEmpty(submitVo.getItemList())) {
            return ResultVoUtil.error("请至少选择一个商品并填写数量");
        }
        if (submitVo.getUserId() == null) {
            return ResultVoUtil.error("请选择合作方");
        }
        if (!roleService.userHasActiveRoleName(submitVo.getUserId(), BaseConstant.TEA_PARTNER_ROLE_NAME)) {
            return ResultVoUtil.error("所选用户不是合作方角色");
        }
        User partner = userService.getById(submitVo.getUserId());
        if (partner == null) {
            return ResultVoUtil.error("合作方用户不存在");
        }
        User admin = ShiroUtil.getSubject();
        if (admin == null) {
            return ResultVoUtil.error("请先登录");
        }
        submitVo.setUserName(partner.getNickname() != null ? partner.getNickname() : partner.getUsername());
        submitVo.setType(TeaQuoteOrderType.Proxy.getCode());
        try {
            String orderNo = teaQuoteOrderService.createQuoteOrder(submitVo, admin.getNickname());
            return ResultVoUtil.success("代建报价单已提交，单号：" + orderNo);
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        } catch (Exception ex) {
            return ResultVoUtil.error("报价提交失败");
        }
    }

    @GetMapping("/audit/confirm/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:audit")
    public String auditConfirmPage(@PathVariable("id") Long id, Model model) {
        TeaQuoteOrder order = teaQuoteOrderService.getById(id);
        if (order == null) {
            model.addAttribute("errorMsg", "报价单不存在");
            return "/business/tea/teaQuotationOrder/auditConfirm";
        }
        model.addAttribute("order", order);
        model.addAttribute("itemList", teaQuoteOrderService.getItemListByOrderId(id));
        return "/business/tea/teaQuotationOrder/auditConfirm";
    }

    @GetMapping("/audit/accept/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:audit")
    public String auditAcceptPage(@PathVariable("id") Long id, Model model) {
        TeaQuoteOrder order = teaQuoteOrderService.getById(id);
        if (order == null) {
            model.addAttribute("errorMsg", "报价单不存在");
            return "/business/tea/teaQuotationOrder/auditAccept";
        }
        model.addAttribute("order", order);
        model.addAttribute("itemList", teaQuoteOrderService.getItemListByOrderId(id));
        return "/business/tea/teaQuotationOrder/auditAccept";
    }

    @GetMapping("/audit/reject/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:audit")
    @ResponseBody
    public ResultVo<?> auditReject(@PathVariable("id") Long id) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return ResultVoUtil.error("请先登录");
        }
        try {
            teaQuoteOrderService.rejectOrderForAudit(id, user.getNickname());
            return ResultVoUtil.success("已拒绝", "submit[refresh]");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @GetMapping("/audit/pay/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:audit")
    @ResponseBody
    public ResultVo<?> auditPay(@PathVariable("id") Long id) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return ResultVoUtil.error("请先登录");
        }
        try {
            teaQuoteOrderService.payOrderForAudit(id, user.getNickname());
            return ResultVoUtil.success("已标记打款", "submit[refresh]");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @PostMapping("/audit/confirm/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:audit")
    @ResponseBody
    public ResultVo<?> auditConfirmSave(@PathVariable("id") Long id,
                                        @RequestBody(required = false) TeaQuoteOrderAuditAdjustRequest body) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return ResultVoUtil.error("请先登录");
        }
        if (body == null) {
            body = new TeaQuoteOrderAuditAdjustRequest();
        }
        try {
            teaQuoteOrderService.confirmOrderForAudit(id, user.getNickname(), body);
            return ResultVoUtil.success("已确认");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @PostMapping("/audit/accept/{id}")
    @RequiresPermissions("business:tea:teaQuotationOrder:audit")
    @ResponseBody
    public ResultVo<?> auditAcceptSave(@PathVariable("id") Long id,
                                       @RequestBody(required = false) TeaQuoteOrderAuditAdjustRequest body) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return ResultVoUtil.error("请先登录");
        }
        if (body == null) {
            body = new TeaQuoteOrderAuditAdjustRequest();
        }
        try {
            teaQuoteOrderService.acceptOrderForAudit(id, user.getNickname(), body);
            return ResultVoUtil.success("已验收");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }
}
