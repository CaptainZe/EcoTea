package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.chai.ChaiSkuInfoQueryService;
import com.ecotea.api.vo.chai.ChaiSkuInfoItemVO;
import com.ecotea.api.vo.chai.ChaiSkuInfoPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 茶品信息（INFO）只读接口：仅上架未删、含无货、无库存字段；同时返回售价与回收价。
 */
@RestController
@RequestMapping("/chai/sku/info")
@RequiredArgsConstructor
public class ChaiSkuInfoController {

    private final ChaiSkuInfoQueryService chaiSkuInfoQueryService;

    /**
     * 上架未删 SKU 分页。keyword：品牌精确优先，否则名称模糊；barcode：条码精确；spuId：同款。
     * brandIds / types：逗号分隔多选；
     * priceMin / priceMax：售价区间；recyclePriceMin / recyclePriceMax：回收价区间；
     * officialPriceMin / officialPriceMax：官方价区间；
     * nonSale：1 只看非卖品 / 0 不看非卖品 / 不传不限（只看非卖品时忽略官方价区间）；
     * year / prodBatch：精确匹配。
     * 不含库存与分仓。
     */
    @GetMapping("/list")
    public ApiResult<ChaiSkuInfoPageVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) Long spuId,
            @RequestParam(required = false) String brandIds,
            @RequestParam(required = false) String types,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) BigDecimal recyclePriceMin,
            @RequestParam(required = false) BigDecimal recyclePriceMax,
            @RequestParam(required = false) BigDecimal officialPriceMin,
            @RequestParam(required = false) BigDecimal officialPriceMax,
            @RequestParam(required = false) Integer nonSale,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer prodBatch,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(chaiSkuInfoQueryService.pageInfoList(
                keyword, barcode, spuId, parseLongIds(brandIds), parseIntIds(types),
                priceMin, priceMax, recyclePriceMin, recyclePriceMax,
                officialPriceMin, officialPriceMax, nonSale, year, prodBatch,
                page, size));
    }

    /**
     * 上架未删 SKU 详情（无库存、无分仓）。
     */
    @GetMapping("/detail")
    public ApiResult<ChaiSkuInfoItemVO> detail(@RequestParam Long id) {
        ChaiSkuInfoItemVO vo = chaiSkuInfoQueryService.getInfoDetail(id);
        if (vo == null) {
            return ApiResult.fail("商品不存在或暂不可查");
        }
        return ApiResult.ok(vo);
    }

    private static List<Long> parseLongIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String[] parts = raw.split(",");
        List<Long> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            String s = part == null ? "" : part.trim();
            if (!StringUtils.hasText(s)) {
                continue;
            }
            try {
                list.add(Long.valueOf(s));
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        return list;
    }

    private static List<Integer> parseIntIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String[] parts = raw.split(",");
        List<Integer> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            String s = part == null ? "" : part.trim();
            if (!StringUtils.hasText(s)) {
                continue;
            }
            try {
                list.add(Integer.valueOf(s));
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        return list;
    }
}
