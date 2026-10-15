package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 品牌选页：常用快捷区 + 全量字母分组 + 右侧索引。
 * <p>常用品牌仍出现在字母分组中，便于全量浏览与索引定位。</p>
 */
@Data
public class ChaiBrandPickerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** common_flag=1，快捷选择（可为空） */
    private List<ChaiBrandPickerItemVO> common = new ArrayList<>();
    /** 全部上架品牌按 nameInitial 分组（含常用） */
    private List<ChaiBrandPickerGroupVO> groups = new ArrayList<>();
    /** 右侧锚点：有常用时前插「常」，再跟各组 initial */
    private List<String> index = new ArrayList<>();
}
