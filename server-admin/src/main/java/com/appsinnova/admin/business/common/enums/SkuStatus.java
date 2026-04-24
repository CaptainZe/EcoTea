package com.appsinnova.admin.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SkuStatus {
    ONLINE(1, "上架"),
    OFFLINE(2, "下架"),
    ;

    private Integer code;
    private String message;
}