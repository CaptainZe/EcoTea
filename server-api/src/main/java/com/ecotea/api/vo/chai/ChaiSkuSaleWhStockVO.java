package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;

/**
 * 销售 SKU 分仓有货明细（仅 qty &gt; 0）。
 */
@Data
public class ChaiSkuSaleWhStockVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long whId;
    private String shortName;
    private Integer qty;
    private Integer qtyNoBag;
    private Integer qtyDamaged;
    private Integer qtyDamagedNoBag;
}
