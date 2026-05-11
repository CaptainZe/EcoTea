package com.appsinnova.admin.business.vo.tea;

import lombok.Data;

@Data
public class TeaQuoteSkuQuoteVo {
    private Long skuId;
    /** 字典 TEA_APPEARANCE_CONDITION，与 AppearanceCondition 编码一致 */
    private Integer appearanceCondition;
    /** 字典 TEA_HAS_BAG，与 HasBag 编码一致 */
    private Integer hasBag;
    private Integer quantity;
}
