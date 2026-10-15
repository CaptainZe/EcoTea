package com.ecotea.api.service.wx;

import lombok.Data;

/**
 * 小程序 {@code jscode2session} 结果（不含 sessionKey，避免外泄）。
 */
@Data
public class WxMaCodeSession {

    private String openid;
    private String unionid;
}
