package com.ecotea.api.controller.chai;

import com.ecotea.api.common.result.ApiResult;
import com.ecotea.api.service.chai.ChaiSkuSaleQueryService;
import com.ecotea.api.vo.chai.ChaiSkuSalePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     */
    @GetMapping("/list")
    public ApiResult<ChaiSkuSalePageVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long spuId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(chaiSkuSaleQueryService.pageSaleList(keyword, spuId, page, size));
    }
}
