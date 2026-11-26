package com.ecotea.api.service.chai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecotea.api.common.constant.RedisConstant;
import com.ecotea.api.common.enums.base.YesOrNo;
import com.ecotea.api.common.utils.JsonUtils;
import com.ecotea.api.common.utils.RedisUtils;
import com.ecotea.api.domain.chai.ChaiSku;
import com.ecotea.api.domain.chai.ChaiSpuBarcode;
import com.ecotea.api.mapper.chai.ChaiSkuMapper;
import com.ecotea.api.mapper.chai.ChaiSpuBarcodeMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 条码 → SPU id 分步解析（扫码精确查第一步）。
 * Redis → miss → 新表 + 过渡期旧列 → SET。
 */
@Service
@RequiredArgsConstructor
public class ChaiSpuBarcodeQueryService {

    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<List<Long>>() {
    };

    private final ChaiSpuBarcodeMapper chaiSpuBarcodeMapper;
    private final ChaiSkuMapper chaiSkuMapper;

    /**
     * 精确条码对应的 SPU id 列表（可多 SPU；无则空列表）。
     */
    public List<Long> resolveSpuIds(String barcode) {
        String code = normalize(barcode);
        if (!StringUtils.hasText(code)) {
            return Collections.emptyList();
        }
        String key = String.format(RedisConstant.CHAI_BARCODE_SPU_KEY, code);
        String cached = RedisUtils.get(RedisUtils.defaultRedis(), key);
        if (cached != null) {
            List<Long> fromCache = JsonUtils.readValueForCache(cached, LONG_LIST_TYPE);
            return fromCache != null ? fromCache : Collections.emptyList();
        }

        List<Long> spuIds = loadSpuIdsFromDb(code);
        String json = JsonUtils.writeValueAsString(spuIds);
        if (json != null) {
            int ttl = spuIds.isEmpty() ? RedisConstant.FIVE_MINUTE_EXPIRE : RedisConstant.DAY_EXPIRE;
            RedisUtils.set(RedisUtils.defaultRedis(), key, json, ttl);
        }
        return spuIds;
    }

    private List<Long> loadSpuIdsFromDb(String code) {
        Set<Long> ids = new LinkedHashSet<>();
        List<ChaiSpuBarcode> rows = chaiSpuBarcodeMapper.selectList(new LambdaQueryWrapper<ChaiSpuBarcode>()
                .eq(ChaiSpuBarcode::getBarcode, code)
                .select(ChaiSpuBarcode::getSpuId));
        for (ChaiSpuBarcode row : rows) {
            if (row.getSpuId() != null) {
                ids.add(row.getSpuId());
            }
        }
        // 过渡期：旧列尚在 SKU 上的继承码
        List<ChaiSku> legacy = chaiSkuMapper.selectList(new LambdaQueryWrapper<ChaiSku>()
                .eq(ChaiSku::getBarcode, code)
                .eq(ChaiSku::getDeleted, YesOrNo.NO.getCode())
                .select(ChaiSku::getSpuId)
                .groupBy(ChaiSku::getSpuId));
        for (ChaiSku sku : legacy) {
            if (sku.getSpuId() != null) {
                ids.add(sku.getSpuId());
            }
        }
        return new ArrayList<>(ids);
    }

    private static String normalize(String barcode) {
        if (barcode == null) {
            return "";
        }
        return barcode.trim().replaceAll("\\D", "");
    }
}
