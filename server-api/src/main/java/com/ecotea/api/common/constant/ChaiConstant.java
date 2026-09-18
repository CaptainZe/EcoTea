package com.ecotea.api.common.constant;

/**
 * 茶饼业务相关常量（字典名等）。
 */
public interface ChaiConstant {

    /** 等级字典 */
    String DICT_GRADE = "CHAI_GRADE";

    /** 生产批次字典 */
    String DICT_PROD_BATCH = "CHAI_PROD_BATCH";

    /**
     * 「新回收」筛选：回收入库回溯天数（程序决定，不对用户展示具体天数文案）。
     */
    int RECYCLE_RECENT_DAYS = 3;
}
