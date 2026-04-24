package com.appsinnova.admin.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppSecretKeyType {
    ALI_OSS(1, "阿里云OSS"),
    ;

    private Integer code;
    private String message;
}