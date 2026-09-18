package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;

/**
 * 上架仓库（筛选下拉等）。
 */
@Data
public class ChaiWarehouseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String shortName;
}
