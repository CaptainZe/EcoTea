package com.ecotea.api.vo.chai;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典选项（code / text）。
 */
@Data
public class ChaiDictOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String text;
}
