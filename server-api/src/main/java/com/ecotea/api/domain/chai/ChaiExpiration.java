package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 保质期，表 chai_expiration（与 admin 持久化字段对齐）。
 */
@Data
@TableName("chai_expiration")
public class ChaiExpiration implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer months;
    private Integer orderNum;
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
