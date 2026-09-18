package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 仓库 chai_warehouse（与 admin 对齐）。
 */
@Data
@TableName("chai_warehouse")
public class ChaiWarehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 简称，必填 */
    private String shortName;
    private String province;
    private String city;
    private String district;
    private String address;
    private Integer orderNum;
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
