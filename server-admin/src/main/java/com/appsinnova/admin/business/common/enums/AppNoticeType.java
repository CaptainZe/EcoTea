package com.appsinnova.admin.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppNoticeType {
    TEA_NOTICE(1, "茶叶公告"),
    ;

    private Integer code;
    private String message;
}