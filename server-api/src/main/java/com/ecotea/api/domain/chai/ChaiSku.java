package com.ecotea.api.domain.chai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 茶饼 SKU，表 chai_sku（与 admin 持久化字段对齐；展示字段用 VO）。
 */
@Data
@TableName("chai_sku")
public class ChaiSku implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    private String skuCode;
    private Integer starLevel;
    private String name;
    /** 搜索关键词（空格分隔，运营可编） */
    private String keywords;
    /** 检索文本（品牌名+商品名+关键词，系统生成） */
    private String searchText;
    /** 【废弃】旧单列条码；业务读写切至 chai_spu_barcode */
    private String barcode;
    private Long brand;
    private Long expiration;
    private Integer type;
    private Integer grade;
    private Integer year;
    private Integer prodBatch;
    private String spec;
    private String showImageUrls;
    private String realImageUrls;
    /** 是否非卖品：0否 1是 */
    private Integer nonSale;
    /** 官方价；非卖品时为 null */
    private BigDecimal officialPrice;
    private BigDecimal salePrice;
    private BigDecimal recyclePrice;
    private Integer recyclePriceReducePer;
    private BigDecimal recyclePriceReduceNoBag;
    /** 对外隐藏：0否 1是（同步自 SPU） */
    private Integer hideExternal;
    /** 核心产品：0否 1是（同步自 SPU） */
    private Integer coreFlag;
    /** 品鉴款：0否 1是（同步自 SPU） */
    private Integer tastingFlag;
    /** 整装 SPU ID；品鉴款可空（同步自 SPU） */
    private Long packSpuId;
    private Integer status;
    /** 0有效 1已删除；不与 status（上下架）混用 */
    private Integer deleted;
    private String operator;
    private Long updateTime;
    private Long createTime;
}
