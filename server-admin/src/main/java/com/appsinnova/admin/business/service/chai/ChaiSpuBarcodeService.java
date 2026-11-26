package com.appsinnova.admin.business.service.chai;

import com.appsinnova.admin.business.common.constant.RedisConstant;
import com.appsinnova.admin.business.common.utils.RedisUtils;
import com.appsinnova.admin.business.common.utils.chai.ChaiBarcodeUtil;
import com.appsinnova.admin.business.domain.chai.ChaiSku;
import com.appsinnova.admin.business.domain.chai.ChaiSpu;
import com.appsinnova.admin.business.domain.chai.ChaiSpuBarcode;
import com.appsinnova.admin.business.repository.chai.ChaiSkuRepository;
import com.appsinnova.admin.business.repository.chai.ChaiSpuBarcodeRepository;
import com.appsinnova.admin.business.repository.chai.ChaiSpuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPU 多码维护（表 chai_spu_barcode）。不写旧列 chai_spu.barcode / chai_sku.barcode。
 * 写后 DEL {@code ecotea_chai_barcode_spu_{barcode}}。
 */
@Service
@RequiredArgsConstructor
public class ChaiSpuBarcodeService {

    private final ChaiSpuBarcodeRepository chaiSpuBarcodeRepository;
    private final ChaiSpuRepository chaiSpuRepository;
    private final ChaiSkuRepository chaiSkuRepository;

    public List<ChaiSpuBarcode> listBySpuId(Long spuId) {
        if (spuId == null) {
            throw new IllegalArgumentException("SPU不能为空");
        }
        return chaiSpuBarcodeRepository.findBySpuIdOrderByIdAsc(spuId);
    }

    /**
     * 条码 → SPU id（Admin 直查库；含新表 + 过渡期旧列）。不做 Redis SET。
     */
    public List<Long> resolveSpuIds(String barcode) {
        String code = normalizeDigits(barcode);
        if (!StringUtils.hasText(code)) {
            return Collections.emptyList();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (ChaiSpuBarcode row : chaiSpuBarcodeRepository.findByBarcode(code)) {
            if (row.getSpuId() != null) {
                ids.add(row.getSpuId());
            }
        }
        for (ChaiSpu spu : chaiSpuRepository.findByBarcodeAndDeleted(code, 0)) {
            if (spu.getId() != null) {
                ids.add(spu.getId());
            }
        }
        return new ArrayList<>(ids);
    }

    public List<Long> listSpuIdsByBarcodePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return Collections.emptyList();
        }
        return chaiSpuBarcodeRepository.findDistinctSpuIdsByBarcodeLike(prefix + "%");
    }

    public List<Long> listSpuIdsHavingAnyBarcode() {
        return chaiSpuBarcodeRepository.findDistinctSpuIds();
    }

    /**
     * 新增一条条码。每 SPU 最多一条 {@code 29*} 系统码；同 SPU 同码不可重复。
     */
    @Transactional
    public ChaiSpuBarcode add(Long spuId, String barcode, String operator) {
        ChaiSpu spu = requireActiveSpu(spuId);
        String code = ChaiBarcodeUtil.normalizeForSave(barcode);
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("条码不能为空");
        }
        if (ChaiBarcodeUtil.isSystem29(code)
                && chaiSpuBarcodeRepository.existsBySpuIdAndBarcodeStartingWith(spu.getId(), ChaiBarcodeUtil.INTERNAL_PREFIX)) {
            throw new IllegalArgumentException("该SPU已有29开头系统码，每SPU最多一条");
        }
        if (chaiSpuBarcodeRepository.existsBySpuIdAndBarcode(spu.getId(), code)) {
            throw new IllegalArgumentException("该SPU已存在相同条码");
        }
        return insertRow(spu.getId(), code, operator);
    }

    /**
     * 生成系统码并落库；若已有 29* 则直接返回已有码（不重复插入）。
     */
    @Transactional
    public ChaiSpuBarcode generateAndAdd(Long spuId, String operator) {
        ChaiSpu spu = requireActiveSpu(spuId);
        List<ChaiSpuBarcode> existing = chaiSpuBarcodeRepository.findBySpuIdOrderByIdAsc(spu.getId());
        for (ChaiSpuBarcode row : existing) {
            if (ChaiBarcodeUtil.isSystem29(row.getBarcode())) {
                return row;
            }
        }
        String code = ChaiBarcodeUtil.generateInternal(spu.getId());
        return add(spu.getId(), code, operator);
    }

    @Transactional
    public void delete(Long spuId, Long barcodeId) {
        requireActiveSpu(spuId);
        if (barcodeId == null) {
            throw new IllegalArgumentException("条码记录不能为空");
        }
        ChaiSpuBarcode row = chaiSpuBarcodeRepository.findById(barcodeId).orElse(null);
        if (row == null || !spuId.equals(row.getSpuId())) {
            throw new IllegalArgumentException("条码不存在或不属于该SPU");
        }
        String code = row.getBarcode();
        chaiSpuBarcodeRepository.delete(row);
        evictBarcodeCache(code);
    }

    /**
     * 旧列有值 → 新表（幂等、可重复跑）。
     * 来源：{@code chai_spu.barcode}、{@code chai_sku.barcode}；不写回旧列。
     * 同 SPU 已有相同码则跳过；已有 29* 再迁另一条 29* 则跳过。
     */
    @Transactional
    public Map<String, Integer> migrateFromLegacy(String operator) {
        int scanned = 0;
        int inserted = 0;
        int skippedExist = 0;
        int skippedInvalid = 0;
        int skippedSecond29 = 0;

        // spuId -> barcodes to try
        Map<Long, Set<String>> candidates = new LinkedHashMap<>();
        for (ChaiSpu spu : chaiSpuRepository.findAllWithLegacyBarcode()) {
            if (spu.getId() == null) {
                continue;
            }
            scanned++;
            addCandidate(candidates, spu.getId(), spu.getBarcode());
        }
        for (ChaiSku sku : chaiSkuRepository.findAllWithLegacyBarcode()) {
            if (sku.getSpuId() == null) {
                continue;
            }
            scanned++;
            addCandidate(candidates, sku.getSpuId(), sku.getBarcode());
        }

        String op = StringUtils.hasText(operator) ? operator : "migrate";
        for (Map.Entry<Long, Set<String>> e : candidates.entrySet()) {
            Long spuId = e.getKey();
            if (!chaiSpuRepository.findById(spuId).isPresent()) {
                continue;
            }
            for (String raw : e.getValue()) {
                String code;
                try {
                    code = ChaiBarcodeUtil.normalizeForSave(raw);
                } catch (IllegalArgumentException ex) {
                    skippedInvalid++;
                    continue;
                }
                if (!StringUtils.hasText(code)) {
                    skippedInvalid++;
                    continue;
                }
                if (chaiSpuBarcodeRepository.existsBySpuIdAndBarcode(spuId, code)) {
                    skippedExist++;
                    continue;
                }
                if (ChaiBarcodeUtil.isSystem29(code)
                        && chaiSpuBarcodeRepository.existsBySpuIdAndBarcodeStartingWith(
                        spuId, ChaiBarcodeUtil.INTERNAL_PREFIX)) {
                    skippedSecond29++;
                    continue;
                }
                insertRow(spuId, code, op);
                inserted++;
            }
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("scanned", scanned);
        result.put("spuGroups", candidates.size());
        result.put("inserted", inserted);
        result.put("skippedExist", skippedExist);
        result.put("skippedInvalid", skippedInvalid);
        result.put("skippedSecond29", skippedSecond29);
        return result;
    }

    /** 旧列仍有值、且对应 (spu,code) 尚未进新表的候选条数（粗估展示用）。 */
    public long countPendingLegacyMigration() {
        long pending = 0;
        for (ChaiSpu spu : chaiSpuRepository.findAllWithLegacyBarcode()) {
            if (spu.getId() == null) {
                continue;
            }
            String code = normalizeDigits(spu.getBarcode());
            if (!StringUtils.hasText(code)) {
                continue;
            }
            if (!chaiSpuBarcodeRepository.existsBySpuIdAndBarcode(spu.getId(), code)) {
                pending++;
            }
        }
        Set<String> seen = new HashSet<>();
        for (ChaiSku sku : chaiSkuRepository.findAllWithLegacyBarcode()) {
            if (sku.getSpuId() == null) {
                continue;
            }
            String code = normalizeDigits(sku.getBarcode());
            if (!StringUtils.hasText(code)) {
                continue;
            }
            String key = sku.getSpuId() + "_" + code;
            if (!seen.add(key)) {
                continue;
            }
            if (!chaiSpuBarcodeRepository.existsBySpuIdAndBarcode(sku.getSpuId(), code)) {
                pending++;
            }
        }
        return pending;
    }

    /** 写后 DEL：{@code ecotea_chai_barcode_spu_{barcode}} */
    public void evictBarcodeCache(String barcode) {
        String code = normalizeDigits(barcode);
        if (!StringUtils.hasText(code)) {
            return;
        }
        RedisUtils.delete(RedisUtils.defaultRedis(),
                String.format(RedisConstant.CHAI_BARCODE_SPU_KEY, code));
    }

    private ChaiSpuBarcode insertRow(Long spuId, String code, String operator) {
        long now = System.currentTimeMillis();
        ChaiSpuBarcode row = new ChaiSpuBarcode();
        row.setSpuId(spuId);
        row.setBarcode(code);
        row.setOperator(StringUtils.hasText(operator) ? operator : "");
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row = chaiSpuBarcodeRepository.save(row);
        evictBarcodeCache(code);
        return row;
    }

    private static void addCandidate(Map<Long, Set<String>> candidates, Long spuId, String barcode) {
        if (spuId == null || !StringUtils.hasText(barcode)) {
            return;
        }
        candidates.computeIfAbsent(spuId, k -> new LinkedHashSet<>()).add(barcode.trim());
    }

    private ChaiSpu requireActiveSpu(Long spuId) {
        if (spuId == null) {
            throw new IllegalArgumentException("SPU不能为空");
        }
        ChaiSpu spu = chaiSpuRepository.findById(spuId).orElse(null);
        if (spu == null) {
            throw new IllegalArgumentException("SPU不存在");
        }
        if (spu.getDeleted() != null && spu.getDeleted() == 1) {
            throw new IllegalArgumentException("已删除的SPU不能维护条码，请先恢复");
        }
        return spu;
    }

    private static String normalizeDigits(String barcode) {
        if (barcode == null) {
            return "";
        }
        return barcode.trim().replaceAll("\\D", "");
    }
}
