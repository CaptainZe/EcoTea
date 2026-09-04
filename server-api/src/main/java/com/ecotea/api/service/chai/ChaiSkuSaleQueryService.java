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
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import com.ecotea.api.mapper.chai.ChaiExpirationMapper;
import com.ecotea.api.mapper.chai.ChaiSkuMapper;
import com.ecotea.api.mapper.chai.ChaiStockMapper;
import com.ecotea.api.vo.chai.ChaiSkuSaleItemVO;
import com.ecotea.api.vo.chai.ChaiSkuSalePageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
 * ChaiSku 销售只读查询（有货现货）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChaiSkuSaleQueryService {

    private static final String STOCK_IN_SQL =
            "SELECT sku_id FROM chai_stock WHERE total_qty > 0";

    private final ChaiSkuMapper chaiSkuMapper;
    private final ChaiBrandMapper chaiBrandMapper;
    private final ChaiExpirationMapper chaiExpirationMapper;
    private final ChaiStockMapper chaiStockMapper;

    /**
     * 上架、未删除、全仓有货 SKU 分页。
     * keyword：先品牌名完全匹配，否则名称模糊；spuId：同款筛选（可与 keyword 组合）。
     */
    public ChaiSkuSalePageVO pageSaleList(String keyword, Long spuId, long page, long size) {
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
        Long brandId = resolveBrandIdExact(kw);

        LambdaQueryWrapper<ChaiSku> wrapper = new LambdaQueryWrapper<ChaiSku>()
                .eq(ChaiSku::getStatus, ChaiStatus.ONLINE.getCode())
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode())
                .inSql(ChaiSku::getId, STOCK_IN_SQL);

        String matchType = "none";
        if (spuId != null) {
            wrapper.eq(ChaiSku::getSpuId, spuId);
            matchType = "spu";
        }
        if (brandId != null) {
            wrapper.eq(ChaiSku::getBrand, brandId);
            if (!"spu".equals(matchType)) {
                matchType = "brand_exact";
            }
        } else if (StringUtils.hasText(kw)) {
            wrapper.like(ChaiSku::getName, kw);
            if (!"spu".equals(matchType)) {
                matchType = "name_like";
            }
        }

        // 与 admin skuView 一致
        wrapper.orderByDesc(ChaiSku::getYear)
                .orderByDesc(ChaiSku::getProdBatch)
                .orderByDesc(ChaiSku::getId);

        Page<ChaiSku> mpPage = chaiSkuMapper.selectPage(new Page<>(page, size), wrapper);
        List<ChaiSku> records = mpPage.getRecords();
        Map<Long, String> brandNameMap = loadBrandNameMap(records);
        Map<Long, String> expirationNameMap = loadExpirationNameMap(records);
        Map<Long, ChaiStock> stockMap = loadStockMap(records);
        Map<Long, Integer> sameSpuCountMap = loadSameSpuSaleCountMap(records);

        List<ChaiSkuSaleItemVO> list = new ArrayList<>();
        for (ChaiSku sku : records) {
            list.add(toSaleVo(sku, brandNameMap, expirationNameMap, stockMap, sameSpuCountMap));
        }

        ChaiSkuSalePageVO result = new ChaiSkuSalePageVO();
        result.setTotal(mpPage.getTotal());
        result.setPage(mpPage.getCurrent());
        result.setSize(mpPage.getSize());
        result.setMatchType(matchType);
        result.setKeyword(kw);
        result.setSpuId(spuId);
        result.setList(list);

        log.info("chai sku sale list, keyword={}, spuId={}, matchType={}, page={}, size={}, total={}",
                kw, spuId, matchType, page, size, mpPage.getTotal());
        return result;
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

    /**
     * 各 spuId 下「上架+未删+有货」SKU 数量。
     */
    private Map<Long, Integer> loadSameSpuSaleCountMap(List<ChaiSku> records) {
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
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode())
                .inSql(ChaiSku::getId, STOCK_IN_SQL));
        Map<Long, Integer> map = new HashMap<>();
        for (ChaiSku sku : sameList) {
            if (sku.getSpuId() == null) {
                continue;
            }
            map.merge(sku.getSpuId(), 1, Integer::sum);
        }
        return map;
    }

    private ChaiSkuSaleItemVO toSaleVo(ChaiSku sku,
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

        ChaiSkuSaleItemVO vo = new ChaiSkuSaleItemVO();
        vo.setId(sku.getId());
        vo.setSpuId(sku.getSpuId());
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
        vo.setSalePrice(sku.getSalePrice());
        vo.setOfficialPriceShow(ChaiPriceUtil.formatOfficialPrice(sku.getNonSale(), sku.getOfficialPrice()));
        vo.setSalePriceShow(ChaiPriceUtil.formatSaleWithDiscount(sku.getSalePrice(), sku.getOfficialPrice()));
        vo.setTotalQty(totalQty);
        vo.setDamageQty(damageQty);
        if (sku.getSpuId() != null) {
            vo.setSameSpuSaleCount(sameSpuCountMap.getOrDefault(sku.getSpuId(), 1));
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
