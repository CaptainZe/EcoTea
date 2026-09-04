package com.ecotea.api.domain.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信通用配置表 wx_global_config（与 admin 同表）。
 */
@Data
@TableName("wx_global_config")
public class WxGlobalConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 见 WxGlobalConfigType */
    private Integer type;

    /** 扁平 JSON 字符串 */
    private String config;

    private String operator;
    private Long updateTime;
    private Long createTime;
}
