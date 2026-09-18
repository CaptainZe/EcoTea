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
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.domain.chai.ChaiExpiration;
import com.ecotea.api.domain.chai.ChaiSku;
import com.ecotea.api.domain.chai.ChaiStock;
import com.ecotea.api.domain.chai.ChaiStockWh;
import com.ecotea.api.domain.chai.ChaiWarehouse;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import com.ecotea.api.mapper.chai.ChaiExpirationMapper;
import com.ecotea.api.mapper.chai.ChaiSkuMapper;
import com.ecotea.api.mapper.chai.ChaiStockMapper;
import com.ecotea.api.mapper.chai.ChaiStockWhMapper;
import com.ecotea.api.mapper.chai.ChaiWarehouseMapper;
import com.ecotea.api.vo.chai.ChaiSkuRecycleItemVO;
import com.ecotea.api.vo.chai.ChaiSkuRecyclePageVO;
import com.ecotea.api.vo.chai.ChaiSkuSaleWhStockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ChaiSku 内部回收价目只读查询（上架未删；不要求有货，但返回库存信息）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChaiSkuRecycleQueryService {

    private final ChaiSkuMapper chaiSkuMapper;
    private final ChaiBrandMapper chaiBrandMapper;
    private final ChaiExpirationMapper chaiExpirationMapper;
    private final ChaiStockMapper chaiStockMapper;
    private final ChaiStockWhMapper chaiStockWhMapper;
    private final ChaiWarehouseMapper chaiWarehouseMapper;

    /**
     * 上架、未删除 SKU 分页（不按库存过滤）。
     * keyword：先品牌名完全匹配，否则名称模糊；spuId：同款。
     */
    public ChaiSkuRecyclePageVO pageRecycleList(String keyword, Long spuId, long page, long size) {
        return pageRecycleList(keyword, spuId, Collections.emptyList(), Collections.emptyList(), page, size);
    }

    /**
     * 上架、未删除 SKU 分页（不按库存过滤）。
     * keyword：先品牌名完全匹配，否则名称模糊；spuId：同款；
     * brandIds / types：多选筛选。
     */
    public ChaiSkuRecyclePageVO pageRecycleList(String keyword, Long spuId,
                                                List<Long> brandIds, List<Integer> types,
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
        List<Long> brandIdList = brandIds == null ? Collections.emptyList() : brandIds;
        List<Integer> typeList = types == null ? Collections.emptyList() : types;
        Long brandIdFromKw = brandIdList.isEmpty() ? resolveBrandIdExact(kw) : null;

        LambdaQueryWrapper<ChaiSku> wrapper = new LambdaQueryWrapper<ChaiSku>()
                .eq(ChaiSku::getStatus, ChaiStatus.ONLINE.getCode())
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode());

        String matchType = "none";
        if (spuId != null) {
            wrapper.eq(ChaiSku::getSpuId, spuId);
            matchType = "spu";
        }

        if (!brandIdList.isEmpty()) {
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

        wrapper.orderByDesc(ChaiSku::getYear)
                .orderByDesc(ChaiSku::getProdBatch)
                .orderByDesc(ChaiSku::getId);

        Page<ChaiSku> mpPage = chaiSkuMapper.selectPage(new Page<>(page, size), wrapper);
        List<ChaiSku> records = mpPage.getRecords();
        Map<Long, String> brandNameMap = loadBrandNameMap(records);
        Map<Long, String> expirationNameMap = loadExpirationNameMap(records);
        Map<Long, ChaiStock> stockMap = loadStockMap(records);
        Map<Long, Integer> sameSpuCountMap = loadSameSpuCountMap(records);

        List<ChaiSkuRecycleItemVO> list = new ArrayList<>();
        for (ChaiSku sku : records) {
            list.add(toRecycleVo(sku, brandNameMap, expirationNameMap, stockMap, sameSpuCountMap));
        }

        ChaiSkuRecyclePageVO result = new ChaiSkuRecyclePageVO();
        result.setTotal(mpPage.getTotal());
        result.setPage(mpPage.getCurrent());
        result.setSize(mpPage.getSize());
        result.setMatchType(matchType);
        result.setKeyword(kw);
        result.setSpuId(spuId);
        result.setList(list);

        log.info("chai sku recycle list, keyword={}, spuId={}, brandIds={}, types={}, matchType={}, page={}, size={}, total={}",
                kw, spuId, brandIdList, typeList, matchType, page, size, mpPage.getTotal());
        return result;
    }

    /**
     * 上架未删 SKU 详情；不存在返回 null。始终带分仓有货明细（不要求全仓有货）。
     */
    public ChaiSkuRecycleItemVO getRecycleDetail(Long id) {
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
        Map<Long, ChaiStock> stockMap = loadStockMap(records);
        Map<Long, Integer> sameSpuCountMap = loadSameSpuCountMap(records);
        ChaiSkuRecycleItemVO vo = toRecycleVo(sku, brandNameMap, expirationNameMap, stockMap, sameSpuCountMap);
        Map<Long, List<ChaiSkuSaleWhStockVO>> whStockMap = loadWarehouseStocksBySkuId(records, stockMap);
        vo.setWarehouseStocks(whStockMap.getOrDefault(sku.getId(), Collections.emptyList()));
        return vo;
    }

    private Long resolveBrandIdExact(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        ChaiBrand brand = chaiBrandMapper.selectOne(new LambdaQueryWrapper<ChaiBrand>()
                .eq(ChaiBrand::getName, keyword)
                .last("LIMIT 1"));
        return brand == null ? null : brand.getId();
    }

    private Map<Long, String> loadBrandNameMap(List<ChaiSku> records) {
        List<Long> ids = records.stream()
                .map(ChaiSku::getBrand)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, String> map = new HashMap<>();
        for (ChaiBrand brand : chaiBrandMapper.selectBatchIds(ids)) {
            map.put(brand.getId(), brand.getName());
        }
        return map;
    }

    private Map<Long, String> loadExpirationNameMap(List<ChaiSku> records) {
        List<Long> ids = records.stream()
                .map(ChaiSku::getExpiration)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, String> map = new HashMap<>();
        for (ChaiExpiration item : chaiExpirationMapper.selectBatchIds(ids)) {
            map.put(item.getId(), item.getName());
        }
        return map;
    }

    private Map<Long, ChaiStock> loadStockMap(List<ChaiSku> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        List<Long> skuIds = records.stream().map(ChaiSku::getId).collect(Collectors.toList());
        Map<Long, ChaiStock> map = new HashMap<>();
        for (ChaiStock stock : chaiStockMapper.selectList(new LambdaQueryWrapper<ChaiStock>()
                .in(ChaiStock::getSkuId, skuIds))) {
            if (stock.getSkuId() != null) {
                map.put(stock.getSkuId(), stock);
            }
        }
        return map;
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

    private Map<Long, List<ChaiSkuSaleWhStockVO>> loadWarehouseStocksBySkuId(List<ChaiSku> records,
                                                                            Map<Long, ChaiStock> stockMap) {
        List<WhLine> lines = loadPositiveWhLines(records, stockMap);
        if (lines.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ChaiSkuSaleWhStockVO>> map = new HashMap<>();
        for (WhLine line : lines) {
            ChaiSkuSaleWhStockVO vo = new ChaiSkuSaleWhStockVO();
            vo.setWhId(line.whId);
            vo.setShortName(line.shortName);
            vo.setQty(line.qty);
            vo.setDamageQty(line.damageQty);
            map.computeIfAbsent(line.skuId, k -> new ArrayList<>()).add(vo);
        }
        return map;
    }

    private List<WhLine> loadPositiveWhLines(List<ChaiSku> records, Map<Long, ChaiStock> stockMap) {
        if (CollectionUtils.isEmpty(records) || stockMap.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> stockIdToSkuId = new HashMap<>();
        for (ChaiSku sku : records) {
            ChaiStock stock = stockMap.get(sku.getId());
            if (stock != null && stock.getId() != null) {
                stockIdToSkuId.put(stock.getId(), sku.getId());
            }
        }
        if (stockIdToSkuId.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChaiStockWh> whRows = chaiStockWhMapper.selectList(new LambdaQueryWrapper<ChaiStockWh>()
                .in(ChaiStockWh::getStockId, stockIdToSkuId.keySet())
                .gt(ChaiStockWh::getQty, 0));
        if (CollectionUtils.isEmpty(whRows)) {
            return Collections.emptyList();
        }
        Set<Long> whIds = whRows.stream()
                .map(ChaiStockWh::getWhId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (whIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ChaiWarehouse> whMap = new HashMap<>();
        for (ChaiWarehouse wh : chaiWarehouseMapper.selectBatchIds(whIds)) {
            whMap.put(wh.getId(), wh);
        }

        List<WhLine> lines = new ArrayList<>();
        for (ChaiStockWh row : whRows) {
            Long skuId = stockIdToSkuId.get(row.getStockId());
            ChaiWarehouse wh = row.getWhId() == null ? null : whMap.get(row.getWhId());
            if (skuId == null || wh == null || !StringUtils.hasText(wh.getShortName())) {
                continue;
            }
            WhLine line = new WhLine();
            line.skuId = skuId;
            line.whId = wh.getId();
            line.shortName = wh.getShortName();
            line.qty = row.getQty() == null ? 0 : row.getQty();
            line.damageQty = row.getDamageQty() == null ? 0 : row.getDamageQty();
            line.orderNum = wh.getOrderNum() == null ? 0 : wh.getOrderNum();
            lines.add(line);
        }
        lines.sort(Comparator
                .comparingInt((WhLine l) -> l.orderNum).reversed()
                .thenComparing(l -> l.whId, Comparator.nullsLast(Long::compareTo)));
        return lines;
    }

    private ChaiSkuRecycleItemVO toRecycleVo(ChaiSku sku,
                                            Map<Long, String> brandNameMap,
                                            Map<Long, String> expirationNameMap,
                                            Map<Long, ChaiStock> stockMap,
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

        ChaiStock stock = stockMap.get(sku.getId());
        int totalQty = stock != null && stock.getTotalQty() != null ? stock.getTotalQty() : 0;
        int damageQty = stock != null && stock.getDamageQty() != null ? stock.getDamageQty() : 0;

        BigDecimal recycleDamagePrice = ChaiPriceUtil.recycleAfterDamage(
                sku.getRecyclePrice(), sku.getRecyclePriceReducePer());

        ChaiSkuRecycleItemVO vo = new ChaiSkuRecycleItemVO();
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
        vo.setOfficialPrice(sku.getOfficialPrice());
        vo.setOfficialPriceShow(ChaiPriceUtil.formatOfficialPrice(sku.getNonSale(), sku.getOfficialPrice()));
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
        vo.setTotalQty(totalQty);
        vo.setDamageQty(damageQty);
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

    private static final class WhLine {
        private Long skuId;
        private Long whId;
        private String shortName;
        private int qty;
        private int damageQty;
        private int orderNum;
    }
}
