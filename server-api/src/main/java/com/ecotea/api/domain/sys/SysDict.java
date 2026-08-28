package com.ecotea.api.domain.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对应 EcoTea 共享表 sys_dict
 */
@Data
@TableName("sys_dict")
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String title;
    private Byte type;
    private String value;
    private String remark;
    private Date createDate;
    private Date updateDate;
    private Byte status;
}
