package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiExpirationService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 茶业运营工具箱（内部运营用）。与开发刷库 {@code /business/tools/chai} 分离。
 */
@Controller
@RequestMapping("/business/chai/toolbox")
@RequiredArgsConstructor
public class ChaiToolboxController {

    private final ChaiSpuService chaiSpuService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiExpirationService chaiExpirationService;

    @GetMapping({"/index", "", "/"})
    @RequiresPermissions("business:chai:toolbox:index")
    public String index(Model model) {
        Map<String, Long> kinds = chaiSpuService.countBarcodeKinds();
        model.addAttribute("internalCount", kinds.getOrDefault("internal", 0L));
        model.addAttribute("nationalCount", kinds.getOrDefault("national", 0L));
        model.addAttribute("emptyCount", kinds.getOrDefault("empty", 0L));
        model.addAttribute("brandStats", chaiSpuService.countInternalBarcodeByBrand());
        model.addAttribute("maxLabelPrint", ChaiSpuService.MAX_LABEL_PRINT);
        return "/business/chai/toolbox/index";
    }

    /**
     * 批量打印系统码标签（新窗口打开）。{@code brandIds} 为空或不传 = 全部系统码。
     */
    @GetMapping("/barcode/print")
    @RequiresPermissions("business:chai:toolbox:index")
    public String barcodePrint(@RequestParam(value = "brandIds", required = false) List<Long> brandIds,
                               Model model) {
        try {
            List<ChaiSpu> list = chaiSpuService.listInternalBarcodeForPrint(brandIds);
            if (list.isEmpty()) {
                model.addAttribute("errorMsg", "没有可打印的系统码（29 开头），请先在 SPU「条码」中生成");
                return "/business/chai/toolbox/barcodePrint";
            }
            Map<Long, String> brandNameMap = new HashMap<>();
            Map<Long, String> expirationNameMap = new HashMap<>();
            for (ChaiSpu spu : list) {
                fillLabelShow(spu, brandNameMap, expirationNameMap);
            }
            model.addAttribute("list", list);
            model.addAttribute("printCount", list.size());
            return "/business/chai/toolbox/barcodePrint";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            return "/business/chai/toolbox/barcodePrint";
        }
    }

    private void fillLabelShow(ChaiSpu spu, Map<Long, String> brandNameMap,
                               Map<Long, String> expirationNameMap) {
        if (spu.getBrand() != null) {
            String brandName = brandNameMap.get(spu.getBrand());
            if (brandName == null) {
                ChaiBrand brand = chaiBrandService.getById(spu.getBrand());
                brandName = brand != null && StringUtils.hasText(brand.getName())
                        ? brand.getName() : String.valueOf(spu.getBrand());
                brandNameMap.put(spu.getBrand(), brandName);
            }
            spu.setBrandName(brandName);
        } else {
            spu.setBrandName("-");
        }
        if (spu.getExpiration() != null) {
            String expirationName = expirationNameMap.get(spu.getExpiration());
            if (expirationName == null) {
                ChaiExpiration expiration = chaiExpirationService.getById(spu.getExpiration());
                expirationName = expiration != null && StringUtils.hasText(expiration.getName())
                        ? expiration.getName() : String.valueOf(spu.getExpiration());
                expirationNameMap.put(spu.getExpiration(), expirationName);
            }
            spu.setExpirationName(expirationName);
        } else {
            spu.setExpirationName("-");
        }
        spu.setSpecShow(ChaiSpecUtil.toShow(spu.getSpec()));
        ChaiPriceUtil.fillSpuListShow(spu);
    }
}
