package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售侧 ChaiSku 展示（不含回收价；不含原封/原盒等包装文案）。
 */
@Data
public class ChaiSkuSaleItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long spuId;
    /** 标题：品牌 + 品名 */
    private String title;
    private String name;
    private String brandName;
    private String gradeName;
    private String specShow;
    private String prodBatchShow;
    private String expirationName;
    private BigDecimal officialPrice;
    private BigDecimal salePrice;
    /** 如：非卖品 / 1000元 */
    private String officialPriceShow;
    /** 如：388元(3.88折) */
    private String salePriceShow;
    /** 如：3.5折；无有效折扣时为 null */
    private String discountShow;
    /** 全仓库存总数 */
    private Integer totalQty;
    /** 破损数量；仅 >0 时有意义，前端按需展示 */
    private Integer damageQty;
    /** 同 SPU 下有货可售 SKU 数（含自身）；>1 可展示「看同款」 */
    private Integer sameSpuSaleCount;
    /** 首图（展示图列表第一张） */
    private String coverImage;
    /** 展示图完整 URL 列表 */
    private List<String> imageUrls;
}
