package com.appsinnova.admin.business.controller.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiProdBatch;
import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiFormHelper;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiExpirationService;
import com.appsinnova.admin.business.service.chai.ChaiSkuService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import com.appsinnova.admin.common.utils.DictUtils;
import com.appsinnova.admin.common.utils.ResultVoUtil;
import com.appsinnova.admin.common.vo.ResultVo;
import com.appsinnova.admin.component.shiro.ShiroUtil;
import com.appsinnova.admin.system.domain.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/business/chai/spu")
@RequiredArgsConstructor
public class ChaiSpuController {

    private final ChaiSpuService chaiSpuService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiExpirationService chaiExpirationService;
    private final ChaiSkuService chaiSkuService;

    @GetMapping("/index")
    @RequiresPermissions("business:chai:spu:index")
    public String index(Model model, ChaiSpu queryParam, HttpServletRequest request) {
        if (queryParam == null) {
            queryParam = new ChaiSpu();
        }
        String deletedParam = request.getParameter("deleted");
        if (deletedParam == null) {
            queryParam.setDeleted(0);
        } else if ("-1".equals(deletedParam)) {
            queryParam.setDeleted(null);
        }
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
            editItem.setOfficialPrice(BigDecimal.ONE);
        } else {
            ChaiSpecUtil.fillSpecFields(editItem);
            if (Integer.valueOf(1).equals(editItem.getDeleted())) {
                model.addAttribute("errorMsg", "该SPU已删除，请先在列表中恢复后再编辑");
                return "/business/chai/spu/edit";
            }
        }
        // 尚无 SKU 时仅允许下架
        if (editItem.getId() == null || chaiSkuService.countBySpuId(editItem.getId()) == 0) {
            editItem.setStatus(ChaiStatus.OFFLINE.getCode());
        }
        model.addAttribute("editItem", editItem);
        model.addAttribute("hasSku", editItem.getId() != null && chaiSkuService.countBySpuId(editItem.getId()) > 0);
        model.addAttribute("brandList", chaiBrandService.listOnlineOrdered());
        model.addAttribute("expirationList", chaiExpirationService.listOnlineOrdered());
        model.addAttribute("yearOptions", ChaiFormHelper.buildYearOptions());
        return "/business/chai/spu/edit";
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
            if (Integer.valueOf(1).equals(oldEntity.getDeleted())) {
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
        if (saveItem.getOfficialPrice() == null) {
            return ResultVoUtil.error("官方价必填");
        }
        if (saveItem.getOfficialPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ResultVoUtil.error("官方价不能为负数");
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
        ChaiSpu saved = chaiSpuService.save(saveItem);
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
        chaiSpuService.softDeleteByIdIn(ids, user.getNickname());
        return ResultVoUtil.success("删除成功");
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
            if (Integer.valueOf(1).equals(entity.getDeleted())) {
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
        Map<String, Object> map = ChaiSpecUtil.parseSpec(item.getSpec());
        BigDecimal total = ChaiSpecUtil.getDecimal(map, "total_net_weight");
        BigDecimal unitWeight = ChaiSpecUtil.getDecimal(map, "unit_weight");
        Integer unitCount = ChaiSpecUtil.getInt(map, "unit_count");
        Integer unitLabel = ChaiSpecUtil.getInt(map, "unit_label");
        String labelText = unitLabel == null ? "" : DictUtils.keyValue("CHAI_SPEC_LABEL", String.valueOf(unitLabel));
        item.setSpecShow(ChaiSpecUtil.formatShow(total, unitWeight, unitCount, labelText));
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
}
