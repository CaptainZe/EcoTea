package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.vo.chai.ChaiSkuSalePageVO;
import com.ecotea.api.service.chai.ChaiSkuSaleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ChaiSku 销售只读接口（H5 / 后续微信价目）。
 */
@RestController
@RequestMapping("/chai/sku/sale")
@RequiredArgsConstructor
public class ChaiSkuSaleController {

    private final ChaiSkuSaleQueryService chaiSkuSaleQueryService;

    /**
     * 上架 SKU 分页列表。keyword：品牌名完全匹配优先，否则名称模糊。
     */
    @GetMapping("/list")
    public ApiResult<ChaiSkuSalePageVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(chaiSkuSaleQueryService.pageSaleList(keyword, page, size));
    }
}
