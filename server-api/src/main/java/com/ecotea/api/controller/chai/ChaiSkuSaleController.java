package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.chai.ChaiSkuSaleQueryService;
import com.ecotea.api.vo.chai.ChaiSkuSaleItemVO;
import com.ecotea.api.vo.chai.ChaiSkuSalePageVO;
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
 * ChaiSku 销售只读接口（H5 / 微信价目）。
 */
@RestController
@RequestMapping("/chai/sku/sale")
@RequiredArgsConstructor
public class ChaiSkuSaleController {

    private final ChaiSkuSaleQueryService chaiSkuSaleQueryService;

    /**
     * 有货上架 SKU 分页。keyword：品牌精确优先，否则名称模糊；spuId：同款。
     * whId：按仓有货筛选；includeWh=1：列表带回有货仓简称（无数量）；
     * recycleRecent=1：近 N 日回收入库（N 见 ChaiConstant.RECYCLE_RECENT_DAYS），按最近回收倒序。
     * brandIds / types：逗号分隔多选；priceMin / priceMax：售价区间。
     */
    @GetMapping("/list")
    public ApiResult<ChaiSkuSalePageVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long spuId,
            @RequestParam(required = false) Long whId,
            @RequestParam(required = false) Integer includeWh,
            @RequestParam(required = false) Integer recycleRecent,
            @RequestParam(required = false) String brandIds,
            @RequestParam(required = false) String types,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        boolean withWhNames = includeWh != null && includeWh != 0;
        boolean recycleRecentFlag = recycleRecent != null && recycleRecent != 0;
        return ApiResult.ok(chaiSkuSaleQueryService.pageSaleList(
                keyword, spuId, whId, withWhNames, recycleRecentFlag,
                parseLongIds(brandIds), parseIntIds(types), priceMin, priceMax,
                page, size));
    }

    /**
     * 有货上架 SKU 详情（含分仓有货明细 warehouseStocks）。
     */
    @GetMapping("/detail")
    public ApiResult<ChaiSkuSaleItemVO> detail(@RequestParam Long id) {
        ChaiSkuSaleItemVO vo = chaiSkuSaleQueryService.getSaleDetail(id);
        if (vo == null) {
            return ApiResult.fail("商品不存在或暂不可售");
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
