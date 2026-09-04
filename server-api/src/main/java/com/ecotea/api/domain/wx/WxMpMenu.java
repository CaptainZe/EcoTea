package com.ecotea.api.domain.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信订阅号自定义菜单 wx_mp_menu（与 admin 同表）。
 */
@Data
@TableName("wx_mp_menu")
public class WxMpMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appId;
    private String appName;
    /** 微信 menu/create JSON，含 button 数组 */
    private String config;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
