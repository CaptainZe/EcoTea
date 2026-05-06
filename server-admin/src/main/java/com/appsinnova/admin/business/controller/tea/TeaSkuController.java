package com.appsinnova.admin.business.controller.tea;

import com.appsinnova.admin.business.common.enums.AppSecretKeyType;
import com.appsinnova.admin.business.common.enums.SkuStatus;
import com.appsinnova.admin.business.domain.sys.AppSecretKey;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.service.base.FeiShuWebhookService;
import com.appsinnova.admin.business.service.sys.AppSecretKeyService;
import com.appsinnova.admin.business.service.tea.TeaSkuService;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/business/tea/teaSku")
@RequiredArgsConstructor
public class TeaSkuController {

    private final TeaSkuService teaSkuService;
    private final AppSecretKeyService appSecretKeyService;
    private final FeiShuWebhookService feiShuWebhookService;

    // 列表页面
    @GetMapping("/index")
    @RequiresPermissions("business:tea:teaSku:index")
    public String index(Model model, TeaSku queryParam) {
        Page<TeaSku> list = teaSkuService.getPageList(queryParam);
        list.forEach(item -> {
            if (item.getSalePrice() != null) {
                String salePriceShow = item.getSalePrice().stripTrailingZeros().toPlainString();
                if (item.getOfficialPrice() != null && item.getOfficialPrice().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = item.getSalePrice()
                            .multiply(BigDecimal.TEN)
                            .divide(item.getOfficialPrice(), 2, RoundingMode.HALF_UP);
                    salePriceShow += " (" + discount.stripTrailingZeros().toPlainString() + "折)";
                }
                item.setSalePriceShow(salePriceShow);
            }

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
            if (StringUtils.isNotBlank(item.getRealImageUrls())) {
                String realImageShow = "<div class=\"photo-group\">";
                List<String> urlList = JsonUtils.readValue(item.getRealImageUrls(), new TypeReference<List<String>>() {});
                for (String imgItem : urlList) {
                    if (StringUtils.isBlank(imgItem)) {
                        continue;
                    }
                    realImageShow += "<img class=\"preview-img\" layer-src=\"" + imgItem +
                            "\" src=\"" + imgItem +
                            "\" style=\"width:60px;cursor:pointer;\">";
                    realImageShow += "&nbsp;&nbsp;";
                }
                realImageShow += "</div>";
                item.setRealImageShow(realImageShow);
            }
        });

        // 封装数据
        model.addAttribute("list", list.getContent());
        model.addAttribute("page", list);
        return "/business/tea/teaSku/index";
    }

    // 跳转到编辑页面
    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:tea:teaSku:index")
    public String toEdit(@PathVariable(value = "id", required = false) TeaSku editItem, Model model) {
        // 初始化（可设置默认值）
        if (editItem == null) {
            editItem = new TeaSku();
            editItem.setStarLevel(5);
            editItem.setRecyclePriceReducePer(5);
            editItem.setRecyclePriceReduceNoBag(new BigDecimal(10));
            editItem.setStatus(SkuStatus.OFFLINE.getCode());
        }

        model.addAttribute("editItem", editItem);
        return "/business/tea/teaSku/edit";
    }

    // 跳转至拷贝页面
    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:tea:teaSku:edit")
    public String toCopy(@PathVariable(value = "id") TeaSku editItem, Model model) {
        editItem.setId(null);
        model.addAttribute("editItem", editItem);
        return "/business/tea/teaSku/edit";
    }

    // 保存
    @PostMapping("/save")
    @RequiresPermissions("business:tea:teaSku:edit")
    @ResponseBody
    public ResultVo<?> save(TeaSku saveItem) {
        TeaSku oldEntity = null;
        if (saveItem.getId() != null) {
            oldEntity = teaSkuService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }

            saveItem.setId(oldEntity.getId());
            saveItem.setSkuCode(oldEntity.getSkuCode());
            saveItem.setCreateTime(oldEntity.getCreateTime());
        }

        // 有效性校验
        if (StringUtils.isBlank(saveItem.getName())) {
            return ResultVoUtil.error("商品名称必填");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());

        // save 合并到会话内同一托管实体时，会就地覆盖字段；必须在 save 前取出「变更前」价格快照再比较与发通知
        BigDecimal prevSalePrice = null;
        BigDecimal prevRecyclePrice = null;
        BigDecimal prevOfficialPrice = null;
        if (oldEntity != null) {
            prevSalePrice = oldEntity.getSalePrice();
            prevRecyclePrice = oldEntity.getRecyclePrice();
            prevOfficialPrice = oldEntity.getOfficialPrice();
        }

        TeaSku saved = teaSkuService.save(saveItem);

        if (oldEntity != null && saleOrRecyclePriceChanged(prevSalePrice, prevRecyclePrice, saved)) {
            sendTeaSkuPriceChangeNoticeAsync(prevSalePrice, prevRecyclePrice, prevOfficialPrice, saved);
        }
        return ResultVoUtil.SAVE_SUCCESS;
    }

    // 删除
    @RequestMapping("/delete")
    @RequiresPermissions("business:tea:teaSku:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }

        teaSkuService.deleteByIdIn(ids);
        return ResultVoUtil.success("删除成功");
    }

    // 状态修改
    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:tea:teaSku:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }

        for (Long id : ids) {
            TeaSku entity = teaSkuService.getById(id);
            if (entity != null && !status.equals(entity.getStatus())) {
                entity.setStatus(status);
                teaSkuService.save(entity);
            }
        }

        return ResultVoUtil.success("操作成功");
    }

    private static boolean saleOrRecyclePriceChanged(BigDecimal prevSalePrice, BigDecimal prevRecyclePrice, TeaSku saved) {
        return !equalsBigDecimal(prevSalePrice, saved.getSalePrice())
                || !equalsBigDecimal(prevRecyclePrice, saved.getRecyclePrice());
    }

    private static boolean equalsBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private void sendTeaSkuPriceChangeNoticeAsync(BigDecimal prevSalePrice, BigDecimal prevRecyclePrice,
                                                  BigDecimal prevOfficialPrice, TeaSku newSku) {
        AppSecretKey appSecretKey = appSecretKeyService.getSecretKey(AppSecretKeyType.TEA_ROBOT_PRICE_CHANGE.getCode());
        if (appSecretKey == null || StringUtils.isBlank(appSecretKey.getAccessSecret())) {
            return;
        }
        final String webhookUrl = appSecretKey.getAccessSecret();
        final String markdown = buildTeaSkuPriceChangeMarkdown(prevSalePrice, prevRecyclePrice, prevOfficialPrice, newSku);
        CompletableFuture.runAsync(() ->
                feiShuWebhookService.sendMarkdownCard(webhookUrl, "价格变更通知", markdown));
    }

    private String buildTeaSkuPriceChangeMarkdown(BigDecimal prevSalePrice, BigDecimal prevRecyclePrice,
                                                BigDecimal prevOfficialPrice, TeaSku newSku) {
        String brandName = resolveTeaBrandName(newSku.getBrand());
        StringBuilder md = new StringBuilder();
        md.append("**商品基础信息**\n");
        md.append("- 品牌：").append(brandName).append("\n");
        md.append("- 商品名称：").append(StringUtils.defaultString(newSku.getName(), "-")).append("\n");
        md.append("- 规格：").append(StringUtils.defaultString(newSku.getSpec(), "-")).append("\n");
        md.append("- 生产批次：").append(StringUtils.defaultString(newSku.getProductionBatch(), "-")).append("\n");
        md.append("- 官方价：").append(newSku.getOfficialPrice() == null ? "-" : newSku.getOfficialPrice()).append(" 元\n\n");
        md.append("**价格变动**\n");
        if (!equalsBigDecimal(prevSalePrice, newSku.getSalePrice())) {
            boolean saleUp = compareMoney(newSku.getSalePrice(), prevSalePrice) > 0;
            md.append("- 售价：").append(feishuPriceTrendTag(saleUp)).append(" ")
                    .append(formatSalePriceWithDiscount(prevSalePrice, prevOfficialPrice))
                    .append(" → ")
                    .append(formatSalePriceWithDiscount(newSku.getSalePrice(), newSku.getOfficialPrice()))
                    .append("\n");
        } else {
            md.append("- 售价：")
                    .append(formatSalePriceWithDiscount(newSku.getSalePrice(), newSku.getOfficialPrice()))
                    .append("\n");
        }
        if (!equalsBigDecimal(prevRecyclePrice, newSku.getRecyclePrice())) {
            boolean recycleUp = compareMoney(newSku.getRecyclePrice(), prevRecyclePrice) > 0;
            md.append("- 回收价：").append(feishuPriceTrendTag(recycleUp)).append(" ")
                    .append(formatMoneyYuan(prevRecyclePrice))
                    .append(" → ")
                    .append(formatMoneyYuan(newSku.getRecyclePrice()))
                    .append("\n");
        } else {
            md.append("- 回收价：").append(formatMoneyYuan(newSku.getRecyclePrice())).append("\n");
        }
        return md.toString();
    }

    // 飞书交互卡片 Markdown 彩色标签（参见开放平台文档 text_tag）
    private static String feishuPriceTrendTag(boolean up) {
        return up ? "<text_tag color='red'>涨价</text_tag>" : "<text_tag color='green'>降价</text_tag>";
    }

    private String resolveTeaBrandName(Integer brand) {
        if (brand == null) {
            return "-";
        }
        String name = DictUtils.keyValue("TEA_BRAND", String.valueOf(brand));
        return StringUtils.isNotBlank(name) ? name : String.valueOf(brand);
    }

    // 售价：金额 + 相对官方价的折扣（折），口径与列表页一致（售价×10÷官方价）
    private String formatSalePriceWithDiscount(BigDecimal salePrice, BigDecimal officialPrice) {
        if (salePrice == null) {
            return "-";
        }
        String yuan = salePrice.stripTrailingZeros().toPlainString() + "元";
        if (officialPrice != null && officialPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = salePrice.multiply(BigDecimal.TEN)
                    .divide(officialPrice, 2, RoundingMode.HALF_UP);
            yuan += "（" + discount.stripTrailingZeros().toPlainString() + "折）";
        }
        return yuan;
    }

    private static String formatMoneyYuan(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return amount.stripTrailingZeros().toPlainString() + "元";
    }

    private static int compareMoney(BigDecimal newer, BigDecimal older) {
        BigDecimal n = newer == null ? BigDecimal.ZERO : newer;
        BigDecimal o = older == null ? BigDecimal.ZERO : older;
        return n.compareTo(o);
    }
}