package com.appsinnova.admin.business.controller.tea;

import com.appsinnova.admin.business.common.enums.AppNoticeType;
import com.appsinnova.admin.business.common.enums.AppSecretKeyType;
import com.appsinnova.admin.business.common.enums.SkuStatus;
import com.appsinnova.admin.business.common.enums.tea.AppearanceCondition;
import com.appsinnova.admin.business.common.enums.tea.HasBag;
import com.appsinnova.admin.business.common.enums.tea.TeaQuoteOrderStatus;
import com.appsinnova.admin.business.common.enums.tea.TeaQuoteOrderType;
import com.appsinnova.admin.business.domain.sys.AppSecretKey;
import com.appsinnova.admin.business.domain.tea.TeaQuoteOrder;
import com.appsinnova.admin.business.domain.tea.TeaQuoteOrderItem;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.service.base.FeiShuWebhookService;
import com.appsinnova.admin.business.service.sys.AppNoticeService;
import com.appsinnova.admin.business.service.sys.AppSecretKeyService;
import com.appsinnova.admin.business.service.tea.TeaQuoteOrderService;
import com.appsinnova.admin.business.service.tea.TeaSkuService;
import com.appsinnova.admin.business.vo.base.PayInfoVo;
import com.appsinnova.admin.business.vo.tea.TeaQuoteOrderSubmitVo;
import com.appsinnova.admin.business.vo.tea.TeaQuoteOrderSupplementVo;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/business/tea/teaQuotation")
@RequiredArgsConstructor
public class TeaQuotationController {

    private final TeaSkuService teaSkuService;
    private final AppNoticeService appNoticeService;
    private final TeaQuoteOrderService teaQuoteOrderService;
    private final AppSecretKeyService appSecretKeyService;
    private final FeiShuWebhookService feiShuWebhookService;

    // 报价必读
    @GetMapping("/readme")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String readme(Model model) {
        model.addAttribute("shippingAddressNotice", appNoticeService.getByType(AppNoticeType.TEA_SHIPPING_ADDRESS.getCode()));
        model.addAttribute("platformRulesNotice", appNoticeService.getByType(AppNoticeType.TEA_PLATFORM_RULES.getCode()));
        model.addAttribute("orderGuideNotice", appNoticeService.getByType(AppNoticeType.TEA_ORDER_GUIDE.getCode()));
        return "/business/tea/teaQuotation/readme";
    }

    // 列表页
    @GetMapping("/index")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String index(Model model, TeaSku queryParam,
                        @RequestParam(value = "selectMode", required = false, defaultValue = "0") Integer selectMode) {
        // 茶叶报价页面仅展示上架中的SKU
        queryParam.setStatus(SkuStatus.ONLINE.getCode());
        Page<TeaSku> list = teaSkuService.getPageList(queryParam);

        list.forEach(item -> {
            if (item.getRecyclePrice() != null && item.getRecyclePriceReducePer() != null) {
                BigDecimal reduceAmount = item.getRecyclePrice()
                        .multiply(BigDecimal.valueOf(item.getRecyclePriceReducePer()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal recycleAfterReduceAmount = item.getRecyclePrice()
                        .subtract(reduceAmount)
                        .setScale(2, RoundingMode.HALF_UP);
                item.setRecycleReduceAmountShow(recycleAfterReduceAmount.toPlainString());
            }

            if (StringUtils.isNotBlank(item.getImageUrls())) {
                String imageShow = "<div class=\"photo-group\">";
                List<String> urlList = JsonUtils.readValue(item.getImageUrls(), new TypeReference<List<String>>() {});
                for (String imgItem : urlList) {
                    if (StringUtils.isBlank(imgItem)) {
                        continue;
                    }
                    imageShow += "<img class=\"preview-img\" layer-src=\"" + imgItem +
                            "\" src=\"" + imgItem +
                            "\" style=\"width:60px;cursor:pointer;\">";
                    imageShow += "&nbsp;&nbsp;";
                }
                imageShow += "</div>";
                item.setImageShow(imageShow);
            }
        });

        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        model.addAttribute("queryParam", queryParam);
        model.addAttribute("selectMode", selectMode != null && selectMode == 1);
        return "/business/tea/teaQuotation/index";
    }

    // 报单页
    @GetMapping("/quote")
    @RequiresPermissions("business:tea:teaQuotation:quote")
    public String quote(@RequestParam(value = "ids", required = false) List<Long> ids, Model model) {
        List<TeaSku> selectedSkuList = new ArrayList<>();
        Map<Long, String> selectedSkuBrandMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(ids)) {
            selectedSkuList = teaSkuService.getByIdIn(ids).stream()
                    .filter(item -> item.getStatus() != null && item.getStatus().equals(SkuStatus.ONLINE.getCode()))
                    .collect(Collectors.toList());
            for (TeaSku item : selectedSkuList) {
                selectedSkuBrandMap.put(item.getId(), DictUtils.keyValue("TEA_BRAND", String.valueOf(item.getBrand())));
            }
        }
        model.addAttribute("selectedSkuList", selectedSkuList);
        model.addAttribute("selectedSkuBrandMap", selectedSkuBrandMap);
        Map<String, String> appearanceDict = DictUtils.value("TEA_APPEARANCE_CONDITION");
        Map<String, String> hasBagDict = DictUtils.value("TEA_HAS_BAG");
        model.addAttribute("appearanceDictJson", JsonUtils.writeValueAsString(appearanceDict != null ? appearanceDict : Collections.emptyMap()));
        model.addAttribute("hasBagDictJson", JsonUtils.writeValueAsString(hasBagDict != null ? hasBagDict : Collections.emptyMap()));
        model.addAttribute("defaultAppearanceCode", AppearanceCondition.COMPLETE.getCode());
        model.addAttribute("defaultHasBagCode", HasBag.YES.getCode());
        return "/business/tea/teaQuotation/quote";
    }

    // 商品选择后回填详情
    @PostMapping("/product/listByIds")
    @RequiresPermissions("business:tea:teaQuotation:index")
    @ResponseBody
    public ResultVo<?> listProductByIds(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.success(new ArrayList<>());
        }
        List<TeaSku> skuList = teaSkuService.getByIdIn(ids).stream()
                .filter(item -> item.getStatus() != null && item.getStatus().equals(SkuStatus.ONLINE.getCode()))
                .collect(Collectors.toList());
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (TeaSku item : skuList) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", item.getId());
            data.put("skuCode", item.getSkuCode());
            data.put("name", item.getName());
            data.put("brandName", DictUtils.keyValue("TEA_BRAND", String.valueOf(item.getBrand())));
            data.put("spec", item.getSpec());
            data.put("productionBatch", item.getProductionBatch());
            data.put("recyclePrice", item.getRecyclePrice());
            data.put("recyclePriceReducePer", item.getRecyclePriceReducePer());
            data.put("recyclePriceReduceNoBag", item.getRecyclePriceReduceNoBag());
            dataList.add(data);
        }
        return ResultVoUtil.success(dataList);
    }

    // 报单保存
    @PostMapping("/quote/save")
    @RequiresPermissions("business:tea:teaQuotation:quote")
    @ResponseBody
    public ResultVo<?> saveQuote(@RequestBody TeaQuoteOrderSubmitVo submitVo) {
        if (submitVo == null) {
            return ResultVoUtil.error("参数不能为空");
        }
        if (CollectionUtils.isEmpty(submitVo.getItemList())) {
            return ResultVoUtil.error("请至少选择一个商品并填写数量");
        }
        User submitUser = ShiroUtil.getSubject();
        if (submitUser == null) {
            return ResultVoUtil.error("请先登录");
        }

        submitVo.setUserId(ShiroUtil.getSubject().getId());
        submitVo.setUserName(ShiroUtil.getSubject().getNickname());
        submitVo.setType(TeaQuoteOrderType.Self.getCode());
        String operator = ShiroUtil.getSubject().getNickname();

        try {
            String orderNo = teaQuoteOrderService.createQuoteOrder(submitVo, operator);
            sendQuoteSubmitNoticeAsync(orderNo);
            return ResultVoUtil.success("报价单已提交，单号：" + orderNo);
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        } catch (Exception ex) {
            return ResultVoUtil.error("报价提交失败");
        }
    }

    // 我的报价订单列表（仅当前登录用户）
    @GetMapping("/orderList")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String orderList(Model model, TeaQuoteOrder queryParam) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return "redirect:/login";
        }
        // 仅查当前用户数据，覆盖请求中可能携带的 userId
        queryParam.setUserId(user.getId());
        Page<TeaQuoteOrder> page = teaQuoteOrderService.getPageList(queryParam);
        teaQuoteOrderService.attachBrandQuantitySummary(page.getContent());
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("queryParam", queryParam);
        return "/business/tea/teaQuotation/orderList";
    }

    // 报价单详情
    @GetMapping("/order/detail/{id}")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return "redirect:/login";
        }
        TeaQuoteOrder order = teaQuoteOrderService.getByIdAndUserId(id, user.getId());
        if (order == null) {
            model.addAttribute("errorMsg", "报价单不存在或无权查看");
            return "/business/tea/teaQuotation/orderDetail";
        }
        PayInfoVo payInfoVo = teaQuoteOrderService.parsePayInfo(order.getPayInfo());
        List<TeaQuoteOrderItem> itemList = teaQuoteOrderService.getItemListByOrderId(order.getId());
        model.addAttribute("order", order);
        model.addAttribute("payInfoVo", payInfoVo);
        model.addAttribute("itemList", itemList);
        return "/business/tea/teaQuotation/orderDetail";
    }

    // 取消报价单（仅【已提交】，未到【已确认】）
    @RequestMapping("/order/cancel")
    @RequiresPermissions("business:tea:teaQuotation:index")
    @ResponseBody
    public ResultVo<?> cancelOrder(@RequestParam("id") Long id) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return ResultVoUtil.error("请先登录");
        }
        TeaQuoteOrder snap = teaQuoteOrderService.getByIdAndUserId(id, user.getId());
        if (snap == null) {
            return ResultVoUtil.error("报价单不存在或无权操作");
        }
        try {
            teaQuoteOrderService.cancelOrderForUser(id, user.getId());
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
        sendQuoteCancelNoticeAsync(snap);
        return ResultVoUtil.success("已取消");
    }

    // 补充快递、打款信息（验收之前）
    @GetMapping("/order/supplement/{id}")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String orderSupplement(@PathVariable("id") Long id, Model model) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return "redirect:/login";
        }
        TeaQuoteOrder order = teaQuoteOrderService.getByIdAndUserId(id, user.getId());
        if (order == null) {
            model.addAttribute("errorMsg", "报价单不存在或无权查看");
            return "/business/tea/teaQuotation/supplement";
        }
        if (order.getStatus() != null && order.getStatus() >= TeaQuoteOrderStatus.ACCEPTED.getCode()) {
            model.addAttribute("errorMsg", "当前状态不可补充信息");
            return "/business/tea/teaQuotation/supplement";
        }
        PayInfoVo payInfoVo = teaQuoteOrderService.parsePayInfo(order.getPayInfo());
        model.addAttribute("editOrder", order);
        model.addAttribute("payInfoVo", payInfoVo);
        return "/business/tea/teaQuotation/supplement";
    }

    @PostMapping("/order/supplement/save/{orderId}")
    @RequiresPermissions("business:tea:teaQuotation:index")
    @ResponseBody
    public ResultVo<?> saveOrderSupplement(@PathVariable("orderId") Long orderId,
                                           @RequestBody TeaQuoteOrderSupplementVo vo) {
        User user = ShiroUtil.getSubject();
        if (user == null) {
            return ResultVoUtil.error("请先登录");
        }
        if (!validateSupplementPay(vo)) {
            return ResultVoUtil.error("请按打款方式填写完整信息");
        }
        try {
            teaQuoteOrderService.updateExpressAndPayForUser(orderId, user.getId(), vo);
            return ResultVoUtil.success("保存成功");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    private void sendQuoteSubmitNoticeAsync(String orderNo) {
        CompletableFuture.runAsync(() -> {
            try {
                AppSecretKey appSecretKey = appSecretKeyService.getSecretKey(AppSecretKeyType.TEA_ROBOT_QUOTE_ORDER.getCode());
                if (appSecretKey == null || StringUtils.isBlank(appSecretKey.getAccessSecret())) {
                    return;
                }
                TeaQuoteOrder order = teaQuoteOrderService.getByOrderNo(orderNo);
                if (order == null || !TeaQuoteOrderType.Self.getCode().equals(order.getType())) {
                    return;
                }
                List<TeaQuoteOrderItem> itemList = teaQuoteOrderService.getItemListByOrderId(order.getId());
                String markdown = buildQuoteSubmitMarkdown(order, itemList);
                feiShuWebhookService.sendMarkdownCard(
                        appSecretKey.getAccessSecret(),
                        "报价单已提交",
                        markdown,
                        "green");
            } catch (Exception ex) {
                log.warn("飞书通知-报价单提交失败: {}", ex.getMessage());
            }
        });
    }

    private void sendQuoteCancelNoticeAsync(TeaQuoteOrder orderBeforeCancel) {
        CompletableFuture.runAsync(() -> {
            try {
                AppSecretKey appSecretKey = appSecretKeyService.getSecretKey(AppSecretKeyType.TEA_ROBOT_QUOTE_ORDER.getCode());
                if (appSecretKey == null || StringUtils.isBlank(appSecretKey.getAccessSecret())) {
                    return;
                }
                String markdown = buildQuoteCancelMarkdown(orderBeforeCancel);
                feiShuWebhookService.sendMarkdownCard(
                        appSecretKey.getAccessSecret(),
                        "报价单已取消",
                        markdown,
                        "orange");
            } catch (Exception ex) {
                log.warn("飞书通知-报价单取消失败: {}", ex.getMessage());
            }
        });
    }

    private String buildQuoteSubmitMarkdown(TeaQuoteOrder order, List<TeaQuoteOrderItem> itemList) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder md = new StringBuilder();
        md.append("**报单概要**\n");
        md.append("- 单号：").append(StringUtils.defaultString(order.getOrderNo(), "-")).append("\n");
        md.append("- 报单人：").append(StringUtils.defaultString(order.getUserName(), "-")).append("\n");
        md.append("- 总金额：").append(formatMoneyYuan(order.getTotalAmount())).append("\n");
        md.append("- 总数量：").append(order.getTotalQuantity() == null ? "-" : String.valueOf(order.getTotalQuantity())).append("\n");
        md.append("- 提交时间：").append(order.getCreateTime() == null ? "-" : sdf.format(new Date(order.getCreateTime()))).append("\n\n");
        if (itemList != null && !itemList.isEmpty()) {
            md.append("**明细（前 15 行）**\n");
            int max = Math.min(itemList.size(), 15);
            for (int i = 0; i < max; i++) {
                TeaQuoteOrderItem it = itemList.get(i);
                String appearance = it.getAppearanceCondition() == null ? "-"
                        : StringUtils.defaultString(DictUtils.keyValue("TEA_APPEARANCE_CONDITION", String.valueOf(it.getAppearanceCondition())), "-");
                String hasBag = it.getHasBag() == null ? "-"
                        : StringUtils.defaultString(DictUtils.keyValue("TEA_HAS_BAG", String.valueOf(it.getHasBag())), "-");
                int qty = it.getQuantity() == null ? 0 : it.getQuantity();
                md.append("- ")
                        .append(quoteDetailBracket(it.getSkuBrand()))
                        .append(quoteDetailBracket(it.getSkuName()))
                        .append(quoteDetailBracket(it.getSkuSpec()))
                        .append(quoteDetailBracket(it.getSkuProductionBatch()))
                        .append(quoteDetailBracket(appearance))
                        .append(quoteDetailBracket(hasBag))
                        .append("x ").append(qty)
                        .append("，小计 ").append(formatMoneyYuan(it.getAmount()))
                        .append("\n");
            }
            if (itemList.size() > 15) {
                md.append("- … 共 **").append(itemList.size()).append("** 行明细\n");
            }
        }
        return md.toString();
    }

    private String buildQuoteCancelMarkdown(TeaQuoteOrder order) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long now = System.currentTimeMillis();

        StringBuilder md = new StringBuilder();
        md.append("**报单信息**\n");
        md.append("- 单号：").append(StringUtils.defaultString(order.getOrderNo(), "-")).append("\n");
        md.append("- 报单人：").append(StringUtils.defaultString(order.getUserName(), "-")).append("\n");
        md.append("- 总金额：").append(formatMoneyYuan(order.getTotalAmount())).append("\n");
        md.append("- 总数量：").append(order.getTotalQuantity() == null ? "-" : String.valueOf(order.getTotalQuantity())).append("\n");
        md.append("- 取消时间：").append(sdf.format(new Date(now))).append("\n");
        return md.toString();
    }

    /** 飞书明细行：【文案】，空则【-】 */
    private static String quoteDetailBracket(String text) {
        if (StringUtils.isBlank(text)) {
            return "【-】";
        }
        return "【" + text.trim() + "】";
    }

    private static String formatMoneyYuan(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return amount.stripTrailingZeros().toPlainString() + " 元";
    }

    private boolean validateSupplementPay(TeaQuoteOrderSupplementVo vo) {
        if (vo == null || vo.getPayMethod() == null) {
            return true;
        }
        PayInfoVo p = vo.getPayInfo();
        if (p == null) {
            return false;
        }
        if (vo.getPayMethod() == 1) {
            return StringUtils.isNotBlank(p.getWxQrCode());
        }
        if (vo.getPayMethod() == 2) {
            return StringUtils.isNotBlank(p.getAlipayQrCode());
        }
        if (vo.getPayMethod() == 3) {
            return StringUtils.isNotBlank(p.getBankName())
                    && StringUtils.isNotBlank(p.getBankAccount())
                    && StringUtils.isNotBlank(p.getPayeeName());
        }
        return true;
    }
}
