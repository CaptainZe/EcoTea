package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecotea.api.common.constant.ChaiConstant;
import com.ecotea.api.common.enums.base.YesOrNo;
import com.ecotea.api.common.enums.chai.ChaiStatus;
import com.ecotea.api.common.utils.DictUtils;
import com.ecotea.api.common.utils.chai.ChaiPriceUtil;
import com.ecotea.api.common.utils.chai.ChaiSpecUtil;
import com.ecotea.api.common.utils.chai.ChaiUrlListUtil;
import com.ecotea.api.domain.chai.ChaiSku;
import com.ecotea.api.mapper.chai.ChaiSkuMapper;
import com.ecotea.api.vo.chai.ChaiSkuInfoItemVO;
import com.ecotea.api.vo.chai.ChaiSkuInfoPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 茶品信息（INFO）只读查询：仅上架未删；含无货；不查库存/分仓；同时带售价与回收价。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChaiSkuInfoQueryService {

    private final ChaiSkuMapper chaiSkuMapper;
    private final ChaiBrandService chaiBrandService;
    private final ChaiExpirationService chaiExpirationService;
    private final ChaiSpuBarcodeQueryService chaiSpuBarcodeQueryService;

    /**
     * 上架、未删除 SKU 分页（不按库存过滤）。
     * barcode：条码精确（有值时优先）；keyword：先品牌名完全匹配，否则名称模糊；spuId：同款；
     * brandIds / types：多选；priceMin / priceMax：售价区间；recyclePriceMin / recyclePriceMax：回收价区间；
     * officialPriceMin / officialPriceMax：官方价区间；
     * nonSale：1 只看非卖品 / 0 不看非卖品 / null 不限（只看非卖品时忽略官方价区间）；
     * year / prodBatch：精确匹配；
     * hideExternal / coreFlag / tastingFlag：1 是 / 0 否 / null 不限。
     */
    public ChaiSkuInfoPageVO pageInfoList(String keyword, String barcode, Long spuId,
                                          List<Long> brandIds, List<Integer> types,
                                          BigDecimal priceMin, BigDecimal priceMax,
                                          BigDecimal recyclePriceMin, BigDecimal recyclePriceMax,
                                          BigDecimal officialPriceMin, BigDecimal officialPriceMax,
                                          Integer nonSale, Integer year, Integer prodBatch,
                                          Integer hideExternal, Integer coreFlag, Integer tastingFlag,
                                          long page, long size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }

        String kw = keyword == null ? null : keyword.trim();
        String code = barcode == null ? null : barcode.trim();
        List<Long> brandIdList = brandIds == null ? Collections.emptyList() : brandIds;
        List<Integer> typeList = types == null ? Collections.emptyList() : types;
        BigDecimal[] saleRange = normalizePriceRange(priceMin, priceMax);
        BigDecimal saleMin = saleRange[0];
        BigDecimal saleMax = saleRange[1];
        BigDecimal[] recycleRange = normalizePriceRange(recyclePriceMin, recyclePriceMax);
        BigDecimal recycleMin = recycleRange[0];
        BigDecimal recycleMax = recycleRange[1];
        boolean onlyNonSale = YesOrNo.YES.getCode().equals(nonSale);
        boolean excludeNonSale = YesOrNo.NO.getCode().equals(nonSale);
        BigDecimal[] officialRange = onlyNonSale
                ? new BigDecimal[]{null, null}
                : normalizePriceRange(officialPriceMin, officialPriceMax);
        BigDecimal officialMin = officialRange[0];
        BigDecimal officialMax = officialRange[1];
        Long brandIdFromKw = (!StringUtils.hasText(code) && brandIdList.isEmpty())
                ? resolveBrandIdExact(kw) : null;

        LambdaQueryWrapper<ChaiSku> wrapper = new LambdaQueryWrapper<ChaiSku>()
                .eq(ChaiSku::getStatus, ChaiStatus.ONLINE.getCode())
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode());

        String matchType = "none";
        if (spuId != null) {
            wrapper.eq(ChaiSku::getSpuId, spuId);
            matchType = "spu";
        }

        if (StringUtils.hasText(code)) {
            List<Long> barcodeSpuIds = chaiSpuBarcodeQueryService.resolveSpuIds(code);
            if (barcodeSpuIds.isEmpty()) {
                ChaiSkuInfoPageVO empty = new ChaiSkuInfoPageVO();
                empty.setTotal(0L);
                empty.setPage(page);
                empty.setSize(size);
                empty.setMatchType("barcode");
                empty.setKeyword(kw);
                empty.setBarcode(code);
                empty.setList(Collections.emptyList());
                return empty;
            }
            wrapper.in(ChaiSku::getSpuId, barcodeSpuIds);
            matchType = "barcode";
        } else if (!brandIdList.isEmpty()) {
            wrapper.in(ChaiSku::getBrand, brandIdList);
            if (!"spu".equals(matchType)) {
                matchType = "brand_filter";
            }
            if (StringUtils.hasText(kw)) {
                wrapper.like(ChaiSku::getName, kw);
                if (!"spu".equals(matchType)) {
                    matchType = "name_like";
                }
            }
        } else if (brandIdFromKw != null) {
            wrapper.eq(ChaiSku::getBrand, brandIdFromKw);
            if (!"spu".equals(matchType)) {
                matchType = "brand_exact";
            }
        } else if (StringUtils.hasText(kw)) {
            wrapper.like(ChaiSku::getName, kw);
            if (!"spu".equals(matchType)) {
                matchType = "name_like";
            }
        }

        if (!typeList.isEmpty()) {
            wrapper.in(ChaiSku::getType, typeList);
        }
        if (saleMin != null) {
            wrapper.ge(ChaiSku::getSalePrice, saleMin);
        }
        if (saleMax != null) {
            wrapper.le(ChaiSku::getSalePrice, saleMax);
        }
        if (recycleMin != null) {
            wrapper.ge(ChaiSku::getRecyclePrice, recycleMin);
        }
        if (recycleMax != null) {
            wrapper.le(ChaiSku::getRecyclePrice, recycleMax);
        }
        if (onlyNonSale) {
            wrapper.eq(ChaiSku::getNonSale, YesOrNo.YES.getCode());
        } else if (excludeNonSale) {
            wrapper.and(w -> w.eq(ChaiSku::getNonSale, YesOrNo.NO.getCode())
                    .or()
                    .isNull(ChaiSku::getNonSale));
        }
        if (officialMin != null) {
            wrapper.ge(ChaiSku::getOfficialPrice, officialMin);
        }
        if (officialMax != null) {
            wrapper.le(ChaiSku::getOfficialPrice, officialMax);
        }
        if (year != null) {
            wrapper.eq(ChaiSku::getYear, year);
        }
        if (prodBatch != null) {
            wrapper.eq(ChaiSku::getProdBatch, prodBatch);
        }
        applyMarkFlagFilter(wrapper, ChaiSku::getHideExternal, hideExternal);
        applyMarkFlagFilter(wrapper, ChaiSku::getCoreFlag, coreFlag);
        applyMarkFlagFilter(wrapper, ChaiSku::getTastingFlag, tastingFlag);

        wrapper.orderByDesc(ChaiSku::getYear)
                .orderByDesc(ChaiSku::getProdBatch)
                .orderByDesc(ChaiSku::getId);

        Page<ChaiSku> mpPage = chaiSkuMapper.selectPage(new Page<>(page, size), wrapper);
        List<ChaiSku> records = mpPage.getRecords();
        Map<Long, String> brandNameMap = loadBrandNameMap(records);
        Map<Long, String> expirationNameMap = loadExpirationNameMap(records);
        Map<Long, Integer> sameSpuCountMap = loadSameSpuCountMap(records);

        List<ChaiSkuInfoItemVO> list = new ArrayList<>();
        for (ChaiSku sku : records) {
            list.add(toInfoVo(sku, brandNameMap, expirationNameMap, sameSpuCountMap));
        }

        ChaiSkuInfoPageVO result = new ChaiSkuInfoPageVO();
        result.setTotal(mpPage.getTotal());
        result.setPage(mpPage.getCurrent());
        result.setSize(mpPage.getSize());
        result.setMatchType(matchType);
        result.setKeyword(kw);
        result.setBarcode(code);
        result.setSpuId(spuId);
        result.setList(list);

        log.info("chai sku info list, keyword={}, barcode={}, spuId={}, brandIds={}, types={}, "
                        + "priceMin={}, priceMax={}, recyclePriceMin={}, recyclePriceMax={}, "
                        + "officialPriceMin={}, officialPriceMax={}, nonSale={}, year={}, prodBatch={}, "
                        + "hideExternal={}, coreFlag={}, tastingFlag={}, matchType={}, page={}, size={}, total={}",
                kw, code, spuId, brandIdList, typeList,
                saleMin, saleMax, recycleMin, recycleMax,
                officialMin, officialMax, nonSale, year, prodBatch,
                hideExternal, coreFlag, tastingFlag,
                matchType, page, size, mpPage.getTotal());
        return result;
    }

    /** 标记筛：1=是 / 0=否（含 null）/ null=不限。 */
    private static void applyMarkFlagFilter(LambdaQueryWrapper<ChaiSku> wrapper,
                                            com.baomidou.mybatisplus.core.toolkit.support.SFunction<ChaiSku, ?> column,
                                            Integer flag) {
        if (YesOrNo.YES.getCode().equals(flag)) {
            wrapper.eq(column, YesOrNo.YES.getCode());
        } else if (YesOrNo.NO.getCode().equals(flag)) {
            wrapper.and(w -> w.eq(column, YesOrNo.NO.getCode()).or().isNull(column));
        }
    }

    /** 上架未删 SKU 详情；不存在返回 null。不含库存。 */
    public ChaiSkuInfoItemVO getInfoDetail(Long id) {
        if (id == null) {
            return null;
        }
        ChaiSku sku = chaiSkuMapper.selectOne(new LambdaQueryWrapper<ChaiSku>()
                .eq(ChaiSku::getId, id)
                .eq(ChaiSku::getStatus, ChaiStatus.ONLINE.getCode())
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode())
                .last("LIMIT 1"));
        if (sku == null) {
            return null;
        }
        List<ChaiSku> records = Collections.singletonList(sku);
        Map<Long, String> brandNameMap = loadBrandNameMap(records);
        Map<Long, String> expirationNameMap = loadExpirationNameMap(records);
        Map<Long, Integer> sameSpuCountMap = loadSameSpuCountMap(records);
        return toInfoVo(sku, brandNameMap, expirationNameMap, sameSpuCountMap);
    }

    private static BigDecimal[] normalizePriceRange(BigDecimal priceMin, BigDecimal priceMax) {
        BigDecimal min = priceMin;
        BigDecimal max = priceMax;
        if (min != null && max != null && min.compareTo(max) > 0) {
            return new BigDecimal[]{max, min};
        }
        return new BigDecimal[]{min, max};
    }

    private Long resolveBrandIdExact(String keyword) {
        return chaiBrandService.resolveOnlineIdByNameExact(keyword);
    }

    private Map<Long, String> loadBrandNameMap(List<ChaiSku> records) {
        List<Long> ids = records.stream()
                .map(ChaiSku::getBrand)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return chaiBrandService.mapNamesByIds(ids);
    }

    private Map<Long, String> loadExpirationNameMap(List<ChaiSku> records) {
        List<Long> ids = records.stream()
                .map(ChaiSku::getExpiration)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return chaiExpirationService.mapNamesByIds(ids);
    }

    /** 各 spuId 下「上架+未删」SKU 数量（不要求有货）。 */
    private Map<Long, Integer> loadSameSpuCountMap(List<ChaiSku> records) {
        Set<Long> spuIds = records.stream()
                .map(ChaiSku::getSpuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (spuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ChaiSku> sameList = chaiSkuMapper.selectList(new LambdaQueryWrapper<ChaiSku>()
                .select(ChaiSku::getId, ChaiSku::getSpuId)
                .in(ChaiSku::getSpuId, spuIds)
                .eq(ChaiSku::getStatus, ChaiStatus.ONLINE.getCode())
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode()));
        Map<Long, Integer> map = new HashMap<>();
        for (ChaiSku sku : sameList) {
            if (sku.getSpuId() == null) {
                continue;
            }
            map.merge(sku.getSpuId(), 1, Integer::sum);
        }
        return map;
    }

    private ChaiSkuInfoItemVO toInfoVo(ChaiSku sku,
                                       Map<Long, String> brandNameMap,
                                       Map<Long, String> expirationNameMap,
                                       Map<Long, Integer> sameSpuCountMap) {
        String brandName = sku.getBrand() == null ? null : brandNameMap.get(sku.getBrand());
        String title;
        if (StringUtils.hasText(brandName) && StringUtils.hasText(sku.getName())) {
            title = brandName + " · " + sku.getName();
        } else if (StringUtils.hasText(sku.getName())) {
            title = sku.getName();
        } else {
            title = brandName != null ? brandName : "未命名";
        }

        BigDecimal recycleDamagePrice = ChaiPriceUtil.recycleAfterDamage(
                sku.getRecyclePrice(), sku.getRecyclePriceReducePer());

        ChaiSkuInfoItemVO vo = new ChaiSkuInfoItemVO();
        vo.setId(sku.getId());
        vo.setSpuId(sku.getSpuId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setTitle(title);
        vo.setName(sku.getName());
        vo.setBrandName(brandName);
        if (sku.getGrade() != null) {
            vo.setGradeName(DictUtils.keyValue(ChaiConstant.DICT_GRADE, String.valueOf(sku.getGrade())));
        }
        vo.setSpecShow(ChaiSpecUtil.toShow(sku.getSpec()));
        vo.setProdBatchShow(formatProdBatch(sku.getYear(), sku.getProdBatch()));
        if (sku.getExpiration() != null) {
            vo.setExpirationName(expirationNameMap.get(sku.getExpiration()));
        }
        vo.setHideExternal(sku.getHideExternal() != null ? sku.getHideExternal() : 0);
        vo.setCoreFlag(sku.getCoreFlag() != null ? sku.getCoreFlag() : 0);
        vo.setTastingFlag(sku.getTastingFlag() != null ? sku.getTastingFlag() : 0);

        vo.setOfficialPrice(sku.getOfficialPrice());
        vo.setOfficialPriceShow(ChaiPriceUtil.formatOfficialPrice(sku.getNonSale(), sku.getOfficialPrice()));

        vo.setSalePrice(sku.getSalePrice());
        vo.setSalePriceShow(ChaiPriceUtil.formatSaleWithDiscount(
                sku.getNonSale(), sku.getSalePrice(), sku.getOfficialPrice()));
        vo.setDiscountShow(ChaiPriceUtil.formatDiscountShow(
                sku.getNonSale(), sku.getSalePrice(), sku.getOfficialPrice()));

        vo.setRecyclePrice(sku.getRecyclePrice());
        vo.setRecyclePriceShow(ChaiPriceUtil.formatRecycleWithDiscount(
                sku.getNonSale(), sku.getRecyclePrice(), sku.getOfficialPrice()));
        vo.setRecycleDiscountShow(ChaiPriceUtil.formatDiscountShow(
                sku.getNonSale(), sku.getRecyclePrice(), sku.getOfficialPrice()));
        vo.setRecyclePriceReducePer(sku.getRecyclePriceReducePer());
        vo.setRecycleDamagePrice(recycleDamagePrice);
        vo.setRecycleDamagePriceShow(ChaiPriceUtil.formatRecycleDamagePriceShow(
                sku.getRecyclePrice(), sku.getRecyclePriceReducePer()));
        vo.setRecyclePriceReduceNoBag(sku.getRecyclePriceReduceNoBag());
        if (sku.getRecyclePriceReduceNoBag() != null) {
            vo.setRecyclePriceReduceNoBagShow(ChaiPriceUtil.plain(sku.getRecyclePriceReduceNoBag()));
        }

        if (sku.getSpuId() != null) {
            vo.setSameSpuCount(sameSpuCountMap.getOrDefault(sku.getSpuId(), 1));
        }
        List<String> images = ChaiUrlListUtil.parseUrlList(sku.getShowImageUrls());
        vo.setImageUrls(images);
        vo.setCoverImage(images.isEmpty() ? null : images.get(0));
        return vo;
    }

    private String formatProdBatch(Integer year, Integer prodBatch) {
        if (year == null && prodBatch == null) {
            return null;
        }
        String batchName = prodBatch == null ? null
                : DictUtils.keyValue(ChaiConstant.DICT_PROD_BATCH, String.valueOf(prodBatch));
        if (year != null && StringUtils.hasText(batchName)) {
            return year + "年" + batchName;
        }
        if (year != null) {
            return year + "年";
        }
        return batchName;
    }
}
