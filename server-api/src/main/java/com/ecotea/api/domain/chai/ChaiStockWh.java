package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 分仓结存 chai_stock_wh（与 admin 对齐）。
 */
@Data
@TableName("chai_stock_wh")
public class ChaiStockWh implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockId;
    private Long whId;
    private Integer qty;
    /** 外观破损结存（计入 qty） */
    private Integer damageQty;
    private Integer version;
}
