package com.appsinnova.admin.business.vo.tea;

import lombok.Data;

@Data
public class TeaQuoteSkuQuoteVo {
    private Long skuId;
    private Integer qtyCompleteBag;
    private Integer qtyCompleteNoBag;
    private Integer qtyBrokenBag;
    private Integer qtyBrokenNoBag;
}
