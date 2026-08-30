package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiDeletedFilter;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillStatus;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockBillType;
import com.appsinnova.admin.business.common.enums.chai.ChaiStockReason;
import com.appsinnova.admin.business.common.utils.chai.ChaiFormHelper;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.*;
import com.appsinnova.admin.business.service.chai.*;
import com.appsinnova.admin.business.vo.chai.ChaiStockBillSaveVo;
import com.appsinnova.admin.common.data.URL;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.JsonUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/business/chai/stockBill")
@RequiredArgsConstructor
public class ChaiStockBillController {

    private final ChaiStockBillService chaiStockBillService;
    private final ChaiWarehouseService chaiWarehouseService;
    private final ChaiStaffService chaiStaffService;
    private final ChaiSkuService chaiSkuService;
    private final ChaiSpuService chaiSpuService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiStockService chaiStockService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:stockBill:index")
    public String index(Model model, ChaiStockBill queryParam) {
        if (queryParam == null) {
            queryParam = new ChaiStockBill();
        }
        Page<ChaiStockBill> page = chaiStockBillService.getPageList(queryParam);
        Map<Long, String> whNameMap = buildWhNameMap(page.getContent());
        page.forEach(bill -> fillBillShow(bill, whNameMap));
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("warehouseList", chaiWarehouseService.listOnlineOrdered());
        model.addAttribute("staffList", chaiStaffService.listOnlineOrdered());
        model.addAttribute("billTypeOptions", ChaiStockBillType.values());
        model.addAttribute("statusOptions", ChaiStockBillStatus.values());
        model.addAttribute("reasonOptions", ChaiStockReason.values());
        return "/business/chai/stockBill/index";
    }

    @GetMapping("/create")
    @RequiresPermissions("business:chai:stockBill:edit")
    public String create(@RequestParam(value = "billType", defaultValue = "1") Integer billType, Model model) {
        ChaiStockBillType type = ChaiStockBillType.fromCode(billType);
        if (type == null) {
            type = ChaiStockBillType.IN;
        }
        model.addAttribute("billType", type.getCode());
        model.addAttribute("billTypeName", type.getMessage());
        model.addAttribute("warehouseList", chaiWarehouseService.listOnlineOrdered());
        model.addAttribute("staffList", chaiStaffService.listOnlineOrdered());
        model.addAttribute("reasonOptions", filterReasons(type));
        return "/business/chai/stockBill/create";
    }

    /**
     * iframe 选品：勾选多行后由父页一次添加；带分页。
     */
    @GetMapping("/skuPick")
    @RequiresPermissions("business:chai:stockBill:edit")
    public String skuPick(Model model, ChaiSku queryParam,
                          @RequestParam(value = "billType", defaultValue = "1") Integer billType,
                          javax.servlet.http.HttpServletRequest request) {
        if (queryParam == null) {
            queryParam = new ChaiSku();
        }
        ChaiStockBillType type = ChaiStockBillType.fromCode(billType);
        if (type == null) {
            type = ChaiStockBillType.IN;
            billType = type.getCode();
        }
        boolean inbound = type == ChaiStockBillType.IN;
        if (inbound) {
            queryParam.setDeleted(0);
        } else {
            queryParam.setDeleted(ChaiDeletedFilter.parseQueryDeleted(request.getParameter("deleted")));
        }
        if (StringUtils.hasText(queryParam.getQuerySpuCode())) {
            ChaiSpu parent = chaiSpuService.getBySpuCodeIncludeDeleted(queryParam.getQuerySpuCode().trim());
            if (parent == null) {
                queryParam.setSpuId(-1L);
            } else {
                queryParam.setSpuId(parent.getId());
            }
        }
        Page<ChaiSku> page = chaiSkuService.getPageList(queryParam);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        page.forEach(item -> fillSkuShowFields(item, brandNameMap));
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("billType", billType);
        model.addAttribute("inbound", inbound);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("deletedFilterOptions", ChaiDeletedFilter.values());
        return "/business/chai/stockBill/skuPick";
    }

    /**
     * 父页根据勾选 id 拉选品数据（顺序按 ids）
     */
    @GetMapping("/skuByIds")
    @RequiresPermissions("business:chai:stockBill:edit")
    @ResponseBody
    public ResultVo<?> skuByIds(@RequestParam("ids") String ids) {
        List<Long> idList = parseIdList(ids);
        if (idList.isEmpty()) {
            return ResultVoUtil.success(new ArrayList<>());
        }
        Map<Long, ChaiSku> byId = new HashMap<>();
        for (ChaiSku sku : chaiSkuService.findByIds(idList)) {
            byId.put(sku.getId(), sku);
        }
        Map<Long, String> brandNameMap = buildBrandNameMap();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long id : idList) {
            ChaiSku sku = byId.get(id);
            if (sku == null) {
                continue;
            }
            fillSkuShowFields(sku, brandNameMap);
            rows.add(toPickRow(sku));
        }
        return ResultVoUtil.success(rows);
    }

    /**
     * 明细行刷新分仓结存：入/出看 fromWh；调拨同时返回调出/调入。
     */
    @GetMapping("/whQty")
    @RequiresPermissions("business:chai:stockBill:edit")
    @ResponseBody
    public ResultVo<?> whQty(@RequestParam("skuIds") String skuIds,
                             @RequestParam(value = "fromWhId", required = false) Long fromWhId,
                             @RequestParam(value = "toWhId", required = false) Long toWhId) {
        List<Long> idList = parseIdList(skuIds);
        Map<String, Object> data = new HashMap<>();
        data.put("fromQtyMap", chaiStockService.mapWhQtyBySkuIds(idList, fromWhId));
        data.put("toQtyMap", chaiStockService.mapWhQtyBySkuIds(idList, toWhId));
        data.put("fromStockMap", chaiStockService.mapWhStockBySkuIds(idList, fromWhId));
        data.put("toStockMap", chaiStockService.mapWhStockBySkuIds(idList, toWhId));
        return ResultVoUtil.success(data);
    }

    @GetMapping("/detail/{id}")
    @RequiresPermissions("business:chai:stockBill:index")
    public String detail(@PathVariable("id") Long id, Model model) {
        ChaiStockBill bill = chaiStockBillService.getById(id);
        if (bill == null) {
            model.addAttribute("errorMsg", "单据不存在");
            return "/business/chai/stockBill/detail";
        }
        List<ChaiStockBill> one = new ArrayList<>();
        one.add(bill);
        fillBillShow(bill, buildWhNameMap(one));
        List<ChaiStockBillItem> items = chaiStockBillService.listItems(id);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        items.forEach(item -> fillItemShow(item, brandNameMap));
        model.addAttribute("bill", bill);
        model.addAttribute("itemList", items);
        model.addAttribute("billTypeName", messageOfType(bill.getBillType()));
        model.addAttribute("statusName", messageOfStatus(bill.getStatus()));
        model.addAttribute("reasonName", messageOfReason(bill.getReason()));
        model.addAttribute("canOperate", ChaiStockBillStatus.POSTED.getCode().equals(bill.getStatus()));
        return "/business/chai/stockBill/detail";
    }

    @PostMapping("/save")
    @RequiresPermissions("business:chai:stockBill:edit")
    @ResponseBody
    public ResultVo<?> save(@RequestBody ChaiStockBillSaveVo saveVo) {
        User user = ShiroUtil.getSubject();
        try {
            ChaiStockBill bill = chaiStockBillService.post(saveVo, user.getNickname());
            // 返回完整详情 URL，避免前端拼接失败后落到列表
            return ResultVoUtil.success("过账成功",
                    new URL("/business/chai/stockBill/detail/" + bill.getId()));
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @PostMapping("/void")
    @RequiresPermissions("business:chai:stockBill:edit")
    @ResponseBody
    public ResultVo<?> voidBill(@RequestParam("id") Long id) {
        User user = ShiroUtil.getSubject();
        try {
            chaiStockBillService.voidBill(id, user.getNickname());
            return ResultVoUtil.success("作废成功");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @PostMapping("/archive")
    @RequiresPermissions("business:chai:stockBill:edit")
    @ResponseBody
    public ResultVo<?> archive(@RequestParam("id") Long id) {
        User user = ShiroUtil.getSubject();
        try {
            chaiStockBillService.archiveBill(id, user.getNickname());
            return ResultVoUtil.success("归档成功");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    private List<ChaiStockReason> filterReasons(ChaiStockBillType type) {
        List<ChaiStockReason> list = new ArrayList<>();
        for (ChaiStockReason reason : ChaiStockReason.values()) {
            if (reason.getBillType() == type) {
                list.add(reason);
            }
        }
        return list;
    }

    private Map<String, Object> toPickRow(ChaiSku sku) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", sku.getId());
        row.put("skuCode", sku.getSkuCode());
        row.put("name", sku.getName());
        row.put("brandName", sku.getBrandName());
        row.put("year", sku.getYear());
        row.put("prodBatch", sku.getProdBatch());
        row.put("prodBatchName", sku.getProdBatch() == null ? ""
                : DictUtils.keyValue("CHAI_PROD_BATCH", String.valueOf(sku.getProdBatch())));
        row.put("specShow", sku.getSpecShow());
        row.put("showImageUrls", sku.getShowImageUrls());
        row.put("deleted", sku.getDeleted());
        row.put("salePrice", sku.getSalePrice());
        row.put("recyclePrice", sku.getRecyclePrice());
        row.put("salePriceShow", sku.getSalePriceShow());
        row.put("recyclePriceShow", sku.getRecyclePriceShow());
        return row;
    }

    private void fillSkuShowFields(ChaiSku item, Map<Long, String> brandNameMap) {
        if (item.getBrand() != null) {
            String brandName = brandNameMap.get(item.getBrand());
            if (brandName == null) {
                ChaiBrand brand = chaiBrandService.getById(item.getBrand());
                brandName = brand != null ? brand.getName() : String.valueOf(item.getBrand());
                brandNameMap.put(item.getBrand(), brandName);
            }
            item.setBrandName(brandName);
        } else {
            item.setBrandName("-");
        }
        item.setSpecShow(ChaiSpecUtil.toShow(item.getSpec()));
        item.setShowImageList(ChaiFormHelper.parseUrlList(item.getShowImageUrls()));
        ChaiPriceUtil.fillListShow(item);
    }

    private Map<Long, String> buildBrandNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ChaiBrand brand : chaiBrandService.listOnlineOrdered()) {
            map.put(brand.getId(), brand.getName());
        }
        return map;
    }

    private static List<Long> parseIdList(String ids) {
        List<Long> list = new ArrayList<>();
        if (!StringUtils.hasText(ids)) {
            return list;
        }
        for (String part : ids.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                list.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return list;
    }

    private void fillItemShow(ChaiStockBillItem item, Map<Long, String> brandNameMap) {
        if (item == null) {
            return;
        }
        if (!StringUtils.hasText(item.getSkuSnap())) {
            item.setBrandName("-");
            item.setSpecShow("-");
            item.setHalfYearShow("-");
            item.setShowImageList(Collections.emptyList());
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> snap = JsonUtils.readValue(item.getSkuSnap(), Map.class);
            if (snap == null) {
                item.setBrandName("-");
                item.setSpecShow("-");
                item.setHalfYearShow("-");
                item.setShowImageList(Collections.emptyList());
                return;
            }
            Object brandObj = snap.get("brand");
            Long brandId = null;
            if (brandObj instanceof Number) {
                brandId = ((Number) brandObj).longValue();
            }
            if (brandId != null) {
                String brandName = brandNameMap.get(brandId);
                if (brandName == null) {
                    ChaiBrand brand = chaiBrandService.getById(brandId);
                    brandName = brand != null ? brand.getName() : String.valueOf(brandId);
                    brandNameMap.put(brandId, brandName);
                }
                item.setBrandName(brandName);
            } else {
                item.setBrandName("-");
            }
            Object yearObj = snap.get("year");
            if (yearObj instanceof Number) {
                item.setYear(((Number) yearObj).intValue());
            }
            Object batchObj = snap.get("prod_batch");
            if (batchObj instanceof Number) {
                item.setProdBatch(((Number) batchObj).intValue());
                item.setProdBatchName(DictUtils.keyValue("CHAI_PROD_BATCH", String.valueOf(item.getProdBatch())));
            }
            String y = item.getYear() != null ? String.valueOf(item.getYear()) : "";
            String b = item.getProdBatchName() != null ? item.getProdBatchName() : "";
            if (!y.isEmpty() && !b.isEmpty()) {
                item.setHalfYearShow(y + " / " + b);
            } else if (!y.isEmpty() || !b.isEmpty()) {
                item.setHalfYearShow(y + b);
            } else {
                item.setHalfYearShow("-");
            }
            Object specObj = snap.get("spec");
            String specJson = specObj != null ? String.valueOf(specObj) : null;
            String specShow = ChaiSpecUtil.toShow(specJson);
            item.setSpecShow(StringUtils.hasText(specShow) ? specShow : "-");
            Object showObj = snap.get("show_image_urls");
            item.setShowImageList(limitShowImages(parseShowImageUrls(showObj)));
        } catch (Exception ex) {
            item.setBrandName("-");
            item.setSpecShow("-");
            item.setHalfYearShow("-");
            item.setShowImageList(Collections.emptyList());
        }
    }

    private static List<String> limitShowImages(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        return urls.size() <= 3 ? urls : urls.subList(0, 3);
    }

    private static List<String> parseShowImageUrls(Object showObj) {
        if (showObj == null) {
            return Collections.emptyList();
        }
        if (showObj instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object item : (List<?>) showObj) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    list.add(String.valueOf(item));
                }
            }
            return list;
        }
        return ChaiFormHelper.parseUrlList(String.valueOf(showObj));
    }

    private void fillBillShow(ChaiStockBill bill, Map<Long, String> whNameMap) {
        if (bill.getFromWhId() != null && bill.getFromWhId() > 0) {
            bill.setFromWhName(whNameMap.getOrDefault(bill.getFromWhId(), String.valueOf(bill.getFromWhId())));
        }
        if (bill.getToWhId() != null && bill.getToWhId() > 0) {
            bill.setToWhName(whNameMap.getOrDefault(bill.getToWhId(), String.valueOf(bill.getToWhId())));
        }
    }

    private Map<Long, String> buildWhNameMap(List<ChaiStockBill> bills) {
        Map<Long, String> map = new HashMap<>();
        for (ChaiWarehouse wh : chaiWarehouseService.listOnlineOrdered()) {
            map.put(wh.getId(), wh.getName());
        }
        if (bills == null) {
            return map;
        }
        Set<Long> need = new HashSet<>();
        for (ChaiStockBill bill : bills) {
            if (bill.getFromWhId() != null && bill.getFromWhId() > 0 && !map.containsKey(bill.getFromWhId())) {
                need.add(bill.getFromWhId());
            }
            if (bill.getToWhId() != null && bill.getToWhId() > 0 && !map.containsKey(bill.getToWhId())) {
                need.add(bill.getToWhId());
            }
        }
        for (Long whId : need) {
            ChaiWarehouse wh = chaiWarehouseService.getById(whId);
            if (wh != null) {
                map.put(whId, wh.getName());
            }
        }
        return map;
    }

    private static String messageOfType(Integer code) {
        ChaiStockBillType type = ChaiStockBillType.fromCode(code);
        return type != null ? type.getMessage() : String.valueOf(code);
    }

    private static String messageOfStatus(Integer code) {
        ChaiStockBillStatus status = ChaiStockBillStatus.fromCode(code);
        return status != null ? status.getMessage() : String.valueOf(code);
    }

    private static String messageOfReason(Integer code) {
        ChaiStockReason reason = ChaiStockReason.fromCode(code);
        return reason != null ? reason.getMessage() : String.valueOf(code);
    }
}
