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
    public String index(Model model, ChaiSku queryParam) {
        if (queryParam == null) {
            queryParam = new ChaiSku();
        }
        // 按父 SPU 编码筛选 → 转为 spuId
        if (StringUtils.hasText(queryParam.getQuerySpuCode())) {
            ChaiSpu parent = chaiSpuService.getBySpuCode(queryParam.getQuerySpuCode());
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

        model.addAttribute("spu", spu);
        model.addAttribute("skuList", skuList);
        model.addAttribute("skuListJson", ChaiFormHelper.toCamelJson(skuList));
        model.addAttribute("generated", generated);
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
        if (CollectionUtils.isEmpty(saveVo.getItemList())) {
            return ResultVoUtil.error("请至少保留一条SKU");
        }

        User user = ShiroUtil.getSubject();
        try {
            // 可选：保存后直接上架 SPU（并级联 SKU）
            if (Boolean.TRUE.equals(saveVo.getOnlineSpu())
                    && !ChaiStatus.ONLINE.getCode().equals(spu.getStatus())) {
                spu.setStatus(ChaiStatus.ONLINE.getCode());
                spu.setOperator(user.getNickname());
                chaiSpuService.save(spu);
            }
            chaiSkuService.saveBatchForSpu(spu, saveVo.getItemList(), user.getNickname());
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("spuId", spu.getId());
        data.put("spuStatus", spu.getStatus());
        boolean needOnlineConfirm = ChaiStatus.OFFLINE.getCode().equals(spu.getStatus());
        data.put("needOnlineConfirm", needOnlineConfirm);
        return ResultVoUtil.success("保存成功", data);
    }

    /**
     * 提供给前端「添加一行」的 SPU 模板数据
     */
    @GetMapping("/templateFromSpu/{spuId}")
    @RequiresPermissions("business:chai:sku:edit")
    @ResponseBody
    public ResultVo<?> templateFromSpu(@PathVariable("spuId") Long spuId) {
        ChaiSpu spu = chaiSpuService.getById(spuId);
        if (spu == null) {
            return ResultVoUtil.error("SPU不存在");
        }
        ChaiSpecUtil.fillSpecFields(spu);
        ChaiSku sku = chaiSkuService.copyFromSpu(spu, spu.getYear(), spu.getProdBatch());
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
