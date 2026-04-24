package com.appsinnova.admin.business.controller.tea;

import com.appsinnova.admin.business.common.enums.AppNoticeType;
import com.appsinnova.admin.business.common.enums.SkuStatus;
import com.appsinnova.admin.business.domain.sys.AppNotice;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.service.sys.AppNoticeService;
import com.appsinnova.admin.business.service.tea.TeaSkuService;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequestMapping("/business/tea/teaQuotation")
@RequiredArgsConstructor
public class TeaQuotationController {

    private final TeaSkuService teaSkuService;
    private final AppNoticeService appNoticeService;

    @GetMapping("/index")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String index(Model model, TeaSku queryParam) {
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
        return "/business/tea/teaQuotation/index";
    }

    @GetMapping("/readme")
    @RequiresPermissions("business:tea:teaQuotation:index")
    public String readme(Model model) {
        AppNotice teaNotice = appNoticeService.getByType(AppNoticeType.TEA_NOTICE.getCode());
        model.addAttribute("teaNotice", teaNotice);
        return "/business/tea/teaQuotation/readme";
    }
}
