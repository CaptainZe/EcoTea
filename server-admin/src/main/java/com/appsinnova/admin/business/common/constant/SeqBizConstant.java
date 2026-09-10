package com.appsinnova.admin.business.common.constant;

/**
 * 日序号业务标识（供 {@link com.appsinnova.admin.business.common.utils.RedisSeqUtils} 使用）。
 * key 形如：{@code ecotea_seq_{biz}_{yyyyMMdd}}。
 */
public interface SeqBizConstant {

    String QUOTE_ORDER = "quote_order";
    String CHAI_STOCK_IN = "chai_stock_in";
    String CHAI_STOCK_OUT = "chai_stock_out";
    String CHAI_STOCK_TRANSFER = "chai_stock_transfer";
}
