package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 内部回收价目 ChaiSku 展示（含回收价与库存；不含销售特价逻辑）。
 */
@Data
public class ChaiSkuRecycleItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long spuId;
    private String skuCode;
    /** 标题：品牌 + 品名 */
    private String title;
    private String name;
    private String brandName;
    private String gradeName;
    private String specShow;
    private String prodBatchShow;
    private String expirationName;

    private BigDecimal officialPrice;
    /** 如：非卖品 / 1000元 */
    private String officialPriceShow;

    private BigDecimal recyclePrice;
    /** 如：268元(3.8折) */
    private String recyclePriceShow;
    /** 如：3.8折；无有效折扣时为 null */
    private String recycleDiscountShow;

    /** 破损压价百分比 */
    private Integer recyclePriceReducePer;
    /** 破损后回收价 */
    private BigDecimal recycleDamagePrice;
    /** 如：200元；无法计算时为 null */
    private String recycleDamagePriceShow;

    /** 无提袋扣减金额（元） */
    private BigDecimal recyclePriceReduceNoBag;
    /** 如：10；无则空串/null，前端展示「扣 ¥yy」 */
    private String recyclePriceReduceNoBagShow;

    private Integer totalQty;
    private Integer damageQty;

    /**
     * 分仓有货明细（仅 qty>0）。详情填充；列表一般为空。
     */
    private List<ChaiSkuSaleWhStockVO> warehouseStocks;

    /** 同 SPU 下上架未删 SKU 数（含自身，不要求有货）；>1 可展示「看同款」 */
    private Integer sameSpuCount;

    private String coverImage;
    private List<String> imageUrls;
}
