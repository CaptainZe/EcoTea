package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;

/**
 * 上架品牌（筛选多选等）。
 */
@Data
public class ChaiBrandVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
}
