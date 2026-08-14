package com.appsinnova.admin.business.vo.chai;

import com.appsinnova.admin.business.domain.chai.ChaiSku;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChaiSkuBatchSaveVo {
    private Long spuId;
    /** 保存成功后是否同时上架 SPU（并级联全部 SKU） */
    private Boolean onlineSpu;
    private List<ChaiSku> itemList = new ArrayList<>();
}
