package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.base.YesOrNo;
import com.appsinnova.admin.business.common.enums.chai.ChaiDeletedFilter;
import com.appsinnova.admin.business.common.utils.RequestParamUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.domain.chai.ChaiStock;
import com.appsinnova.admin.business.domain.chai.ChaiStockWh;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiSkuService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import com.appsinnova.admin.business.service.chai.ChaiStockService;
import com.appsinnova.admin.business.service.chai.ChaiWarehouseService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/business/chai/stock")
@RequiredArgsConstructor
public class ChaiStockController {

    private final ChaiStockService chaiStockService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiSpuService chaiSpuService;
    private final ChaiSkuService chaiSkuService;
    private final ChaiWarehouseService chaiWarehouseService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:stock:index")
    public String index(Model model, ChaiStock queryParam, HttpServletRequest request) {
        if (queryParam == null) {
            queryParam = new ChaiStock();
        }
        queryParam.setDeleted(ChaiDeletedFilter.parseQueryDeleted(request.getParameter("deleted")));
        queryParam.setQueryHasQty(YesOrNo.parseQueryCode(request.getParameter("queryHasQty")));
        queryParam.setQueryWhId(RequestParamUtil.parseLong(request.getParameter("queryWhId")));

        if (StringUtils.hasText(queryParam.getQuerySpuCode())) {
            ChaiSpu parent = chaiSpuService.getBySpuCodeIncludeDeleted(queryParam.getQuerySpuCode().trim());
            if (parent == null) {
                queryParam.setSpuId(-1L);
            } else {
                queryParam.setSpuId(parent.getId());
            }
        }

        Page<ChaiStock> page = chaiStockService.getPageList(queryParam);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        Map<Long, String> spuCodeMap = buildSpuCodeMap(page.getContent());
        page.forEach(item -> chaiStockService.fillSkuShow(item, brandNameMap, spuCodeMap));

        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("warehouseList", chaiWarehouseService.listOnlineOrdered());
        model.addAttribute("deletedFilterOptions", ChaiDeletedFilter.values());
        return "/business/chai/stock/index";
    }

    /**
     * 分仓结存弹窗
     */
    @GetMapping("/wh/{stockId}")
    @RequiresPermissions("business:chai:stock:index")
    public String listWh(@PathVariable("stockId") Long stockId, Model model) {
        ChaiStock stock = chaiStockService.getById(stockId);
        if (stock == null) {
            model.addAttribute("errorMsg", "库存记录不存在");
            return "/business/chai/stock/wh";
        }
        ChaiSku sku = chaiSkuService.getById(stock.getSkuId());
        stock.setSku(sku);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        Map<Long, String> spuCodeMap = new HashMap<>();
        if (sku != null && sku.getSpuId() != null) {
            List<ChaiSpu> spus = chaiSpuService.getByIdIn(
                    new ArrayList<>(Collections.singletonList(sku.getSpuId())));
            if (!spus.isEmpty()) {
                spuCodeMap.put(spus.get(0).getId(), spus.get(0).getSpuCode());
            }
        }
        chaiStockService.fillSkuShow(stock, brandNameMap, spuCodeMap);

        List<ChaiStockWh> whList = chaiStockService.listWhByStockId(stockId);
        model.addAttribute("stock", stock);
        model.addAttribute("whList", whList);
        return "/business/chai/stock/wh";
    }

    private Map<Long, String> buildBrandNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ChaiBrand brand : chaiBrandService.listOnlineOrdered()) {
            map.put(brand.getId(), brand.getName());
        }
        return map;
    }

    private Map<Long, String> buildSpuCodeMap(List<ChaiStock> stocks) {
        Set<Long> spuIds = new HashSet<>();
        for (ChaiStock stock : stocks) {
            if (stock.getSku() != null && stock.getSku().getSpuId() != null) {
                spuIds.add(stock.getSku().getSpuId());
            }
        }
        Map<Long, String> map = new HashMap<>();
        if (spuIds.isEmpty()) {
            return map;
        }
        for (ChaiSpu spu : chaiSpuService.getByIdIn(new ArrayList<>(spuIds))) {
            map.put(spu.getId(), spu.getSpuCode());
        }
        return map;
    }
}
