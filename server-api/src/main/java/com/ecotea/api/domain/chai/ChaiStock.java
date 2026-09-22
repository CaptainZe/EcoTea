package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 全仓库存合计 chai_stock（与 admin 对齐）。
 * 完整件数 = totalQty − qtyNoBag − qtyDamaged − qtyDamagedNoBag，不单独落库。
 */
@Data
@TableName("chai_stock")
public class ChaiStock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private Integer totalQty;
    /** 外观完整无提袋（计入 totalQty） */
    private Integer qtyNoBag;
    /** 外观破损有提袋（计入 totalQty） */
    private Integer qtyDamaged;
    /** 外观破损无提袋（计入 totalQty） */
    private Integer qtyDamagedNoBag;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
