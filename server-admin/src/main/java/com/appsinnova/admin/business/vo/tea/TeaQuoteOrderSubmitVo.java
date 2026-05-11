package com.appsinnova.admin.business.vo.tea;

import com.appsinnova.admin.business.vo.base.PayInfoVo;
import lombok.Data;

import java.util.List;

@Data
public class TeaQuoteOrderSubmitVo {
    private Integer type;
    private Long userId;
    private String userName;
    private Integer expressCompany;
    private String expressNo;
    private Integer payMethod;
    private PayInfoVo payInfo;
    private List<TeaQuoteSkuQuoteVo> itemList;
}
