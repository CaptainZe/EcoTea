package com.ecotea.api.req.employee;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/** 小程序 {@code wx.login} 得到的临时 code */
@Data
public class WxCodeReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "微信登录码必填")
    private String code;
}
