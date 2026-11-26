package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.base.YesOrNo;
import com.appsinnova.admin.business.common.enums.chai.ChaiBarcodeKind;
import com.appsinnova.admin.business.common.enums.chai.ChaiDeletedFilter;
import com.appsinnova.admin.business.common.enums.chai.ChaiProdBatch;
import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiBarcodeUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiFormHelper;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSearchTextUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.domain.chai.ChaiSpuBarcode;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiExpirationService;
import com.appsinnova.admin.business.service.chai.ChaiSkuService;
import com.appsinnova.admin.business.service.chai.ChaiSpuBarcodeService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/business/chai/spu")
@RequiredArgsConstructor
public class ChaiSpuController {

    private final ChaiSpuService chaiSpuService;
    private final ChaiSpuBarcodeService chaiSpuBarcodeService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiExpirationService chaiExpirationService;
    private final ChaiSkuService chaiSkuService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:spu:index")
    public String index(Model model, ChaiSpu queryParam, HttpServletRequest request) {
        if (queryParam == null) {
            queryParam = new ChaiSpu();
        }
        queryParam.setDeleted(ChaiDeletedFilter.parseQueryDeleted(request.getParameter("deleted")));
        Page<ChaiSpu> page = chaiSpuService.getPageList(queryParam);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        Map<Long, String> expirationNameMap = buildExpirationNameMap();
        page.forEach(item -> {
            fillShowFields(item, brandNameMap, expirationNameMap);
            ChaiPriceUtil.fillSpuListShow(item);
            item.setSkuCount(chaiSkuService.countBySpuId(item.getId()));
        });
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("yearOptions", ChaiFormHelper.buildYearOptions());
        model.addAttribute("deletedFilterOptions", ChaiDeletedFilter.values());
        model.addAttribute("barcodeKindOptions", ChaiBarcodeKind.values());
        return "/business/chai/spu/index";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    @RequiresPermissions("business:chai:spu:index")
    public String toEdit(@PathVariable(value = "id", required = false) ChaiSpu editItem, Model model) {
        if (editItem == null) {
            editItem = new ChaiSpu();
            editItem.setStarLevel(5);
            editItem.setStatus(ChaiStatus.OFFLINE.getCode());
            editItem.setProdBatch(ChaiProdBatch.FIRST_HALF.getCode());
            editItem.setGrade(0);
            editItem.setNonSale(0);
            editItem.setOfficialPrice(BigDecimal.ONE);
            editItem.setHideExternal(0);
            editItem.setCoreFlag(0);
            editItem.setTastingFlag(0);
            editItem.setPackSpuId(null);
        } else {
            ChaiSpecUtil.fillSpecFields(editItem);
            if (YesOrNo.isYes(editItem.getDeleted())) {
                model.addAttribute("errorMsg", "该SPU已删除，请先在列表中恢复后再编辑");
                return "/business/chai/spu/edit";
            }
        }
        // 尚无 SKU 时仅允许下架
        if (editItem.getId() == null || chaiSkuService.countBySpuId(editItem.getId()) == 0) {
            editItem.setStatus(ChaiStatus.OFFLINE.getCode());
        }
        fillEditModel(editItem, model, editItem.getId() != null && chaiSkuService.countBySpuId(editItem.getId()) > 0);
        return "/business/chai/spu/edit";
    }

    @GetMapping("/copy/{id}")
    @RequiresPermissions("business:chai:spu:edit")
    public String copy(@PathVariable("id") Long id, Model model) {
        ChaiSpu source = chaiSpuService.getById(id);
        if (source == null) {
            model.addAttribute("errorMsg", "SPU不存在");
            return "/business/chai/spu/edit";
        }
        if (YesOrNo.isYes(source.getDeleted())) {
            model.addAttribute("errorMsg", "已删除的SPU不能复制，请先恢复");
            return "/business/chai/spu/edit";
        }
        ChaiSpu editItem = chaiSpuService.copyForEdit(id);
        ChaiSpecUtil.fillSpecFields(editItem);
        fillEditModel(editItem, model, false);
        return "/business/chai/spu/edit";
    }

    /**
     * 整装 SPU 弹窗选择（编辑页标记区）；未删、非品鉴；可排除当前；支持名称/编码/条码。
     */
    @GetMapping("/packPick")
    @RequiresPermissions("business:chai:spu:index")
    public String packPick(Model model, ChaiSpu queryParam,
                           @RequestParam(value = "excludeId", required = false) Long excludeId) {
        if (queryParam == null) {
            queryParam = new ChaiSpu();
        }
        queryParam.setDeleted(0);
        queryParam.setTastingFlag(0);
        queryParam.setExcludeId(excludeId);
        Page<ChaiSpu> page = chaiSpuService.getPageList(queryParam);
        Map<Long, String> brandNameMap = buildBrandNameMap();
        Map<Long, String> expirationNameMap = buildExpirationNameMap();
        page.forEach(item -> {
            fillShowFields(item, brandNameMap, expirationNameMap);
            ChaiPriceUtil.fillSpuListShow(item);
        });
        model.addAttribute("list", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("excludeId", excludeId);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        return "/business/chai/spu/packPick";
    }

    /**
     * 整装 SPU 远程选项（兼容）；排除自己与品鉴款。
     */
    @GetMapping("/packOptions")
    @RequiresPermissions("business:chai:spu:index")
    @ResponseBody
    public ResultVo<?> packOptions(@RequestParam(value = "q", required = false) String q,
                                   @RequestParam(value = "excludeId", required = false) Long excludeId,
                                   @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        List<Map<String, Object>> list = chaiSpuService.listPackOptions(q, excludeId, limit);
        return ResultVoUtil.success(list != null ? list : new ArrayList<>());
    }

    /**
     * 独立维护搜索关键词（chip）；SKU 继承 SPU。
     */
    @GetMapping("/keywords/{id}")
    @RequiresPermissions("business:chai:spu:index")
    public String toKeywords(@PathVariable("id") Long id, Model model) {
        ChaiSpu spu = chaiSpuService.getById(id);
        if (spu == null) {
            model.addAttribute("errorMsg", "SPU不存在");
            return "/business/chai/spu/keywords";
        }
        if (YesOrNo.isYes(spu.getDeleted())) {
            model.addAttribute("errorMsg", "该SPU已删除，请先在列表中恢复后再维护关键词");
            return "/business/chai/spu/keywords";
        }
        if (spu.getBrand() != null) {
            ChaiBrand brand = chaiBrandService.getById(spu.getBrand());
            spu.setBrandName(brand != null ? brand.getName() : String.valueOf(spu.getBrand()));
        } else {
            spu.setBrandName("-");
        }
        if (spu.getExpiration() != null) {
            ChaiExpiration expiration = chaiExpirationService.getById(spu.getExpiration());
            spu.setExpirationName(expiration != null ? expiration.getName() : String.valueOf(spu.getExpiration()));
        } else {
            spu.setExpirationName("-");
        }
        spu.setSpecShow(ChaiSpecUtil.toShow(spu.getSpec()));
        model.addAttribute("editItem", spu);
        model.addAttribute("maxKeywordCount", ChaiSearchTextUtil.MAX_KEYWORD_COUNT);
        model.addAttribute("maxKeywordLength", ChaiSearchTextUtil.MAX_KEYWORD_LENGTH);
        return "/business/chai/spu/keywords";
    }

    @PostMapping("/keywords/save")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> saveKeywords(@RequestParam("id") Long id,
                                    @RequestParam(value = "keywords", required = false) String keywords) {
        User user = ShiroUtil.getSubject();
        try {
            chaiSpuService.saveKeywords(id, keywords, user.getNickname());
            return ResultVoUtil.SAVE_SUCCESS;
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    /**
     * 独立维护条形码 + 标签预览/打印（同一页）。
     */
    @GetMapping("/barcode/{id}")
    @RequiresPermissions("business:chai:spu:index")
    public String toBarcode(@PathVariable("id") Long id, Model model) {
        ChaiSpu spu = chaiSpuService.getById(id);
        if (spu == null) {
            model.addAttribute("errorMsg", "SPU不存在");
            return "/business/chai/spu/barcode";
        }
        if (YesOrNo.isYes(spu.getDeleted())) {
            model.addAttribute("errorMsg", "该SPU已删除，请先在列表中恢复后再维护条码");
            return "/business/chai/spu/barcode";
        }
        if (spu.getBrand() != null) {
            ChaiBrand brand = chaiBrandService.getById(spu.getBrand());
            spu.setBrandName(brand != null ? brand.getName() : String.valueOf(spu.getBrand()));
        } else {
            spu.setBrandName("-");
        }
        if (spu.getExpiration() != null) {
            ChaiExpiration expiration = chaiExpirationService.getById(spu.getExpiration());
            spu.setExpirationName(expiration != null ? expiration.getName() : String.valueOf(spu.getExpiration()));
        } else {
            spu.setExpirationName("-");
        }
        spu.setSpecShow(ChaiSpecUtil.toShow(spu.getSpec()));
        ChaiPriceUtil.fillSpuListShow(spu);
        List<ChaiSpuBarcode> barcodeList = chaiSpuBarcodeService.listBySpuId(id);
        String previewCode = pickPreviewBarcode(barcodeList);
        model.addAttribute("editItem", spu);
        model.addAttribute("barcodeList", barcodeList);
        model.addAttribute("previewCode", previewCode);
        model.addAttribute("legacyBarcode", StringUtils.isNotBlank(spu.getBarcode()) ? spu.getBarcode().trim() : "");
        model.addAttribute("gradeName", DictUtils.keyValue("CHAI_GRADE",
                spu.getGrade() != null ? String.valueOf(spu.getGrade()) : null));
        model.addAttribute("prodBatchName", DictUtils.keyValue("CHAI_PROD_BATCH",
                spu.getProdBatch() != null ? String.valueOf(spu.getProdBatch()) : null));
        return "/business/chai/spu/barcode";
    }

    /** 新增一条条码（多码） */
    @PostMapping("/barcode/add")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> addBarcode(@RequestParam("id") Long id,
                                  @RequestParam(value = "barcode", required = false) String barcode) {
        User user = ShiroUtil.getSubject();
        try {
            ChaiSpuBarcode row = chaiSpuBarcodeService.add(id, barcode, user.getNickname());
            Map<String, Object> data = new HashMap<>();
            data.put("id", row.getId());
            data.put("barcode", row.getBarcode());
            return ResultVoUtil.success("添加成功", data);
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    /** 删除一条条码 */
    @PostMapping("/barcode/delete")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> deleteBarcode(@RequestParam("id") Long id,
                                     @RequestParam("barcodeId") Long barcodeId) {
        try {
            chaiSpuBarcodeService.delete(id, barcodeId);
            return ResultVoUtil.success("已删除");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    /**
     * 生成 29 系统码并落库；已有 29* 则返回已有记录。
     */
    @PostMapping("/barcode/generate/{id}")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> generateBarcode(@PathVariable("id") Long id) {
        User user = ShiroUtil.getSubject();
        try {
            ChaiSpuBarcode row = chaiSpuBarcodeService.generateAndAdd(id, user.getNickname());
            Map<String, Object> data = new HashMap<>();
            data.put("id", row.getId());
            data.put("barcode", row.getBarcode());
            return ResultVoUtil.success("生成成功", data);
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    /** EAN-13 条码 PNG（预览/打印用；SPU 维护页与运营工具箱共用） */
    @GetMapping("/barcode/image")
    @RequiresPermissions(value = {"business:chai:spu:index", "business:chai:toolbox:index"},
            logical = Logical.OR)
    public void barcodeImage(@RequestParam("code") String code,
                             @RequestParam(value = "w", required = false, defaultValue = "280") int width,
                             @RequestParam(value = "h", required = false, defaultValue = "80") int height,
                             HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader("Cache-Control", "no-store");
        try {
            int w = Math.min(Math.max(width, 120), 600);
            int h = Math.min(Math.max(height, 40), 200);
            BufferedImage image = ChaiBarcodeUtil.toImage(code, w, h);
            ImageIO.write(image, "png", response.getOutputStream());
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @PostMapping("/save")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> save(ChaiSpu saveItem,
                            @RequestParam(value = "confirmDuplicate", required = false, defaultValue = "false")
                            boolean confirmDuplicate) {
        Integer oldYear = null;
        Integer oldProdBatch = null;
        boolean isCreate = saveItem.getId() == null;
        if (saveItem.getId() != null) {
            ChaiSpu oldEntity = chaiSpuService.getById(saveItem.getId());
            if (oldEntity == null) {
                return ResultVoUtil.error("编辑的数据记录不存在");
            }
            saveItem.setId(oldEntity.getId());
            saveItem.setSpuCode(oldEntity.getSpuCode());
            saveItem.setCreateTime(oldEntity.getCreateTime());
            saveItem.setDeleted(oldEntity.getDeleted());
            if (YesOrNo.isYes(oldEntity.getDeleted())) {
                return ResultVoUtil.error("已删除的SPU不能编辑，请先恢复");
            }
            oldYear = oldEntity.getYear();
            oldProdBatch = oldEntity.getProdBatch();
        }

        if (StringUtils.isBlank(saveItem.getName())) {
            return ResultVoUtil.error("商品名称必填");
        }
        saveItem.setName(saveItem.getName().trim());
        if (saveItem.getStarLevel() == null) {
            return ResultVoUtil.error("星级必选");
        }
        if (saveItem.getBrand() == null) {
            return ResultVoUtil.error("品牌必选");
        }
        ChaiBrand brand = chaiBrandService.getById(saveItem.getBrand());
        if (brand == null) {
            return ResultVoUtil.error("品牌不存在");
        }
        if (saveItem.getExpiration() == null) {
            return ResultVoUtil.error("保质期必选");
        }
        if (chaiExpirationService.getById(saveItem.getExpiration()) == null) {
            return ResultVoUtil.error("保质期不存在");
        }
        if (saveItem.getType() == null) {
            return ResultVoUtil.error("茶类必选");
        }
        if (saveItem.getGrade() == null) {
            return ResultVoUtil.error("等级必选");
        }
        if (saveItem.getYear() == null) {
            return ResultVoUtil.error("年份必填");
        }
        if (saveItem.getProdBatch() == null) {
            return ResultVoUtil.error("生产批次必选");
        }
        if (saveItem.getNonSale() == null) {
            saveItem.setNonSale(0);
        }
        if (ChaiPriceUtil.isNonSale(saveItem.getNonSale())) {
            saveItem.setNonSale(1);
            saveItem.setOfficialPrice(null);
        } else {
            saveItem.setNonSale(0);
            if (saveItem.getOfficialPrice() == null) {
                return ResultVoUtil.error("官方价必填");
            }
            if (saveItem.getOfficialPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return ResultVoUtil.error("官方价须大于0");
            }
        }
        if (saveItem.getTotalNetWeight() == null || saveItem.getTotalNetWeight().compareTo(BigDecimal.ZERO) <= 0) {
            return ResultVoUtil.error("总净重必填且大于0");
        }
        if (saveItem.getUnitWeight() == null || saveItem.getUnitWeight().compareTo(BigDecimal.ZERO) <= 0) {
            return ResultVoUtil.error("单份重量必填且大于0");
        }
        if (saveItem.getUnitCount() == null || saveItem.getUnitCount() <= 0) {
            return ResultVoUtil.error("份数必填且大于0");
        }
        if (saveItem.getUnitLabel() == null) {
            return ResultVoUtil.error("规格单位必选");
        }
        if (StringUtils.isBlank(saveItem.getShowImageUrls())) {
            saveItem.setShowImageUrls("[]");
        }

        // 同品牌同名：未确认时返回 409，前端二次确认后带 confirmDuplicate=true 再存
        if (!confirmDuplicate
                && chaiSpuService.isNameTakenByOtherInBrand(saveItem.getBrand(), saveItem.getName(), saveItem.getId())) {
            return ResultVoUtil.error(409,
                    "同品牌下已存在同名商品「" + saveItem.getName() + "」，是否仍要继续保存？");
        }

        saveItem.setSpec(ChaiSpecUtil.buildSpecJson(
                saveItem.getTotalNetWeight(),
                saveItem.getUnitWeight(),
                saveItem.getUnitCount(),
                saveItem.getUnitLabel()));
        // 尚无 SKU 时状态强制下架
        boolean noSku = saveItem.getId() == null || chaiSkuService.countBySpuId(saveItem.getId()) == 0;
        if (noSku) {
            saveItem.setStatus(ChaiStatus.OFFLINE.getCode());
        } else if (saveItem.getStatus() == null) {
            saveItem.setStatus(ChaiStatus.OFFLINE.getCode());
        }
        if (StringUtils.isBlank(saveItem.getRealImageUrls())) {
            saveItem.setRealImageUrls("[]");
        }

        User user = ShiroUtil.getSubject();
        saveItem.setOperator(user.getNickname());
        ChaiSpu saved;
        try {
            saved = chaiSpuService.save(saveItem);
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
        // SPU 状态变更时级联全部 SKU
        if (saved.getId() != null && saved.getStatus() != null && !noSku) {
            chaiSkuService.syncStatusFromSpu(saved.getId(), saved.getStatus(), user.getNickname());
        }

        boolean anchorChanged = isCreate
                || !java.util.Objects.equals(oldYear, saved.getYear())
                || !java.util.Objects.equals(oldProdBatch, saved.getProdBatch());
        boolean needSkuForAnchor = false;
        if (anchorChanged && saved.getYear() != null && saved.getProdBatch() != null) {
            needSkuForAnchor = !chaiSkuService.existsActiveBySpuAndHalfYear(
                    saved.getId(), saved.getYear(), saved.getProdBatch());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("spuId", saved.getId());
        data.put("anchorChanged", anchorChanged);
        data.put("needSkuForAnchor", needSkuForAnchor);
        return ResultVoUtil.success("保存成功", data);
    }

    @RequestMapping("/delete")
    @RequiresPermissions("business:chai:spu:delete")
    @ResponseBody
    public ResultVo<?> toDelete(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        User user = ShiroUtil.getSubject();
        try {
            chaiSpuService.softDeleteByIdIn(ids, user.getNickname());
            return ResultVoUtil.success("删除成功");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    @RequestMapping("/restore")
    @RequiresPermissions("business:chai:spu:delete")
    @ResponseBody
    public ResultVo<?> restore(@RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        User user = ShiroUtil.getSubject();
        chaiSpuService.restoreByIdIn(ids, user.getNickname());
        return ResultVoUtil.success("恢复成功");
    }

    /**
     * 列表「操作」批量改标记位；field=hideExternal|coreFlag|tastingFlag，value=0|1。
     * 取消品鉴时服务端清空 pack_spu_id 并同步 SKU。
     */
    @RequestMapping("/marks/{field}/{value}")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> batchMarks(
            @PathVariable("field") String field,
            @PathVariable("value") Integer value,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        if (value == null || (value != 0 && value != 1)) {
            return ResultVoUtil.error("标记值无效");
        }
        Integer hideExternal = null;
        Integer coreFlag = null;
        Integer tastingFlag = null;
        if ("hideExternal".equals(field)) {
            hideExternal = value;
        } else if ("coreFlag".equals(field)) {
            coreFlag = value;
        } else if ("tastingFlag".equals(field)) {
            tastingFlag = value;
        } else {
            return ResultVoUtil.error("标记类型无效");
        }
        User user = ShiroUtil.getSubject();
        try {
            chaiSpuService.batchUpdateMarks(ids, hideExternal, coreFlag, tastingFlag, user.getNickname());
            return ResultVoUtil.success("操作成功");
        } catch (IllegalArgumentException ex) {
            return ResultVoUtil.error(ex.getMessage());
        }
    }

    /**
     * SPU 上下架：无 SKU 不可上架；成功后级联全部 SKU 状态
     */
    @RequestMapping("/status/{param}")
    @RequiresPermissions("business:chai:spu:edit")
    @ResponseBody
    public ResultVo<?> status(
            @PathVariable("param") Integer status,
            @RequestParam(value = "ids", required = false) List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultVoUtil.error("请选择一条记录");
        }
        if (!ChaiStatus.ONLINE.getCode().equals(status) && !ChaiStatus.OFFLINE.getCode().equals(status)) {
            return ResultVoUtil.error("状态参数无效");
        }
        User user = ShiroUtil.getSubject();
        for (Long id : ids) {
            ChaiSpu entity = chaiSpuService.getById(id);
            if (entity == null) {
                continue;
            }
            if (YesOrNo.isYes(entity.getDeleted())) {
                return ResultVoUtil.error("SPU「" + entity.getName() + "」已删除，请先恢复");
            }
            if (ChaiStatus.ONLINE.getCode().equals(status) && chaiSkuService.countBySpuId(id) == 0) {
                return ResultVoUtil.error("SPU「" + entity.getName() + "」尚无SKU，不能上架");
            }
            if (!status.equals(entity.getStatus())) {
                entity.setStatus(status);
                entity.setOperator(user.getNickname());
                chaiSpuService.save(entity);
            }
            chaiSkuService.syncStatusFromSpu(id, status, user.getNickname());
        }
        return ResultVoUtil.success("操作成功");
    }

    private void fillEditModel(ChaiSpu editItem, Model model, boolean hasSku) {
        if (editItem.getHideExternal() == null) {
            editItem.setHideExternal(0);
        }
        if (editItem.getCoreFlag() == null) {
            editItem.setCoreFlag(0);
        }
        if (editItem.getTastingFlag() == null) {
            editItem.setTastingFlag(0);
        }
        if (!YesOrNo.isYes(editItem.getTastingFlag())) {
            editItem.setPackSpuId(null);
        }
        String packSpuLabel = "";
        if (editItem.getPackSpuId() != null) {
            ChaiSpu pack = chaiSpuService.getById(editItem.getPackSpuId());
            if (pack != null) {
                packSpuLabel = chaiSpuService.formatPackLabel(pack);
            }
        }
        model.addAttribute("editItem", editItem);
        model.addAttribute("hasSku", hasSku);
        model.addAttribute("packSpuLabel", packSpuLabel);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("expirationList", chaiExpirationService.listOnlineOrdered());
        model.addAttribute("yearOptions", ChaiFormHelper.buildYearOptions());
        model.addAttribute("maxKeywordCount", ChaiSearchTextUtil.MAX_KEYWORD_COUNT);
        model.addAttribute("maxKeywordLength", ChaiSearchTextUtil.MAX_KEYWORD_LENGTH);
    }

    private void fillShowFields(ChaiSpu item, Map<Long, String> brandNameMap, Map<Long, String> expirationNameMap) {
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
        item.setSpecShow(ChaiSpecUtil.toShow(item.getSpec()));
        item.setShowImageList(ChaiFormHelper.parseUrlList(item.getShowImageUrls()));
        item.setRealImageList(ChaiFormHelper.parseUrlList(item.getRealImageUrls()));
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

    /** 预览优先唯一 29 系统码，否则取第一条。 */
    private static String pickPreviewBarcode(List<ChaiSpuBarcode> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        for (ChaiSpuBarcode row : list) {
            if (ChaiBarcodeUtil.isSystem29(row.getBarcode())) {
                return row.getBarcode();
            }
        }
        return list.get(0).getBarcode();
    }
}
