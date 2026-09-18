package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class ChaiSkuRecyclePageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private long page;
    private long size;
    /** brand_exact / name_like / spu / none */
    private String matchType;
    private String keyword;
    private Long spuId;
    private List<ChaiSkuRecycleItemVO> list = Collections.emptyList();
}
