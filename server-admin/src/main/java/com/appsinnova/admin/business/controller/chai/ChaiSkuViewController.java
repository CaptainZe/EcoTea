package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiSkuViewType;
import com.appsinnova.admin.business.common.utils.chai.ChaiFormHelper;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiSkuViewService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/business/chai/skuView")
@RequiredArgsConstructor
public class ChaiSkuViewController {

    private final ChaiSkuViewService chaiSkuViewService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiSpuService chaiSpuService;

    @GetMapping("/internal")
    @RequiresPermissions("business:chai:skuView:internal")
    public String internal(Model model, ChaiSku queryParam) {
        return renderList(model, queryParam, ChaiSkuViewType.INTERNAL, "/business/chai/skuView/internal");
    }

    @GetMapping("/sales")
    @RequiresPermissions("business:chai:skuView:sales")
    public String sales(Model model, ChaiSku queryParam) {
        return renderList(model, queryParam, ChaiSkuViewType.SALES, "/business/chai/skuView/sales");
    }

    @GetMapping("/recycle")
    @RequiresPermissions("business:chai:skuView:recycle")
    public String recycle(Model model, ChaiSku queryParam) {
        return renderList(model, queryParam, ChaiSkuViewType.RECYCLE, "/business/chai/skuView/recycle");
    }

    @GetMapping("/internal/sameSpu/{spuId}")
    @RequiresPermissions("business:chai:skuView:internal")
    public String sameSpuInternal(@PathVariable("spuId") Long spuId, Model model) {
        return renderSameSpu(model, spuId, ChaiSkuViewType.INTERNAL);
    }

    @GetMapping("/sales/sameSpu/{spuId}")
    @RequiresPermissions("business:chai:skuView:sales")
    public String sameSpuSales(@PathVariable("spuId") Long spuId, Model model) {
        return renderSameSpu(model, spuId, ChaiSkuViewType.SALES);
    }

    @GetMapping("/recycle/sameSpu/{spuId}")
    @RequiresPermissions("business:chai:skuView:recycle")
    public String sameSpuRecycle(@PathVariable("spuId") Long spuId, Model model) {
        return renderSameSpu(model, spuId, ChaiSkuViewType.RECYCLE);
    }

    private String renderList(Model model, ChaiSku queryParam, ChaiSkuViewType viewType, String template) {
        if (queryParam == null) {
            queryParam = new ChaiSku();
        }
        Page<ChaiSku> page = chaiSkuViewService.getPageList(queryParam, viewType);
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("viewTitle", viewType.getTitle());
        model.addAttribute("recentHalfYearOnly", viewType.isRecentHalfYearOnly());
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("yearOptions", ChaiFormHelper.buildYearOptions());
        return template;
    }

    private String renderSameSpu(Model model, Long spuId, ChaiSkuViewType viewType) {
        ChaiSpu spu = chaiSpuService.getById(spuId);
        if (spu == null) {
            model.addAttribute("errorMsg", "商品不存在");
            model.addAttribute("list", java.util.Collections.emptyList());
        } else {
            List<ChaiSku> list = chaiSkuViewService.listSameSpu(spuId);
            model.addAttribute("spu", spu);
            model.addAttribute("list", list);
        }
        model.addAttribute("viewTitle", viewType.getTitle());
        model.addAttribute("showSalePrice", viewType != ChaiSkuViewType.RECYCLE);
        model.addAttribute("showRecyclePrice", viewType != ChaiSkuViewType.SALES);
        model.addAttribute("showRealImage", viewType != ChaiSkuViewType.SALES);
        return "/business/chai/skuView/sameSpu";
    }
}
