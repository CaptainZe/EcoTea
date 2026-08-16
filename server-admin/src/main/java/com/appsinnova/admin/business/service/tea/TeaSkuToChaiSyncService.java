package com.appsinnova.admin.business.service.tea;

import com.appsinnova.admin.business.common.enums.chai.ChaiStatus;
import com.appsinnova.admin.business.common.utils.chai.ChaiSpecUtil;
import com.appsinnova.admin.business.domain.chai.ChaiBrand;
import com.appsinnova.admin.business.domain.chai.ChaiExpiration;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.domain.tea.TeaSku;
import com.appsinnova.admin.business.service.chai.ChaiBrandService;
import com.appsinnova.admin.business.service.chai.ChaiExpirationService;
import com.appsinnova.admin.business.service.chai.ChaiSkuService;
import com.appsinnova.admin.business.service.chai.ChaiSpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 将旧 tea_sku 快速创建为 chai SPU + 6 个半年 SKU
 */
@Service
@RequiredArgsConstructor
public class TeaSkuToChaiSyncService {

    private final TeaSkuService teaSkuService;
    private final ChaiSpuService chaiSpuService;
    private final ChaiSkuService chaiSkuService;
    private final ChaiBrandService chaiBrandService;
    private final ChaiExpirationService chaiExpirationService;

    @Transactional
    public Long sync(Long teaSkuId, ChaiSpu form, String operator) {
        TeaSku teaSku = teaSkuService.getById(teaSkuId);
        if (teaSku == null) {
            throw new IllegalArgumentException("茶叶SKU不存在");
        }
        if (Integer.valueOf(1).equals(teaSku.getSyncFlag())) {
            throw new IllegalArgumentException("该记录已同步，请先标记为未同步后再操作");
        }
        validateForm(form);

        ChaiBrand brand = chaiBrandService.getById(form.getBrand());
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        ChaiExpiration expiration = chaiExpirationService.getById(form.getExpiration());
        if (expiration == null) {
            throw new IllegalArgumentException("保质期不存在");
        }

        ChaiSpu spu = new ChaiSpu();
        spu.setName(form.getName().trim());
        spu.setStarLevel(form.getStarLevel());
        spu.setBrand(form.getBrand());
        spu.setExpiration(form.getExpiration());
        spu.setType(form.getType());
        spu.setGrade(form.getGrade());
        spu.setYear(form.getYear());
        spu.setProdBatch(form.getProdBatch());
        spu.setSpec(ChaiSpecUtil.buildSpecJson(
                form.getTotalNetWeight(), form.getUnitWeight(),
                form.getUnitCount(), form.getUnitLabel()));
        spu.setShowImageUrls(blankToEmptyJson(form.getShowImageUrls()));
        spu.setRealImageUrls(blankToEmptyJson(form.getRealImageUrls()));
        spu.setStatus(ChaiStatus.OFFLINE.getCode());
        spu.setOperator(operator);
        ChaiSpu savedSpu = chaiSpuService.save(spu);

        List<ChaiSku> skuList = chaiSkuService.buildDefaultSkuList(savedSpu);
        if (skuList.isEmpty()) {
            throw new IllegalArgumentException("无法根据年份与生产批次生成半年SKU");
        }
        for (ChaiSku sku : skuList) {
            applyTeaPrices(sku, teaSku);
        }
        chaiSkuService.saveBatchForSpu(savedSpu, skuList, operator);

        teaSkuService.updateSyncFlag(Collections.singletonList(teaSkuId), 1, operator);
        return savedSpu.getId();
    }

    private void applyTeaPrices(ChaiSku sku, TeaSku teaSku) {
        sku.setOfficialPrice(defaultOne(teaSku.getOfficialPrice()));
        sku.setSalePrice(defaultOne(teaSku.getSalePrice()));
        sku.setRecyclePrice(defaultOne(teaSku.getRecyclePrice()));
        sku.setRecyclePriceReducePer(teaSku.getRecyclePriceReducePer() != null
                ? teaSku.getRecyclePriceReducePer() : 5);
        sku.setRecyclePriceReduceNoBag(teaSku.getRecyclePriceReduceNoBag() != null
                ? teaSku.getRecyclePriceReduceNoBag() : new BigDecimal("10"));
    }

    private static BigDecimal defaultOne(BigDecimal value) {
        return value != null ? value : BigDecimal.ONE;
    }

    private static String blankToEmptyJson(String urls) {
        return StringUtils.hasText(urls) ? urls : "[]";
    }

    private void validateForm(ChaiSpu form) {
        if (form == null) {
            throw new IllegalArgumentException("同步数据不能为空");
        }
        if (!StringUtils.hasText(form.getName())) {
            throw new IllegalArgumentException("商品名称必填");
        }
        if (form.getStarLevel() == null) {
            throw new IllegalArgumentException("星级必选");
        }
        if (form.getBrand() == null) {
            throw new IllegalArgumentException("品牌必选");
        }
        if (form.getExpiration() == null) {
            throw new IllegalArgumentException("保质期必选");
        }
        if (form.getType() == null) {
            throw new IllegalArgumentException("茶类必选");
        }
        if (form.getGrade() == null) {
            throw new IllegalArgumentException("等级必选");
        }
        if (form.getYear() == null) {
            throw new IllegalArgumentException("年份必填");
        }
        if (form.getProdBatch() == null) {
            throw new IllegalArgumentException("生产批次必选");
        }
        if (form.getTotalNetWeight() == null || form.getTotalNetWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("总净重必填且大于0");
        }
        if (form.getUnitWeight() == null || form.getUnitWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("单份重量必填且大于0");
        }
        if (form.getUnitCount() == null || form.getUnitCount() <= 0) {
            throw new IllegalArgumentException("份数必填且大于0");
        }
        if (form.getUnitLabel() == null) {
            throw new IllegalArgumentException("规格单位必选");
        }
    }
}
