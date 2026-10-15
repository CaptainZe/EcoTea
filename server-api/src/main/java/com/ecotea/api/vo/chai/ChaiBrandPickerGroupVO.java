package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 品牌选页字母分组。
 */
@Data
public class ChaiBrandPickerGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A-Z / # */
    private String initial;
    private List<ChaiBrandPickerItemVO> items = new ArrayList<>();
}
