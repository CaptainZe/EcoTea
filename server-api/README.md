# server-api

EcoTea 对外 API 服务。与 `server-admin` **仅共享数据库/Redis**，代码不互通。

编码规范见仓库根目录：[docs/API_CODING.md](../docs/API_CODING.md)。

## 技术栈

- JDK 8
- Spring Boot 2.7.18
- MyBatis-Plus
- MySQL / Redis
- Validation（JSR-303）

## 配置

- `application.yml`：公共配置（端口、MyBatis 等），可提交 git。
- `application-dev.yml` / `application-prod.yml`：**不进 git**；从 example 复制后填写：

```bash
cd server-api/src/main/resources
cp application-env.yml.example application-dev.yml
# 生产：cp application-env.yml.example application-prod.yml
# 再编辑副本，填入 MySQL / Redis / 微信等真实值
```

生产也可用外置目录：`--spring.config.additional-location=file:/data/ecotea-api/config/`。

## 启动

在本模块目录（需已有本地 `application-dev.yml` 或对应 profile）：

```bash
mvn spring-boot:run
```

或在 EcoTea 根目录：

```bash
mvn -pl server-api -am spring-boot:run
```

默认 `dev`，端口 `8081`。

- 健康检查：`GET http://localhost:8081/health`
- 品牌列表：`GET http://localhost:8081/chai/brand/list`
- 工具测试：`GET http://localhost:8081/test/util/redis` 等
