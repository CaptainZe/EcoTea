package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.base.YesOrNo;
import com.appsinnova.admin.business.common.enums.chai.ChaiBarcodeKind;
import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiBarcodeUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiCodeUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSearchTextUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.domain.chai.ChaiSpuBarcode;
import com.appsinnova.admin.business.repository.chai.ChaiBrandRepository;
import com.appsinnova.admin.business.repository.chai.ChaiSpuBarcodeRepository;
import com.appsinnova.admin.business.repository.chai.ChaiSpuRepository;
import com.appsinnova.admin.common.data.PageSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChaiSpuService {

    /** 运营工具箱批量打标签单次上限 */
    public static final int MAX_LABEL_PRINT = 300;

    private final ChaiSpuRepository chaiSpuRepository;
    private final ChaiSpuBarcodeRepository chaiSpuBarcodeRepository;
    private final ChaiSkuService chaiSkuService;
    private final ChaiStockService chaiStockService;
    private final ChaiBrandRepository chaiBrandRepository;
    private final ChaiSpuBarcodeService chaiSpuBarcodeService;

    public ChaiSpu getById(Long id) {
        return chaiSpuRepository.findById(id).orElse(null);
    }

    /**
     * 复制 SPU 为新建草稿（不含 id/编码/时间；不复制 SKU）
     */
    public ChaiSpu copyForEdit(Long id) {
        ChaiSpu source = getById(id);
        if (source == null) {
            return null;
        }
        ChaiSpu copy = new ChaiSpu();
        copy.setStarLevel(source.getStarLevel());
        copy.setName(source.getName());
        copy.setKeywords(source.getKeywords());
        copy.setBrand(source.getBrand());
        copy.setExpiration(source.getExpiration());
        copy.setType(source.getType());
        copy.setGrade(source.getGrade());
        copy.setYear(source.getYear());
        copy.setProdBatch(source.getProdBatch());
        copy.setSpec(source.getSpec());
        copy.setShowImageUrls(source.getShowImageUrls());
        copy.setRealImageUrls(source.getRealImageUrls());
        copy.setNonSale(source.getNonSale() != null ? source.getNonSale() : 0);
        copy.setOfficialPrice(ChaiPriceUtil.isNonSale(copy.getNonSale()) ? null : source.getOfficialPrice());
        copy.setHideExternal(source.getHideExternal() != null ? source.getHideExternal() : 0);
        copy.setCoreFlag(source.getCoreFlag() != null ? source.getCoreFlag() : 0);
        copy.setTastingFlag(source.getTastingFlag() != null ? source.getTastingFlag() : 0);
        copy.setPackSpuId(YesOrNo.isYes(copy.getTastingFlag()) ? source.getPackSpuId() : null);
        copy.setStatus(ChaiStatus.OFFLINE.getCode());
        copy.setDeleted(0);
        return copy;
    }

    public ChaiSpu getBySpuCode(String spuCode) {
        if (!StringUtils.hasText(spuCode)) {
            return null;
        }
        return chaiSpuRepository.findFirstBySpuCodeAndDeleted(spuCode.trim(), 0).orElse(null);
    }

    /** 列表按编码找父 SPU 时包含已删除 */
    public ChaiSpu getBySpuCodeIncludeDeleted(String spuCode) {
        if (!StringUtils.hasText(spuCode)) {
            return null;
        }
        return chaiSpuRepository.findFirstBySpuCode(spuCode.trim()).orElse(null);
    }

    public List<ChaiSpu> getByIdIn(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return new ArrayList<>();
        }
        return chaiSpuRepository.findByIdIn(idList);
    }

    /**
     * 未删 SPU 条码分类（读新表）：internal=有 29…；national=有 69…（可与系统码并存）；empty=新表无任何码。
     */
    public Map<String, Long> countBarcodeKinds() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("internal", chaiSpuBarcodeRepository.countDistinctSpuIdByBarcodeLike(
                ChaiBarcodeUtil.INTERNAL_PREFIX + "%"));
        map.put("national", chaiSpuBarcodeRepository.countDistinctSpuIdByBarcodeLike("69%"));
        long totalActive = chaiSpuRepository.countByDeleted(0);
        long withAny = chaiSpuBarcodeRepository.findDistinctSpuIds().size();
        // empty：未删且新表无码；粗算 = 未删总数 − 有码且未删（有码含已删 SPU 时略偏，再精确过滤）
        Set<Long> activeWithCode = new HashSet<>();
        if (withAny > 0) {
            List<Long> ids = chaiSpuBarcodeRepository.findDistinctSpuIds();
            for (ChaiSpu spu : chaiSpuRepository.findByIdIn(ids)) {
                if (spu.getDeleted() == null || spu.getDeleted() == 0) {
                    activeWithCode.add(spu.getId());
                }
            }
        }
        map.put("empty", Math.max(0L, totalActive - activeWithCode.size()));
        return map;
    }

    /**
     * 系统码按品牌统计（新表 29*；仅未删 SPU）。每项：brandId / brandName / count。
     */
    public List<Map<String, Object>> countInternalBarcodeByBrand() {
        List<Object[]> rows = chaiSpuBarcodeRepository.countGroupByBrandAndBarcodeLike(
                0, ChaiBarcodeUtil.INTERNAL_PREFIX + "%");
        Map<Long, String> brandNames = loadBrandNameMap();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row[0] == null) {
                continue;
            }
            Long brandId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("brandId", brandId);
            item.put("brandName", brandNames.getOrDefault(brandId, String.valueOf(brandId)));
            item.put("count", count);
            list.add(item);
        }
        list.sort(Comparator
                .comparing((Map<String, Object> m) -> String.valueOf(m.get("brandName")),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(m -> (Long) m.get("brandId")));
        return list;
    }

    /**
     * 批量打印用：未删 SPU 且新表有 29*；可选品牌过滤；按品牌名→品名→id 排序。
     * 将打印码写入实体 {@code barcode} 字段供模板使用（不落库）。
     * 超过 {@link #MAX_LABEL_PRINT} 抛错。
     */
    public List<ChaiSpu> listInternalBarcodeForPrint(List<Long> brandIds) {
        final List<Long> filterBrands = brandIds == null ? null : brandIds;
        List<ChaiSpuBarcode> rows = chaiSpuBarcodeRepository.findByBarcodeStartingWith(
                ChaiBarcodeUtil.INTERNAL_PREFIX);
        Map<Long, String> codeBySpu = new LinkedHashMap<>();
        for (ChaiSpuBarcode row : rows) {
            if (row.getSpuId() == null || !StringUtils.hasText(row.getBarcode())) {
                continue;
            }
            codeBySpu.putIfAbsent(row.getSpuId(), row.getBarcode());
        }
        if (codeBySpu.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChaiSpu> list = chaiSpuRepository.findByIdIn(new ArrayList<>(codeBySpu.keySet()));
        List<ChaiSpu> result = new ArrayList<>();
        for (ChaiSpu spu : list) {
            if (spu.getDeleted() != null && spu.getDeleted() == 1) {
                continue;
            }
            if (filterBrands != null && !filterBrands.isEmpty()
                    && (spu.getBrand() == null || !filterBrands.contains(spu.getBrand()))) {
                continue;
            }
            spu.setBarcode(codeBySpu.get(spu.getId()));
            result.add(spu);
        }
        Map<Long, String> brandNames = loadBrandNameMap();
        result.sort(Comparator
                .comparing((ChaiSpu s) -> brandNames.getOrDefault(s.getBrand(), ""),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.getName() == null ? "" : s.getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.getId() == null ? 0L : s.getId()));
        if (result.size() > MAX_LABEL_PRINT) {
            throw new IllegalArgumentException(
                    "一次最多打印 " + MAX_LABEL_PRINT + " 张，当前 " + result.size()
                            + " 张，请缩小品牌范围");
        }
        return result;
    }

    private Map<Long, String> loadBrandNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (ChaiBrand brand : chaiBrandRepository.findAll()) {
            if (brand.getId() != null) {
                map.put(brand.getId(), brand.getName() != null ? brand.getName() : "");
            }
        }
        return map;
    }

    /** 任意 SPU（含已软删）是否引用该品牌 */
    public boolean isBrandInUse(Long brand) {
        return brand != null && chaiSpuRepository.existsByBrand(brand);
    }

    /** 任意 SPU（含已软删）是否引用该保质期 */
    public boolean isExpirationInUse(Long expiration) {
        return expiration != null && chaiSpuRepository.existsByExpiration(expiration);
    }

    /**
     * 同品牌下是否存在同名商品（可排除当前编辑记录）
     */
    public boolean isNameTakenByOtherInBrand(Long brand, String name, Long excludeId) {
        if (brand == null || !StringUtils.hasText(name)) {
            return false;
        }
        String trimmed = name.trim();
        if (excludeId == null) {
            return chaiSpuRepository.existsByBrandAndNameAndDeleted(brand, trimmed, 0);
        }
        return chaiSpuRepository.existsByBrandAndNameAndIdNotAndDeleted(brand, trimmed, excludeId, 0);
    }

    public Page<ChaiSpu> getPageList(ChaiSpu param) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "updateTime"));
        PageRequest page = PageSort.pageRequest(orders);
        return chaiSpuRepository.findAll((Root<ChaiSpu> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> preList = genCondition(root, cb, param);
            Predicate[] pres = new Predicate[preList.size()];
            return query.where(preList.toArray(pres)).getRestriction();
        }, page);
    }

    public ChaiSpu save(ChaiSpu entity) {
        boolean isCreate = false;
        String oldKeywords = null;
        Long oldBrand = null;
        String oldBarcode = null;
        Integer oldHideExternal = null;
        Integer oldCoreFlag = null;
        Integer oldTastingFlag = null;
        Long oldPackSpuId = null;
        if (entity.getId() == null) {
            entity.setSpuCode("");
            entity.setCreateTime(System.currentTimeMillis());
            if (entity.getDeleted() == null) {
                entity.setDeleted(0);
            }
            if (entity.getNonSale() == null) {
                entity.setNonSale(0);
            }
            // 停写旧列：新建占位空串；条码走 chai_spu_barcode
            entity.setBarcode("");
            isCreate = true;
        } else {
            ChaiSpu old = getById(entity.getId());
            if (old != null) {
                oldKeywords = old.getKeywords();
                oldBrand = old.getBrand();
                oldBarcode = old.getBarcode();
                oldHideExternal = old.getHideExternal();
                oldCoreFlag = old.getCoreFlag();
                oldTastingFlag = old.getTastingFlag();
                oldPackSpuId = old.getPackSpuId();
                // 停写旧列：主表单不改 barcode，保留迁移源
                entity.setBarcode(oldBarcode != null ? oldBarcode : "");
            }
        }
        if (ChaiPriceUtil.isNonSale(entity.getNonSale())) {
            entity.setNonSale(1);
            entity.setOfficialPrice(null);
        } else {
            entity.setNonSale(0);
            if (entity.getOfficialPrice() == null) {
                throw new IllegalArgumentException("官方价必填且须大于0");
            }
        }
        normalizeAndValidateMarks(entity);
        String keywords = ChaiSearchTextUtil.normalizeKeywords(entity.getKeywords());
        entity.setKeywords(keywords);
        String brandName = resolveBrandName(entity.getBrand());
        entity.setSearchText(ChaiSearchTextUtil.buildSearchText(brandName, entity.getName(), keywords));
        entity.setUpdateTime(System.currentTimeMillis());
        entity = chaiSpuRepository.save(entity);
        if (isCreate) {
            entity.setSpuCode(ChaiCodeUtil.spuCode(entity.getId()));
            // 新建后 id 已有，再验整装不能指自己
            normalizeAndValidateMarks(entity);
            entity = chaiSpuRepository.save(entity);
        } else {
            boolean keywordsChanged = !Objects.equals(
                    ChaiSearchTextUtil.normalizeKeywords(oldKeywords), keywords);
            boolean brandChanged = !Objects.equals(oldBrand, entity.getBrand());
            if (keywordsChanged || brandChanged) {
                chaiSkuService.syncKeywordsFromSpu(entity, brandName, entity.getOperator());
            }
        }
        boolean marksChanged = isCreate
                || !Objects.equals(nzFlag(oldHideExternal), entity.getHideExternal())
                || !Objects.equals(nzFlag(oldCoreFlag), entity.getCoreFlag())
                || !Objects.equals(nzFlag(oldTastingFlag), entity.getTastingFlag())
                || !Objects.equals(oldPackSpuId, entity.getPackSpuId());
        if (marksChanged) {
            chaiSkuService.syncMarksFromSpu(entity, entity.getOperator());
        }
        return entity;
    }

    /**
     * 规范化标记默认值并校验：取消品鉴清空整装；整装不可指自己；目标须存在未删。
     */
    public void normalizeAndValidateMarks(ChaiSpu entity) {
        if (entity == null) {
            return;
        }
        entity.setHideExternal(YesOrNo.isYes(entity.getHideExternal()) ? 1 : 0);
        entity.setCoreFlag(YesOrNo.isYes(entity.getCoreFlag()) ? 1 : 0);
        entity.setTastingFlag(YesOrNo.isYes(entity.getTastingFlag()) ? 1 : 0);
        if (!YesOrNo.isYes(entity.getTastingFlag())) {
            entity.setPackSpuId(null);
            return;
        }
        Long packId = entity.getPackSpuId();
        if (packId == null) {
            return;
        }
        if (entity.getId() != null && packId.equals(entity.getId())) {
            throw new IllegalArgumentException("整装SPU不能选择自己");
        }
        ChaiSpu pack = getById(packId);
        if (pack == null || (pack.getDeleted() != null && pack.getDeleted() == 1)) {
            throw new IllegalArgumentException("整装SPU不存在或已删除");
        }
    }

    private static int nzFlag(Integer v) {
        return v != null ? v : 0;
    }

    /**
     * 编辑页选整装：未删、非品鉴款、可排除当前 SPU；按名称/编码/search_text 模糊，最多 limit 条。
     */
    public List<Map<String, Object>> listPackOptions(String keyword, Long excludeId, int limit) {
        int lim = limit <= 0 ? 20 : Math.min(limit, 50);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        PageRequest page = PageRequest.of(0, lim, Sort.by(Sort.Direction.DESC, "updateTime"));
        Page<ChaiSpu> result = chaiSpuRepository.findAll((Root<ChaiSpu> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> list = new ArrayList<>();
            list.add(cb.equal(root.get("deleted").as(Integer.class), 0));
            list.add(cb.or(
                    cb.isNull(root.get("tastingFlag")),
                    cb.equal(root.get("tastingFlag").as(Integer.class), 0)));
            if (excludeId != null) {
                list.add(cb.notEqual(root.get("id").as(Long.class), excludeId));
            }
            if (StringUtils.hasText(kw)) {
                String like = "%" + kw + "%";
                list.add(cb.or(
                        cb.like(root.get("name").as(String.class), like),
                        cb.like(root.get("spuCode").as(String.class), like),
                        cb.like(root.get("searchText").as(String.class), like)));
            }
            return cb.and(list.toArray(new Predicate[0]));
        }, page);
        Map<Long, String> brandNames = loadBrandNameMap();
        List<Map<String, Object>> out = new ArrayList<>();
        for (ChaiSpu spu : result.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", spu.getId());
            row.put("spuCode", spu.getSpuCode());
            row.put("name", spu.getName());
            row.put("brandName", brandNames.getOrDefault(spu.getBrand(), ""));
            row.put("label", formatPackLabel(spu, brandNames.getOrDefault(spu.getBrand(), "")));
            out.add(row);
        }
        return out;
    }

    /** 整装选择展示文案 */
    public String formatPackLabel(ChaiSpu spu) {
        if (spu == null) {
            return "";
        }
        return formatPackLabel(spu, resolveBrandName(spu.getBrand()));
    }

    private static String formatPackLabel(ChaiSpu spu, String brandName) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(brandName)) {
            sb.append(brandName).append(' ');
        }
        if (StringUtils.hasText(spu.getName())) {
            sb.append(spu.getName());
        }
        if (StringUtils.hasText(spu.getSpuCode())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append('(').append(spu.getSpuCode()).append(')');
        }
        return sb.length() > 0 ? sb.toString() : String.valueOf(spu.getId());
    }

    /**
     * 批量只改标记位（列表「操作」）；取消品鉴时清空 pack_spu_id；同步全部 SKU。
     */
    @Transactional
    public void batchUpdateMarks(List<Long> idList, Integer hideExternal, Integer coreFlag,
                                 Integer tastingFlag, String operator) {
        if (idList == null || idList.isEmpty()) {
            throw new IllegalArgumentException("请先选择SPU");
        }
        if (hideExternal == null && coreFlag == null && tastingFlag == null) {
            throw new IllegalArgumentException("未指定要修改的标记");
        }
        long now = System.currentTimeMillis();
        for (Long id : idList) {
            if (id == null) {
                continue;
            }
            ChaiSpu spu = getById(id);
            if (spu == null) {
                continue;
            }
            if (spu.getDeleted() != null && spu.getDeleted() == 1) {
                throw new IllegalArgumentException("SPU「" + spu.getName() + "」已删除，请先恢复");
            }
            if (hideExternal != null) {
                spu.setHideExternal(hideExternal);
            }
            if (coreFlag != null) {
                spu.setCoreFlag(coreFlag);
            }
            if (tastingFlag != null) {
                spu.setTastingFlag(tastingFlag);
            }
            normalizeAndValidateMarks(spu);
            if (StringUtils.hasText(operator)) {
                spu.setOperator(operator);
            }
            spu.setUpdateTime(now);
            spu = chaiSpuRepository.save(spu);
            chaiSkuService.syncMarksFromSpu(spu, operator);
        }
    }

    /**
     * 独立维护关键词：更新 keywords / search_text，并同步全部 SKU。
     */
    @Transactional
    public ChaiSpu saveKeywords(Long id, String keywords, String operator) {
        if (id == null) {
            throw new IllegalArgumentException("SPU不能为空");
        }
        ChaiSpu spu = getById(id);
        if (spu == null) {
            throw new IllegalArgumentException("SPU不存在");
        }
        if (spu.getDeleted() != null && spu.getDeleted() == 1) {
            throw new IllegalArgumentException("已删除的SPU不能维护关键词，请先恢复");
        }
        String kw = ChaiSearchTextUtil.normalizeKeywords(keywords);
        String brandName = resolveBrandName(spu.getBrand());
        spu.setKeywords(kw);
        spu.setSearchText(ChaiSearchTextUtil.buildSearchText(brandName, spu.getName(), kw));
        if (StringUtils.hasText(operator)) {
            spu.setOperator(operator);
        }
        spu.setUpdateTime(System.currentTimeMillis());
        spu = chaiSpuRepository.save(spu);
        chaiSkuService.syncKeywordsFromSpu(spu, brandName, operator);
        return spu;
    }

    /**
     * 品牌改名：重算该品牌下全部 SPU / SKU 的 search_text。
     */
    @Transactional
    public int rebuildSearchTextForBrand(Long brandId, String brandName) {
        if (brandId == null) {
            return 0;
        }
        String bn = brandName != null ? brandName : "";
        List<ChaiSpu> spus = chaiSpuRepository.findByBrand(brandId);
        long now = System.currentTimeMillis();
        for (ChaiSpu spu : spus) {
            spu.setSearchText(ChaiSearchTextUtil.buildSearchText(bn, spu.getName(), spu.getKeywords()));
            spu.setUpdateTime(now);
            chaiSpuRepository.save(spu);
        }
        return chaiSkuService.rebuildSearchTextByBrand(brandId, bn);
    }

    public String resolveBrandName(Long brandId) {
        if (brandId == null) {
            return "";
        }
        return chaiBrandRepository.findById(brandId)
                .map(ChaiBrand::getName)
                .orElse("");
    }

    /**
     * 批量回填 SPU / SKU 的 search_text（keywords 不变；空则仅品牌+品名）。
     *
     * @param forceAll true=全量重算；false=仅 search_text 为空
     * @return spuTotal / spuUpdated / spuSkipped / skuTotal / skuUpdated / skuSkipped
     */
    @Transactional
    public Map<String, Integer> fillSearchTextBatch(boolean forceAll) {
        Map<Long, String> brandNames = new HashMap<>();
        for (ChaiBrand brand : chaiBrandRepository.findAll()) {
            if (brand.getId() != null) {
                brandNames.put(brand.getId(), brand.getName() != null ? brand.getName() : "");
            }
        }
        List<ChaiSpu> spus = chaiSpuRepository.findAll();
        int spuUpdated = 0;
        int spuSkipped = 0;
        long now = System.currentTimeMillis();
        for (ChaiSpu spu : spus) {
            if (!forceAll && StringUtils.hasText(spu.getSearchText())) {
                spuSkipped++;
                continue;
            }
            String bn = spu.getBrand() != null ? brandNames.getOrDefault(spu.getBrand(), "") : "";
            String text = ChaiSearchTextUtil.buildSearchText(bn, spu.getName(), spu.getKeywords());
            String before = spu.getSearchText() != null ? spu.getSearchText() : "";
            if (Objects.equals(before, text)) {
                spuSkipped++;
                continue;
            }
            spu.setSearchText(text);
            spu.setUpdateTime(now);
            chaiSpuRepository.save(spu);
            spuUpdated++;
        }
        Map<String, Integer> skuResult = chaiSkuService.fillSearchTextBatch(forceAll, brandNames);
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("spuTotal", spus.size());
        result.put("spuUpdated", spuUpdated);
        result.put("spuSkipped", spuSkipped);
        result.put("skuTotal", skuResult.getOrDefault("total", 0));
        result.put("skuUpdated", skuResult.getOrDefault("updated", 0));
        result.put("skuSkipped", skuResult.getOrDefault("skipped", 0));
        return result;
    }

    /**
     * search_text 为空的 SPU / SKU 数量（工具页展示用）。
     */
    public Map<String, Long> countMissingSearchText() {
        long spuMissing = chaiSpuRepository.count((Root<ChaiSpu> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.or(
                        cb.isNull(root.get("searchText")),
                        cb.equal(root.get("searchText").as(String.class), "")
                ));
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("spu", spuMissing);
        result.put("sku", chaiSkuService.countMissingSearchText());
        return result;
    }

    @Transactional
    public void softDeleteByIdIn(List<Long> idList, String operator) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        for (Long id : idList) {
            if (id == null) {
                continue;
            }
            if (chaiStockService.hasPositiveQtyBySpuId(id)) {
                ChaiSpu spu = getById(id);
                String label = spu != null && StringUtils.hasText(spu.getSpuCode())
                        ? spu.getSpuCode() : String.valueOf(id);
                throw new IllegalArgumentException(
                        "「" + label + "」下仍有库存，不能删除；请先出库或调拨至 0");
            }
        }
        long now = System.currentTimeMillis();
        for (Long id : idList) {
            int updated = chaiSpuRepository.softDeleteById(
                    id, ChaiStatus.OFFLINE.getCode(), operator, now);
            if (updated > 0) {
                chaiSkuService.markDeletedBySpuId(id, operator);
            }
        }
    }

    @Transactional
    public void restoreByIdIn(List<Long> idList, String operator) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Long id : idList) {
            int updated = chaiSpuRepository.restoreById(
                    id, ChaiStatus.OFFLINE.getCode(), operator, now);
            if (updated > 0) {
                chaiSkuService.restoreBySpuId(id, operator);
            }
        }
    }

    private List<Predicate> genCondition(Root<ChaiSpu> root, CriteriaBuilder cb, ChaiSpu param) {
        List<Predicate> preList = new ArrayList<>();
        if (param == null) {
            return preList;
        }
        if (StringUtils.hasText(param.getSpuCode())) {
            preList.add(cb.equal(root.get("spuCode").as(String.class), param.getSpuCode().trim()));
        }
        if (StringUtils.hasText(param.getBarcode())) {
            List<Long> spuIds = chaiSpuBarcodeService.resolveSpuIds(param.getBarcode().trim());
            if (spuIds.isEmpty()) {
                preList.add(cb.disjunction());
            } else {
                preList.add(root.get("id").in(spuIds));
            }
        } else {
            ChaiBarcodeKind kind = ChaiBarcodeKind.fromCode(param.getBarcodeKind());
            if (kind == ChaiBarcodeKind.EMPTY) {
                List<Long> withCode = chaiSpuBarcodeService.listSpuIdsHavingAnyBarcode();
                if (!withCode.isEmpty()) {
                    preList.add(cb.not(root.get("id").in(withCode)));
                }
            } else if (kind == ChaiBarcodeKind.NATIONAL) {
                List<Long> ids = chaiSpuBarcodeService.listSpuIdsByBarcodePrefix("69");
                if (ids.isEmpty()) {
                    preList.add(cb.disjunction());
                } else {
                    preList.add(root.get("id").in(ids));
                }
            } else if (kind == ChaiBarcodeKind.SYSTEM) {
                List<Long> ids = chaiSpuBarcodeService.listSpuIdsByBarcodePrefix(
                        ChaiBarcodeUtil.INTERNAL_PREFIX);
                if (ids.isEmpty()) {
                    preList.add(cb.disjunction());
                } else {
                    preList.add(root.get("id").in(ids));
                }
            }
        }
        if (StringUtils.hasText(param.getName())) {
            preList.add(cb.like(root.get("name").as(String.class), "%" + param.getName().trim() + "%"));
        }
        if (param.getBrand() != null) {
            preList.add(cb.equal(root.get("brand").as(Long.class), param.getBrand()));
        }
        if (param.getType() != null) {
            preList.add(cb.equal(root.get("type").as(Integer.class), param.getType()));
        }
        if (param.getGrade() != null) {
            preList.add(cb.equal(root.get("grade").as(Integer.class), param.getGrade()));
        }
        if (param.getYear() != null) {
            preList.add(cb.equal(root.get("year").as(Integer.class), param.getYear()));
        }
        if (param.getProdBatch() != null) {
            preList.add(cb.equal(root.get("prodBatch").as(Integer.class), param.getProdBatch()));
        }
        if (param.getStarLevel() != null) {
            preList.add(cb.equal(root.get("starLevel").as(Integer.class), param.getStarLevel()));
        }
        if (param.getStatus() != null) {
            preList.add(cb.equal(root.get("status").as(Integer.class), param.getStatus()));
        }
        if (param.getNonSale() != null) {
            preList.add(cb.equal(root.get("nonSale").as(Integer.class), param.getNonSale()));
        }
        if (param.getHideExternal() != null) {
            preList.add(cb.equal(root.get("hideExternal").as(Integer.class), param.getHideExternal()));
        }
        if (param.getCoreFlag() != null) {
            preList.add(cb.equal(root.get("coreFlag").as(Integer.class), param.getCoreFlag()));
        }
        if (param.getTastingFlag() != null) {
            preList.add(cb.equal(root.get("tastingFlag").as(Integer.class), param.getTastingFlag()));
        }
        if (param.getExcludeId() != null) {
            preList.add(cb.notEqual(root.get("id").as(Long.class), param.getExcludeId()));
        }
        if (param.getDeleted() != null) {
            preList.add(cb.equal(root.get("deleted").as(Integer.class), param.getDeleted()));
        }
        return preList;
    }
}
