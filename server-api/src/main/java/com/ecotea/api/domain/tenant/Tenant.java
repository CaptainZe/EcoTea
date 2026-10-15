package com.ecotea.api.domain.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("tenant")
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务租户 ID */
    private Long tenantId;
    private String name;
    /** 0停用 1启用 */
    private Integer status;
    private Integer employeeQuota;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contactName;
    private String contactMobile;
    private String remark;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
