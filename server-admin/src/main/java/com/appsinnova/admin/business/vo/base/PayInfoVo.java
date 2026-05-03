package com.appsinnova.admin.business.vo.base;

import lombok.Data;

@Data
public class PayInfoVo {

    private String wxQrCode;
    private String alipayQrCode;

    private String bankName;
    private String bankAccount;
    private String payeeName;
}
