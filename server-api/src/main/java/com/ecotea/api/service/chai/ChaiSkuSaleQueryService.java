package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecotea.api.common.enums.base.YesOrNo;
import com.ecotea.api.common.enums.chai.ChaiStatus;
import com.ecotea.api.common.utils.DictUtils;
import com.ecotea.api.common.utils.chai.ChaiPriceUtil;
import com.ecotea.api.common.utils.chai.ChaiSpecUtil;
import com.ecotea.api.common.utils.chai.ChaiUrlListUtil;
import com.ecotea.api.domain.chai.ChaiBrand;
import com.ecotea.api.domain.chai.ChaiExpiration;
import com.ecotea.api.domain.chai.ChaiSku;
import com.ecotea.api.mapper.chai.ChaiBrandMapper;
import com.ecotea.api.mapper.chai.ChaiExpirationMapper;
import com.ecotea.api.mapper.chai.ChaiSkuMapper;
import com.ecotea.api.vo.chai.ChaiSkuSaleItemVO;
import com.ecotea.api.vo.chai.ChaiSkuSalePageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChaiSku 销售只读查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChaiSkuSaleQueryService {

    private static final String DICT_GRADE = "CHAI_GRADE";
    private static final String DICT_PROD_BATCH = "CHAI_PROD_BATCH";

    private final ChaiSkuMapper chaiSkuMapper;
    private final ChaiBrandMapper chaiBrandMapper;
    private final ChaiExpirationMapper chaiExpirationMapper;

    /**
     * 上架且未删除 SKU 分页。keyword：先品牌名完全匹配（chai_brand），否则按名称模糊。
     */
    public ChaiSkuSalePageVO pageSaleList(String keyword, long page, long size) {
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
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode());

        String matchType = "none";
        if (brandId != null) {
            wrapper.eq(ChaiSku::getBrand, brandId);
            matchType = "brand_exact";
        } else if (StringUtils.hasText(kw)) {
            wrapper.like(ChaiSku::getName, kw);
            matchType = "name_like";
        }

        wrapper.orderByDesc(ChaiSku::getUpdateTime).orderByDesc(ChaiSku::getId);

        Page<ChaiSku> mpPage = chaiSkuMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> brandNameMap = loadBrandNameMap(mpPage.getRecords());
        Map<Long, String> expirationNameMap = loadExpirationNameMap(mpPage.getRecords());

        List<ChaiSkuSaleItemVO> list = new ArrayList<>();
        for (ChaiSku sku : mpPage.getRecords()) {
            list.add(toSaleVo(sku, brandNameMap, expirationNameMap));
        }

        ChaiSkuSalePageVO result = new ChaiSkuSalePageVO();
        result.setTotal(mpPage.getTotal());
        result.setPage(mpPage.getCurrent());
        result.setSize(mpPage.getSize());
        result.setMatchType(matchType);
        result.setKeyword(kw);
        result.setList(list);

        log.info("chai sku sale list, keyword={}, matchType={}, page={}, size={}, total={}",
                kw, matchType, page, size, mpPage.getTotal());
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
                .filter(id -> id != null)
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
                .filter(id -> id != null)
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

    private ChaiSkuSaleItemVO toSaleVo(ChaiSku sku,
                                       Map<Long, String> brandNameMap,
                                       Map<Long, String> expirationNameMap) {
        String brandName = sku.getBrand() == null ? null : brandNameMap.get(sku.getBrand());
        String title;
        if (StringUtils.hasText(brandName) && StringUtils.hasText(sku.getName())) {
            title = brandName + " " + sku.getName();
        } else if (StringUtils.hasText(sku.getName())) {
            title = sku.getName();
        } else {
            title = brandName != null ? brandName : "未命名";
        }

        ChaiSkuSaleItemVO vo = new ChaiSkuSaleItemVO();
        vo.setId(sku.getId());
        vo.setTitle(title);
        vo.setName(sku.getName());
        vo.setBrandName(brandName);
        if (sku.getGrade() != null) {
            vo.setGradeName(DictUtils.keyValue(DICT_GRADE, String.valueOf(sku.getGrade())));
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
        java.util.List<String> images = ChaiUrlListUtil.parseUrlList(sku.getShowImageUrls());
        vo.setImageUrls(images);
        vo.setCoverImage(images.isEmpty() ? null : images.get(0));
        return vo;
    }

    private String formatProdBatch(Integer year, Integer prodBatch) {
        if (year == null && prodBatch == null) {
            return null;
        }
        String batchName = prodBatch == null ? null
                : DictUtils.keyValue(DICT_PROD_BATCH, String.valueOf(prodBatch));
        if (year != null && StringUtils.hasText(batchName)) {
            return year + "年" + batchName;
        }
        if (year != null) {
            return year + "年";
        }
        return batchName;
    }
}
