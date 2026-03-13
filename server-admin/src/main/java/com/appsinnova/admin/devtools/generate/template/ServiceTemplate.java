package com.appsinnova.admin.devtools.generate.template;

import com.appsinnova.admin.common.enums.StatusEnum;
import com.appsinnova.admin.common.utils.ToolUtil;
import com.appsinnova.admin.devtools.generate.domain.Generate;
import com.appsinnova.admin.devtools.generate.enums.TierType;
import com.appsinnova.admin.devtools.generate.utils.FileUtil;
import com.appsinnova.admin.devtools.generate.utils.GenerateUtil;
import com.appsinnova.admin.devtools.generate.utils.jAngel.JAngelContainer;
import com.appsinnova.admin.devtools.generate.utils.jAngel.nodes.Document;
import com.appsinnova.admin.devtools.generate.utils.jAngel.parser.Expression;
import com.appsinnova.admin.devtools.generate.utils.parser.JavaParseUtil;

import java.nio.file.FileAlreadyExistsException;
import java.util.Set;

/**
 * @author 小懒虫
 * @date 2018/10/25
 */
public class ServiceTemplate {

    /**
     * 生成需要导入的包
     */
    private static Set<String> genImports(Generate generate) {
        JAngelContainer container = new JAngelContainer();
        container.importClass(JavaParseUtil.getPackage(generate, TierType.DOMAIN));
        container.importClass(StatusEnum.class);
        return container.getImports();
    }

    /**
     * 生成类体
     */
    private static Document genClazzBody(Generate generate) {
        // 构建数据-模板表达式
        Expression expression = new Expression();
        expression.label("name", ToolUtil.lowerFirst(generate.getBasic().getTableEntity()));
        expression.label("entity", generate.getBasic().getTableEntity());
        String path = FileUtil.templatePath(ServiceTemplate.class);

        // 获取jAngel文档对象
        Document document = JavaParseUtil.document(path, expression, generate, TierType.SERVICE);
        document.getContainer().importClass(genImports(generate));

        return document;
    }

    /**
     * 生成服务层模板
     */
    public static String generate(Generate generate) {
        // 生成文件
        String filePath = GenerateUtil.getJavaFilePath(generate, TierType.SERVICE);
        try {
            Document document = genClazzBody(generate);
            GenerateUtil.generateFile(filePath, document.content());
        } catch (FileAlreadyExistsException e) {
            return GenerateUtil.fileExist(filePath);
        }
        return filePath;
    }
}
