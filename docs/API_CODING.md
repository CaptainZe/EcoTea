# server-api 编码规范

适用范围：`server-api`（包名 `com.ecotea.api`）。新增与修改代码须遵守本文档。

## 1. 范围与边界

- 与 `server-admin`：**仅共享** MySQL 库 `tea` 与 Redis；**禁止**互相依赖 jar、禁止抽共用业务模块。
- 表结构靠约定 / SQL 对齐；两边各自维护 Entity / Mapper。
- 微信相关新表前缀：`wx_`；业务表不用 `sys_`（系统字典等存量 `sys_*` 除外）。

## 2. 技术栈约定

| 项 | 约定 |
|----|------|
| JDK | 8 |
| Spring Boot | 2.7.x（当前 2.7.18） |
| ORM | MyBatis-Plus；Mapper 扫描 `com.ecotea.api.mapper` |
| 校验 | `spring-boot-starter-validation`；入参使用 `@Valid` / `@Validated` |
| 缓存 | `StringRedisTemplate`；业务 key 前缀 `ecotea_api_`（`RedisConstant.KEY_PRE`） |
| 微信 token 等 | 单独前缀（如 `wx:mp:`），勿与 `ecotea_api_` / admin 混用 |
| 日志 | `logback-spring.xml`；目录 `/data/logs/ecotea-api` |
| 环境 | 仅 `dev` / `prod`；默认端口 `8081` |

## 3. 分层与包结构

```
com.ecotea.api
├── ApiApplication
├── controller/          # HTTP 入口，按业务分子包（chai、wx、tea…）
├── service/             # 业务逻辑，按业务分子包
├── handler/             # 消息/事件 Handler，按业务分子包（如 handler.wx）
├── mapper/              # MyBatis-Plus Mapper
├── domain/              # 与表对应的实体
├── dto/ 或 req/vo/      # 入参/出参（复杂接口用；需要时再建，不提前空包）
├── config/              # 仅 Spring @Configuration
└── common/
    ├── constant/
    ├── enums/
    ├── exception/
    ├── result/
    └── utils/
```

规则：

- 调用链：`controller` → `service` → `mapper` → `domain`。
- Controller **不写**复杂业务、**不直接**调 Mapper（健康检查等脚手架例外需克制）。
- `@Configuration` **只放** `config/`，不放进 `common`。
- 横切（工具、异常、统一返回、错误码）放 `common`。

## 4. 依赖注入

- **必须**：`@RequiredArgsConstructor` + `private final` 字段注入。
- **禁止**：字段上 `@Autowired`。
- 静态工具（`RedisUtils`、`DictUtils` 等）可保留；新业务优先注入 Service，避免滥用 `SpringContextUtil.getBean`。

## 5. 接口与返回

- 统一包装：`ApiResult<T>`；成功 `code = ErrorCode.SUCCESS`（0）。
- 业务失败：抛 `BizException`，由 `GlobalExceptionHandler` 转 `ApiResult.fail`。
- 参数校验失败：同样经全局 Handler，使用 `ErrorCode.BAD_REQUEST`。
- 禁止在 Controller 里大量 `try/catch` 后手动拼失败结果（除非有明确边界转换）。
- URL：小写、与现有资源路径风格一致（如 `/chai/brand/list`）；微信回调路径须与公众平台配置一致。
- 入参校验：使用 `@Valid` / `@Validated` + JSR-303。

## 6. 命名

| 类型 | 约定 | 示例 |
|------|------|------|
| Controller | `XxxController` | `ChaiBrandController` |
| Service | `XxxService`（可不强制 Interface） | `ChaiBrandService` |
| Mapper | `XxxMapper` | `ChaiBrandMapper` |
| 实体 | 与表对应，PascalCase | `ChaiBrand` → `chai_brand` |
| 入参 | `XxxReq` / `XxxQuery` | `BrandListQuery` |
| 出参 | `XxxVO` / `XxxResp` | `BrandVO` |
| 枚举 | 业务含义清晰 | `ChaiStatus` |
| 常量 | `XxxConstant` / `ErrorCode` | `RedisConstant` |

## 7. 数据库与实体

- 实体时间字段：对齐 admin 业务表时优先 **`Long` 毫秒时间戳**；存量如 `sys_dict` 为 `Date` 则保持。
- 主键策略与表一致。
- 只映射需要的列；对外可在 VO 裁剪字段。
- 写操作在 Service 层使用 `@Transactional`（只读查询不必加）。

## 8. Redis 与锁

- 业务 key：`RedisConstant.KEY_PRE + 业务后缀`。
- `LockUtils.lock` 语义：**true = 未拿到锁**，**false = 拿到锁**。
- 多 Redis：经 `RedisTemplateHolder` 注册；默认实例名 `default`。

## 9. 日志

- 类上 `@Slf4j`；业务告警 `warn`，系统异常 `error` 并带堆栈。
- 禁止输出密钥、token、完整证件号等敏感信息。
- 微信回调可打 msgType / event / openid 摘要，避免无节制输出整包。

## 10. 异常与错误码

- 用户可感知错误用 `BizException`。
- 错误码集中在 `com.ecotea.api.common.constant.ErrorCode`，避免魔法数字。
- Handler 兜底 `Exception` → 统一文案，不把内部细节返回给调用方。
- 脚手架测试接口（`/test/util/**`）非生产能力，可保留但勿当正式 API。

## 11. 配置与密钥

- 公共配置：`application.yml`（可提交）。
- 环境配置：`application-dev.yml` / `application-prod.yml`（**不提交 git**）；仓库只提供一份 `application-env.yml.example`。
- 本地与部署：从 example 复制为对应 profile 文件后填写，或外置 `--spring.config.additional-location`。
- 不把密钥写入公开文档或 example 文件。

## 12. 改动原则（最小改动）

- 只改当前任务需要的文件；不做无关重构。
- 不主动改 `.gitignore`（除非任务明确要求）。
- 不在 `server-admin` 为 api 抽共用业务代码。
- 不需要的能力不提前引入依赖。

## 13. 微信相关（预留）

- 包约定（分层优先，与 controller/service 一致）：`controller.wx`、`handler.wx`、`config` 中 Wx 配置、`service.wx`（有业务服务时再加）。
- 回调验签失败按微信要求返回纯文本 / XML，不用业务 `ApiResult`。
- 可不落库 `wx_user`；加表需确认后再做。

## 14. 提交前自检

- [ ] 新类 DI 是否为 `@RequiredArgsConstructor` + `final`
- [ ] Controller 是否未直接依赖 Mapper（脚手架例外除外）
- [ ] 失败是否走 `BizException` / `ApiResult` / 全局校验处理
- [ ] 新错误码是否已写入 `ErrorCode`
- [ ] Redis key 是否带正确前缀
- [ ] 是否误改 admin 或引入两边共用业务模块
- [ ] 是否包含无关重构或密钥进公开文档
