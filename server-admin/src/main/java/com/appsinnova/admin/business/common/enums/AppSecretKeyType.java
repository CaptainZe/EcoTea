package com.appsinnova.admin.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppSecretKeyType {
    ALI_OSS(1, "阿里云OSS"),
    FEISHU_APP(2, "飞书应用"),

    TEA_ROBOT_PRICE_CHANGE(100, "机器人-茶类价格变动通知"),
    TEA_ROBOT_QUOTE_ORDER(101, "机器人-报价订单通知"),
    ;

    private Integer code;
    private String message;
}