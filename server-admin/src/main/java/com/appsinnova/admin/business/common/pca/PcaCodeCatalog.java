package com.appsinnova.admin.business.common.pca;

import com.appsinnova.admin.business.vo.base.PcaOptionVo;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 省市区编码目录（内存索引，由 {@link PcaCodeLoader} 构建，对外只通过 {@link PcaCodeService} 访问）。
 */
final class PcaCodeCatalog {

    private final List<PcaNode> provinces;
    private final Map<String, PcaNode> nodeByCode;
    private final Map<String, String> parentCodeByCode;

    private PcaCodeCatalog(List<PcaNode> provinces, Map<String, PcaNode> nodeByCode, Map<String, String> parentCodeByCode) {
        this.provinces = Collections.unmodifiableList(provinces);
        this.nodeByCode = Collections.unmodifiableMap(nodeByCode);
        this.parentCodeByCode = Collections.unmodifiableMap(parentCodeByCode);
    }

    static PcaCodeCatalog build(List<PcaNode> provinceNodes) {
        Map<String, PcaNode> nodeByCode = new HashMap<>();
        Map<String, String> parentCodeByCode = new HashMap<>();
        if (provinceNodes != null) {
            for (PcaNode province : provinceNodes) {
                indexNode(province, null, nodeByCode, parentCodeByCode);
            }
        }
        List<PcaNode> provinces = provinceNodes == null ? Collections.emptyList() : provinceNodes;
        return new PcaCodeCatalog(provinces, nodeByCode, parentCodeByCode);
    }

    private static void indexNode(PcaNode node, String parentCode, Map<String, PcaNode> nodeByCode, Map<String, String> parentCodeByCode) {
        if (node == null || !StringUtils.hasText(node.getCode())) {
            return;
        }
        if (nodeByCode.containsKey(node.getCode())) {
            throw new IllegalStateException("重复的 PCA 编码: " + node.getCode());
        }
        nodeByCode.put(node.getCode(), node);
        if (parentCode != null) {
            parentCodeByCode.put(node.getCode(), parentCode);
        }
        for (PcaNode child : node.safeChildren()) {
            indexNode(child, node.getCode(), nodeByCode, parentCodeByCode);
        }
    }

    List<PcaOptionVo> listProvinces() {
        return provinces.stream().map(this::toOption).collect(Collectors.toList());
    }

    List<PcaOptionVo> listChildren(String parentCode) {
        PcaNode parent = nodeByCode.get(parentCode);
        if (parent == null) {
            return Collections.emptyList();
        }
        return parent.safeChildren().stream().map(this::toOption).collect(Collectors.toList());
    }

    String getName(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        PcaNode node = nodeByCode.get(code.trim());
        return node == null ? null : node.getName();
    }

    boolean contains(String code) {
        return StringUtils.hasText(code) && nodeByCode.containsKey(code.trim());
    }

    /**
     * @return 错误信息；合法时返回 null
     */
    String validateRegion(String provinceCode, String cityCode, String districtCode) {
        if (!StringUtils.hasText(provinceCode)) {
            return "省必选";
        }
        if (!StringUtils.hasText(cityCode)) {
            return "市必选";
        }
        if (!StringUtils.hasText(districtCode)) {
            return "区/县必选";
        }
        String p = provinceCode.trim();
        String c = cityCode.trim();
        String d = districtCode.trim();
        if (!contains(p)) {
            return "省份编码无效";
        }
        if (!parentCodeByCode.getOrDefault(c, "").equals(p)) {
            return "所选市不属于该省";
        }
        if (!parentCodeByCode.getOrDefault(d, "").equals(c)) {
            return "所选区/县不属于该市";
        }
        return null;
    }

    private PcaOptionVo toOption(PcaNode node) {
        return new PcaOptionVo(node.getCode(), node.getName());
    }
}
