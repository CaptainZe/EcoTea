package com.appsinnova.admin.business.common.pca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 对应 classpath:json/pca-code.json 单节点结构（仅供 {@link PcaCodeLoader} 反序列化，业务层请用 {@link PcaCodeService}）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PcaNode {

    private String code;
    private String name;
    private List<PcaNode> children;

    public List<PcaNode> safeChildren() {
        return children == null ? Collections.emptyList() : children;
    }
}
