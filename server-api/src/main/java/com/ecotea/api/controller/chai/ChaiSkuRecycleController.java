package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.chai.ChaiSkuRecycleQueryService;
import com.ecotea.api.vo.chai.ChaiSkuRecycleItemVO;
import com.ecotea.api.vo.chai.ChaiSkuRecyclePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ChaiSku 内部回收价目只读接口（不要求有货；不暴露到公开销售页）。
 */
@RestController
@RequestMapping("/chai/sku/recycle")
@RequiredArgsConstructor
public class ChaiSkuRecycleController {

    private final ChaiSkuRecycleQueryService chaiSkuRecycleQueryService;

    /**
     * 上架未删 SKU 分页。keyword：品牌精确优先，否则名称模糊；spuId：同款。
     * brandIds / types：逗号分隔多选。
     * 返回回收价/破损价/无袋扣减与库存数量；列表不含分仓明细。
     */
    @GetMapping("/list")
    public ApiResult<ChaiSkuRecyclePageVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long spuId,
            @RequestParam(required = false) String brandIds,
            @RequestParam(required = false) String types,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(chaiSkuRecycleQueryService.pageRecycleList(
                keyword, spuId, parseLongIds(brandIds), parseIntIds(types), page, size));
    }

    /**
     * 上架未删 SKU 详情（含分仓有货明细 warehouseStocks）。
     */
    @GetMapping("/detail")
    public ApiResult<ChaiSkuRecycleItemVO> detail(@RequestParam Long id) {
        ChaiSkuRecycleItemVO vo = chaiSkuRecycleQueryService.getRecycleDetail(id);
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
