package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * SPU 条码（一 SPU 多码），表 chai_spu_barcode。
 * barcode 不做全局唯一；同 SPU 同码唯一。
 */
@Data
@TableName("chai_spu_barcode")
public class ChaiSpuBarcode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    /** 条形码（EAN-13；一 SPU 可多码） */
    private String barcode;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
