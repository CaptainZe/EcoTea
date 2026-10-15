package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;

/**
 * 品牌选页单项（常用区 / 字母分组共用）。
 */
@Data
public class ChaiBrandPickerItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    /** A-Z / # */
    private String nameInitial;
    /** 品牌名称每字首字母拼接（前端本地搜索） */
    private String namePinyin;
    /** Logo URL，可空 */
    private String logo;
}
