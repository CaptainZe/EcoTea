package com.appsinnova.admin.business.domain.chai;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

/**
 * SPU 条码（一 SPU 多码），表 chai_spu_barcode。
 * barcode 不做全局唯一；同 SPU 同码唯一（uk_spu_barcode）。
 */
@Data
@Entity
@Table(name = "chai_spu_barcode")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class ChaiSpuBarcode implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long spuId;
    /** 条形码（EAN-13；一 SPU 可多码） */
    private String barcode;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
