package com.appsinnova.admin.business.vo.base;

import lombok.Data;

/**
 * 多语言项
 */
@Data
public class LangNameVo {
    private String lang;  // 语言码
    private String text;  // 对应文案
}