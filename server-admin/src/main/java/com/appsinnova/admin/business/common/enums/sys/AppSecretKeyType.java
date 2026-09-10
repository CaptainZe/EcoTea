package com.appsinnova.admin.business.common.enums.sys;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppSecretKeyType {
    ALI_OSS(1, "阿里云OSS"),

    TEA_ROBOT_PRICE_CHANGE(100, "机器人-茶类价格变动通知"),
    ;

    private Integer code;
    private String message;
}