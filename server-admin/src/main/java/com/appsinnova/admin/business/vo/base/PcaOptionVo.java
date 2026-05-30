package com.appsinnova.admin.business.vo.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 省市区下拉选项（编码 + 名称）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PcaOptionVo {

    private String code;
    private String name;
}
