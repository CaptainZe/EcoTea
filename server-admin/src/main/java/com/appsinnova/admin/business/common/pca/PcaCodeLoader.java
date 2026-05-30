package com.appsinnova.admin.business.common.pca;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 从 classpath 加载 pca-code.json，仅包内使用。
 */
final class PcaCodeLoader {

    /** 与 resources/json/pca-code.json 对应，勿在业务代码中直接读该文件 */
    static final String PCA_JSON_CLASSPATH = "json/pca-code.json";

    private static final ObjectMapper PCA_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private PcaCodeLoader() {
    }

    static PcaCodeCatalog load() {
        ClassPathResource resource = new ClassPathResource(PCA_JSON_CLASSPATH);
        if (!resource.exists()) {
            throw new IllegalStateException("未找到省市区数据文件: " + PCA_JSON_CLASSPATH);
        }
        try (InputStream in = resource.getInputStream()) {
            List<PcaNode> provinces = PCA_MAPPER.readValue(in, new TypeReference<List<PcaNode>>() {
            });
            return PcaCodeCatalog.build(provinces);
        } catch (IOException e) {
            throw new IllegalStateException("加载省市区数据失败: " + PCA_JSON_CLASSPATH, e);
        }
    }
}
