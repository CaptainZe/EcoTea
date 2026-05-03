package com.appsinnova.admin.business.vo.tea;

import com.appsinnova.admin.business.vo.base.PayInfoVo;
import lombok.Data;

@Data
public class TeaQuoteOrderSupplementVo {
    private Integer expressCompany;
    private String expressNo;
    private Integer payMethod;
    private PayInfoVo payInfo;
}
