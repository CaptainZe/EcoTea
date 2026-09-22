package com.appsinnova.admin.business.domain.chai;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "chai_stock_wh")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiStockWh implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long stockId;
    private Long whId;
    private Integer qty;
    /** 外观完整无提袋（计入 qty） */
    private Integer qtyNoBag;
    /** 外观破损有提袋（计入 qty） */
    private Integer qtyDamaged;
    /** 外观破损无提袋（计入 qty） */
    private Integer qtyDamagedNoBag;
    private Integer version;

    /** 列表展示 */
    @Transient
    private String whName;
}
