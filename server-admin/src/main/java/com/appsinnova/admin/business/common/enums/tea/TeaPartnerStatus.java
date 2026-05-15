package com.appsinnova.admin.business.common.enums.tea;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合作方状态，字典 TEA_PARTNER_STATUS
 */
@Getter
@AllArgsConstructor
public enum TeaPartnerStatus {
    SIGNED(1, "签约"),
    TERMINATED(2, "解约"),
    ;

    private final Integer code;
    private final String message;
}
