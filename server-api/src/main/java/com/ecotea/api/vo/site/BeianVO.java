package com.ecotea.api.vo.site;

import lombok.Data;

import java.io.Serializable;

/**
 * H5 备案页脚展示数据。
 */
@Data
public class BeianVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String icpText;
    private String icpUrl;
    private String mpsText;
    private String mpsUrl;
}
