package com.ecotea.api.common.constant;

/**
 * 微信相关常量（被动回复条数、H5 路径等）。
 */
public interface WxConstant {

    /** 关键词查价最多摘要条数 */
    int SALE_KEYWORD_MAX_ITEMS = 10;

    /** 被动文本建议上限（微信约 2048 字节，预留下限） */
    int TEXT_SOFT_MAX_CHARS = 550;

    String H5_SALE_PATH = "/h5/chai/sale.html";
    String H5_RECYCLE_PATH = "/h5/chai/recycle.html";
    String H5_ABOUT_PATH = "/h5/about.html";
}
