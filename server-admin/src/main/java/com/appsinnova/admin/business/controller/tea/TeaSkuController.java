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
        if (saveItem.getId() != null) {
            TeaSku oldEntity = teaSkuService.getById(saveItem.getId());
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
        teaSkuService.save(saveItem);
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

    // 价格变更通知
    @RequestMapping("/notifyPriceChange")
    @RequiresPermissions("business:tea:teaSku:edit")
    @ResponseBody
    public ResultVo<?> notifyPriceChange(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        if (ids.size() != 1) {
            return ResultVoUtil.error("价格变更通知仅支持单条SKU发送");
        }

        AppSecretKey appSecretKey = appSecretKeyService.getSecretKey(AppSecretKeyType.TEA_ROBOT_PRICE_CHANGE.getCode());
        if (appSecretKey == null || StringUtils.isBlank(appSecretKey.getAccessSecret())) {
            return ResultVoUtil.error("未配置茶类价格变动通知机器人webhookUrl");
        }

        TeaSku item = teaSkuService.getById(ids.get(0));
        if (item == null) {
            return ResultVoUtil.error("未找到可通知的SKU记录");
        }

        String brandName = "-";
        if (item.getBrand() != null) {
            brandName = DictUtils.keyValue("TEA_BRAND", String.valueOf(item.getBrand()));
            if (StringUtils.isBlank(brandName)) {
                brandName = String.valueOf(item.getBrand());
            }
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append("**商品基础信息**\n");
        markdown.append("- 品牌：").append(brandName).append("\n");
        markdown.append("- 商品名称：").append(StringUtils.defaultString(item.getName(), "-")).append("\n");
        markdown.append("- 规格：").append(StringUtils.defaultString(item.getSpec(), "-")).append("\n");
        markdown.append("- 生产批次：").append(StringUtils.defaultString(item.getProductionBatch(), "-")).append("\n\n");
        markdown.append("**价格信息**\n");
        markdown.append("- 官方价：").append(item.getOfficialPrice() == null ? "-" : item.getOfficialPrice()).append("\n");
        markdown.append("- 销售价：").append(item.getSalePrice() == null ? "-" : item.getSalePrice()).append("\n");
        markdown.append("- 回收价：").append(item.getRecyclePrice() == null ? "-" : item.getRecyclePrice()).append("\n");

        final String webhookUrl = appSecretKey.getAccessSecret();
        final String markdownContent = markdown.toString();

        // 异步发送飞书消息，避免阻塞页面请求
        CompletableFuture.runAsync(() ->
                feiShuWebhookService.sendMarkdownCard(
                        webhookUrl,
                        "茶类价格变更通知",
                        markdownContent
                )
        );
        return ResultVoUtil.success("价格变更通知已提交发送");
    }
}