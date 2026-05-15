package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合作方类型，字典 TEA_PARTNER_TYPE
 */
@Getter
@AllArgsConstructor
public enum TeaPartnerType {
    MERCHANT(100, "商家"),
    PERSONAL(200, "个人"),
    ;

    private final Integer code;
    private final String message;
}
