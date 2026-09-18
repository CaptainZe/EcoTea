package com.ecotea.api.common.enums.chai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存事由（与 admin ChaiStockReason / 字典 CHAI_STOCK_REASON 对齐）。
 * 入库 100–199，出库 200–299，调拨 300–399。
 */
@Getter
@AllArgsConstructor
public enum ChaiStockReason {
    RECYCLE(100, "回收", ChaiStockBillType.IN),
    RETURN(101, "退货", ChaiStockBillType.IN),
    SURPLUS(102, "盘盈", ChaiStockBillType.IN),
    FIRST_COUNT(198, "盘点(首次)", ChaiStockBillType.IN),
    OTHER_IN(199, "其它入库", ChaiStockBillType.IN),

    SALE(200, "销售", ChaiStockBillType.OUT),
    GIFT(201, "送礼", ChaiStockBillType.OUT),
    SELF_USE(202, "自用", ChaiStockBillType.OUT),
    LOSS(203, "报损", ChaiStockBillType.OUT),
    OTHER_OUT(299, "其它出库", ChaiStockBillType.OUT),

    DISPATCH(300, "调度", ChaiStockBillType.TRANSFER),
    ;

    private final Integer code;
    private final String message;
    private final ChaiStockBillType billType;
}
