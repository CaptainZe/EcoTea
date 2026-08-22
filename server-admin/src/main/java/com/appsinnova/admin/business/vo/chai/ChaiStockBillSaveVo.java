package com.appsinnova.admin.business.vo.chai;

import com.appsinnova.admin.business.domain.chai.ChaiStockBillItem;
import lombok.Data;

import java.util.List;

@Data
public class ChaiStockBillSaveVo {
    private Integer billType;
    private Integer reason;
    private Long handlerId;
    private Long fromWhId;
    private Long toWhId;
    private String remark;
    private List<ChaiStockBillItem> itemList;
}
