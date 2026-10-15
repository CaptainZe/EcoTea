package com.ecotea.api.domain.employee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("employee")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String salt;
    private String realName;
    private String mobile;
    private String wxOpenid;
    private String wxUnionid;
    private Long wxBindTime;
    /** 0停用 1启用 */
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
