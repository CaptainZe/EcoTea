package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiCodeUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiPriceUtil;
import com.appsinnova.admin.business.common.utils.chai.ChaiSearchTextUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.repository.chai.ChaiBrandRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChaiSpuService {

    private final ChaiSpuRepository chaiSpuRepository;
    private final ChaiSkuService chaiSkuService;
    private final ChaiStockService chaiStockService;
    private final ChaiBrandRepository chaiBrandRepository;

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
        if (entity.getId() == null) {
            entity.setSpuCode("");
            entity.setCreateTime(System.currentTimeMillis());
            if (entity.getDeleted() == null) {
                entity.setDeleted(0);
            }
            if (entity.getNonSale() == null) {
                entity.setNonSale(0);
            }
            isCreate = true;
        } else {
            ChaiSpu old = getById(entity.getId());
            if (old != null) {
                oldKeywords = old.getKeywords();
                oldBrand = old.getBrand();
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
        String keywords = ChaiSearchTextUtil.normalizeKeywords(entity.getKeywords());
        entity.setKeywords(keywords);
        String brandName = resolveBrandName(entity.getBrand());
        entity.setSearchText(ChaiSearchTextUtil.buildSearchText(brandName, entity.getName(), keywords));
        entity.setUpdateTime(System.currentTimeMillis());
        entity = chaiSpuRepository.save(entity);
        if (isCreate) {
            entity.setSpuCode(ChaiCodeUtil.spuCode(entity.getId()));
            entity = chaiSpuRepository.save(entity);
        } else {
            boolean keywordsChanged = !Objects.equals(
                    ChaiSearchTextUtil.normalizeKeywords(oldKeywords), keywords);
            boolean brandChanged = !Objects.equals(oldBrand, entity.getBrand());
            if (keywordsChanged || brandChanged) {
                chaiSkuService.syncKeywordsFromSpu(entity, brandName, entity.getOperator());
            }
        }
        return entity;
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
        if (param.getDeleted() != null) {
            preList.add(cb.equal(root.get("deleted").as(Integer.class), param.getDeleted()));
        }
        return preList;
    }
}
