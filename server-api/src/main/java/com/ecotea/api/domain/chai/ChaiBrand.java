package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("chai_brand")
public class ChaiBrand implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 品牌名称首字母（A-Z / #） */
    private String nameInitial;
    /** 品牌名称每字首字母拼接 */
    private String namePinyin;
    private String logo;
    private Integer orderNum;
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
