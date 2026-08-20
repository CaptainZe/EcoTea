package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiFormHelper;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiExpirationService;
import com.appsinnova.admin.business.service.chai.ChaiSkuService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import com.appsinnova.admin.business.vo.chai.ChaiSkuBatchSaveVo;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/business/chai/sku")
@RequiredArgsConstructor
public class ChaiSkuController {

    private final ChaiSkuService chaiSkuService;
    private final ChaiSpuService chaiSpuService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiExpirationService chaiExpirationService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:sku:index")
    public String index(Model model, ChaiSku queryParam, HttpServletRequest request) {
        if (queryParam == null) {
            queryParam = new ChaiSku();
        }
        String deletedParam = request.getParameter("deleted");
        if (deletedParam == null) {
            queryParam.setDeleted(0);
        } else if ("-1".equals(deletedParam)) {
            queryParam.setDeleted(null);
        }
        // 按父 SPU 编码筛选 → 转为 spuId（含已删除 SPU）
        if (StringUtils.hasText(queryParam.getQuerySpuCode())) {
            ChaiSpu parent = chaiSpuService.getBySpuCodeIncludeDeleted(queryParam.getQuerySpuCode());
            if (parent == null) {
                queryParam.setSpuId(-1L);
            } else {
                queryParam.setSpuId(parent.getId());
            }
        }

        Page<ChaiSku> page = chaiSkuService.getPageList(queryParam);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        Map<Long, String> expirationNameMap = buildExpirationNameMap();
        page.forEach(item -> fillShowFields(item, brandNameMap, expirationNameMap));

        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("yearOptions", ChaiFormHelper.buildYearOptions());
        return "/business/chai/sku/index";
    }

    /**
     * 只读：某 SPU 下 SKU 列表（弹窗，不锁定主列表搜索）
     */
    @GetMapping("/listBySpu/{spuId}")
    @RequiresPermissions("business:chai:sku:index")
    public String listBySpu(@PathVariable("spuId") Long spuId, HttpServletRequest request, Model model) {
        ChaiSpu spu = chaiSpuService.getById(spuId);
        if (spu == null) {
            model.addAttribute("errorMsg", "SPU不存在");
            return "/business/chai/sku/listBySpu";
        }
        String deletedParam = request.getParameter("deleted");
        Integer deleted = 0;
        if ("-1".equals(deletedParam)) {
            deleted = null;
        } else if ("1".equals(deletedParam)) {
            deleted = 1;
        }
        List<ChaiSku> list = chaiSkuService.listBySpuId(spuId, deleted);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        Map<Long, String> expirationNameMap = buildExpirationNameMap();
        list.forEach(item -> fillShowFields(item, brandNameMap, expirationNameMap));
        model.addAttribute("spu", spu);
        model.addAttribute("list", list);
        return "/business/chai/sku/listBySpu";
    }

    /**
     * 按 SPU 维护 SKU：无数据则按锚点预填 6 条；已有则加载已有
     */
    @GetMapping("/editBySpu/{spuId}")
    @RequiresPermissions("business:chai:sku:edit")
    public String editBySpu(@PathVariable("spuId") Long spuId, Model model) {
        ChaiSpu spu = chaiSpuService.getById(spuId);
        if (spu == null) {
            model.addAttribute("errorMsg", "SPU不存在");
            return "/business/chai/sku/editBySpu";
        }
        if (Integer.valueOf(1).equals(spu.getDeleted())) {
            model.addAttribute("errorMsg", "该SPU已删除，请先在列表中恢复后再维护SKU");
            return "/business/chai/sku/editBySpu";
        }
        ChaiSpecUtil.fillSpecFields(spu);
        if (spu.getBrand() != null) {
            ChaiBrand brand = chaiBrandService.getById(spu.getBrand());
            spu.setBrandName(brand != null ? brand.getName() : String.valueOf(spu.getBrand()));
        }
        if (spu.getExpiration() != null) {
            ChaiExpiration expiration = chaiExpirationService.getById(spu.getExpiration());
            spu.setExpirationName(expiration != null ? expiration.getName() : String.valueOf(spu.getExpiration()));
        }

        List<ChaiSku> skuList = chaiSkuService.listBySpuId(spuId);
        boolean generated = false;
        if (CollectionUtils.isEmpty(skuList)) {
            skuList = chaiSkuService.buildDefaultSkuList(spu);
            generated = true;
        } else {
            skuList.forEach(ChaiSpecUtil::fillSpecFields);
        }

        // 当前向导卡片是否含锚点半年（含未落库预填）；无 year/batch 时不提示
        boolean anchorSkuMissing = false;
        if (spu.getYear() != null && spu.getProdBatch() != null) {
            boolean hasAnchorInList = skuList.stream().anyMatch(s ->
                    spu.getYear().equals(s.getYear()) && spu.getProdBatch().equals(s.getProdBatch()));
            // 已落库时以库为准更稳；预填未保存则以列表为准
            if (generated) {
                anchorSkuMissing = !hasAnchorInList;
            } else {
                anchorSkuMissing = !chaiSkuService.existsActiveBySpuAndHalfYear(
                        spuId, spu.getYear(), spu.getProdBatch());
            }
        }

        model.addAttribute("spu", spu);
        model.addAttribute("skuList", skuList);
        model.addAttribute("skuListJson", ChaiFormHelper.toCamelJson(skuList));
        model.addAttribute("generated", generated);
        model.addAttribute("anchorSkuMissing", anchorSkuMissing);
        model.addAttribute("yearOptions", ChaiFormHelper.buildYearOptions());
        model.addAttribute("prodBatchDictJson", dictJson("CHAI_PROD_BATCH"));
        model.addAttribute("starLevelDictJson", dictJson("STAR_LEVEL"));
        model.addAttribute("gradeDictJson", dictJson("CHAI_GRADE"));
        model.addAttribute("specLabelDictJson", dictJson("CHAI_SPEC_LABEL"));
        return "/business/chai/sku/editBySpu";
    }

    @PostMapping("/saveBatch")
    @RequiresPermissions("business:chai:sku:edit")
    @ResponseBody
    public ResultVo<?> saveBatch(@RequestBody ChaiSkuBatchSaveVo saveVo) {
        if (saveVo == null || saveVo.getSpuId() == null) {
            return ResultVoUtil.error("SPU不能为空");
        }
        ChaiSpu spu = chaiSpuService.getById(saveVo.getSpuId());
        if (spu == null) {
            return ResultVoUtil.error("SPU不存在");
        }
        if (Integer.valueOf(1).equals(spu.getDeleted())) {
            return ResultVoUtil.error("已删除的SPU不能维护SKU，请先恢复");
        }

        User user = ShiroUtil.getSubject();
        boolean hasSku;
        try {
            chaiSkuService.saveBatchForSpu(spu, saveVo.getItemList(), user.getNickname());
            hasSku = chaiSkuService.countBySpuId(spu.getId()) > 0;
            if (!hasSku) {
                if (Boolean.TRUE.equals(saveVo.getOnlineSpu())) {
                    return ResultVoUtil.error("尚无SKU，不能上架");
                }
                if (!ChaiStatus.OFFLINE.getCode().equals(spu.getStatus())) {
                    spu.setStatus(ChaiStatus.OFFLINE.getCode());
                    spu.setOperator(user.getNickname());
                    chaiSpuService.save(spu);
                }
            } else if (Boolean.TRUE.equals(saveVo.getOnlineSpu())
                    && !ChaiStatus.ONLINE.getCode().equals(spu.getStatus())) {
                spu.setStatus(ChaiStatus.ONLINE.getCode());
                spu.setOperator(user.getNickname());
                chaiSpuService.save(spu);
                chaiSkuService.syncStatusFromSpu(spu.getId(), spu.getStatus(), user.getNickname());
            }
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("spuId", spu.getId());
        data.put("spuStatus", spu.getStatus());
        data.put("needOnlineConfirm", hasSku && ChaiStatus.OFFLINE.getCode().equals(spu.getStatus()));
        return ResultVoUtil.success("保存成功", data);
    }

    /**
     * 提供给前端「添加一行」的 SPU 模板数据。
     * blankHalfYear=true：年份/生产批次留空（普通添加）；默认带 SPU 锚点（添加锚点半年）。
     */
    @GetMapping("/templateFromSpu/{spuId}")
    @RequiresPermissions("business:chai:sku:edit")
    @ResponseBody
    public ResultVo<?> templateFromSpu(@PathVariable("spuId") Long spuId,
                                       @RequestParam(value = "blankHalfYear", required = false) Boolean blankHalfYear) {
        ChaiSpu spu = chaiSpuService.getById(spuId);
        if (spu == null) {
            return ResultVoUtil.error("SPU不存在");
        }
        if (Integer.valueOf(1).equals(spu.getDeleted())) {
            return ResultVoUtil.error("已删除的SPU不能维护SKU，请先恢复");
        }
        ChaiSpecUtil.fillSpecFields(spu);
        Integer year = Boolean.TRUE.equals(blankHalfYear) ? null : spu.getYear();
        Integer prodBatch = Boolean.TRUE.equals(blankHalfYear) ? null : spu.getProdBatch();
        ChaiSku sku = chaiSkuService.copyFromSpu(spu, year, prodBatch);
        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        return ResultVoUtil.success(data);
    }

    private void fillShowFields(ChaiSku item,
                                Map<Long, String> brandNameMap,
                                Map<Long, String> expirationNameMap) {
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
        if (item.getExpiration() != null) {
            String expirationName = expirationNameMap.get(item.getExpiration());
            if (expirationName == null) {
                ChaiExpiration expiration = chaiExpirationService.getById(item.getExpiration());
                expirationName = expiration != null ? expiration.getName() : String.valueOf(item.getExpiration());
                expirationNameMap.put(item.getExpiration(), expirationName);
            }
            item.setExpirationName(expirationName);
        } else {
            item.setExpirationName("-");
        }
        Map<String, Object> map = ChaiSpecUtil.parseSpec(item.getSpec());
        BigDecimal total = ChaiSpecUtil.getDecimal(map, "total_net_weight");
        BigDecimal unitWeight = ChaiSpecUtil.getDecimal(map, "unit_weight");
        Integer unitCount = ChaiSpecUtil.getInt(map, "unit_count");
        Integer unitLabel = ChaiSpecUtil.getInt(map, "unit_label");
        String labelText = unitLabel == null ? "" : DictUtils.keyValue("CHAI_SPEC_LABEL", String.valueOf(unitLabel));
        item.setSpecShow(ChaiSpecUtil.formatShow(total, unitWeight, unitCount, labelText));
        item.setShowImageList(ChaiFormHelper.parseUrlList(item.getShowImageUrls()));
        item.setRealImageList(ChaiFormHelper.parseUrlList(item.getRealImageUrls()));
        ChaiPriceUtil.fillListShow(item);
    }

    private Map<Long, String> buildBrandNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ChaiBrand brand : chaiBrandService.listOnlineOrdered()) {
            map.put(brand.getId(), brand.getName());
        }
        return map;
    }

    private Map<Long, String> buildExpirationNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ChaiExpiration item : chaiExpirationService.listOnlineOrdered()) {
            map.put(item.getId(), item.getName());
        }
        return map;
    }

    private String dictJson(String dictName) {
        Map<String, String> map = DictUtils.value(dictName);
        return JsonUtils.writeValueAsString(map != null ? map : new HashMap<>());
    }
}
