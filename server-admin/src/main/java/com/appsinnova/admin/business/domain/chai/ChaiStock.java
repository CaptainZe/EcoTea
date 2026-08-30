package com.appsinnova.admin.business.domain.chai;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "chai_stock")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiStock implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long skuId;
    private Integer totalQty;
    /** 外观破损结存（计入 totalQty） */
    private Integer damageQty;
    private String operator;
    private Long updateTime;
    private Long createTime;

    /** 非表字段：列表批量挂载，避免与 skuId 重复映射 sku_id */
    @Transient
    private ChaiSku sku;

    /** 列表展示 */
    @Transient
    private String skuCode;
    @Transient
    private String name;
    @Transient
    private Long brand;
    @Transient
    private String brandName;
    @Transient
    private Integer starLevel;
    @Transient
    private Integer year;
    @Transient
    private Integer prodBatch;
    @Transient
    private Integer type;
    @Transient
    private Integer status;
    /** 查询：非卖品（映射 SKU.nonSale） */
    @Transient
    private Integer nonSale;
    @Transient
    private Integer deleted;
    @Transient
    private String specShow;
    @Transient
    private Long spuId;
    @Transient
    private String spuCode;

    /** 查询条件（非表字段） */
    @Transient
    private String querySpuCode;
    @Transient
    private Long queryWhId;
    /** 有库存：total_qty > 0 */
    @Transient
    private Integer queryHasQty;
}
