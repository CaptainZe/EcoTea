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
| 缓存 | `StringRedisTemplate`；业务 key 前缀 **`ecotea_`**（`RedisConstant.KEY_PRE`，与 admin 共用同一 Redis） |
| 微信 token 等 | 单独前缀（如 `wx:mp:`），勿与 `ecotea_` 业务 key 混用 |
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
├── vo/                  # 出参/展示对象（XxxVO）
├── req/                 # 入参（复杂查询体时再建，如 XxxQuery / XxxReq）
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
- URL：小写、与现有资源路径风格一致（如 `/chai/sku/sale/list`）；微信回调路径须与公众平台配置一致。
- 入参校验：使用 `@Valid` / `@Validated` + JSR-303。

## 6. 命名

| 类型 | 约定 | 示例 |
|------|------|------|
| Controller | `XxxController` | `ChaiSkuSaleController` |
| Service | `XxxService`（可不强制 Interface） | `ChaiBrandService` |
| Mapper | `XxxMapper` | `ChaiBrandMapper` |
| 实体 | 与表对应，PascalCase | `ChaiBrand` → `chai_brand` |
| 入参 | `XxxReq` / `XxxQuery`（包 `req`） | `BrandListQuery` |
| 出参 | `XxxVO`（包 `vo`） | `ChaiSkuSaleItemVO` |
| 枚举 | 业务含义清晰 | `ChaiStatus` |
| 常量 | `XxxConstant` / `ErrorCode` | `RedisConstant` |

## 7. 数据库与实体

- 实体时间字段：对齐 admin 业务表时优先 **`Long` 毫秒时间戳**；存量如 `sys_dict` 为 `Date` 则保持。
- 主键策略与表一致。
- **`domain` 映射整张表持久化字段**，勿按接口裁剪缺列；对外裁剪放在 **`vo`**（如不返回回收价）。
- 表外展示字段（admin 的 `@Transient`）不要写进 domain。
- 写操作在 Service 层使用 `@Transactional`（只读查询不必加）。

## 8. Redis、锁与业务缓存

> 明细 key / 接入进度见 `d-docs/TODO_REDIS_CACHE.md`。本文只定**必须遵守**的约定。

### 8.1 基础设施

- 业务 key：`RedisConstant.KEY_PRE`（`ecotea_`）+ 业务后缀；用 `_` 分隔，不用 `:`。
- 常量进 `RedisConstant`；与 admin **码值一致、各自维护**（禁止共用 jar）。
- `LockUtils.lock` 语义：**true = 未拿到锁**，**false = 拿到锁**。
- 多 Redis：经 `RedisTemplateHolder` 注册；默认实例名 `default`。
- **禁止**生产用 `KEYS *` 扫删。

### 8.2 热读缓存模式（主数据）

| 端 | 行为 |
|----|------|
| **api** | Redis → miss → MySQL → SET（建议 `DAY_EXPIRE` 兜底；库无可用短 TTL 防穿透） |
| **admin** | 写库成功后 **DEL** 对应 key（立刻生效，不靠 TTL） |

### 8.3 新功能必须评估缓存

新增「读多写少」的主数据、按业务键反复查询、或跨请求复用的读路径时，设计阶段须明确：

1. 是否走 Redis；不走则写明理由（写频繁 / key 爆炸 / 强一致）。  
2. key 模板、value 形态、TTL。  
3. 写端（通常 admin）成功后的失效方式（DEL / 踢会话）。  

**不优先缓存：** Admin/API 后台分页 CRUD；高频写入的业务单据列表/详情（报价单、销售单、回收单等默认直查库；短 TTL 仅在有压测证据时再议，见 `TODO_REDIS_CACHE.md`）。

### 8.4 组织域（租户 / 员工）

| 项 | 约定 |
|----|------|
| `tenant` | 按业务键 `tenant_id` 热读缓存（规划见 TODO；**当前未接**） |
| 员工会话 | 已接：`ecotea_emp_session_{token}`、`ecotea_emp_uid_{employeeId}`，TTL 7 天 |
| 挂店 / perm | **不**另做长缓存；以会话内 `permCodes` 为准 |
| Admin 改员工 | 改密、停用、改挂店/perm 后须 **踢该员工会话**（DEL uid→token 与对应 session），避免旧权限残留 |
| Admin 改店铺 | save / 启停后 DEL 对应 `tenant` 缓存 key（接入后） |

## 9. 日志

- 类上 `@Slf4j`；业务告警 `warn`，系统异常 `error` 并带堆栈。
- 禁止输出密钥、token、完整证件号等敏感信息。
- 微信回调可打 msgType / event / openid 摘要，避免无节制输出整包。

## 10. 异常与错误码

- 用户可感知错误用 `BizException`。
- 错误码集中在 `com.ecotea.api.common.constant.ErrorCode`，避免魔法数字。
- Handler 兜底 `Exception` → 统一文案，不把内部细节返回给调用方。
- 脚手架测试接口勿当作正式 API；已移除 `/test/util/**`。

## 11. 配置与密钥

- 公共配置：`application.yml`（可提交）。
- 环境配置：`application-dev.yml` / `application-prod.yml`（**不提交 git**）；仓库只提供一份 `application-env.yml.example`。
- 本地与部署：从 example 复制为对应 profile 文件后填写，或外置 `--spring.config.additional-location`。
- 不把密钥写入公开文档或 example 文件。
- 微信：
  - 订阅号：`wx.mp.*`（现有 `wx-java-mp`）。
  - 内部小程序：`wx.miniapp.*`（WxJava `wx-java-miniapp-spring-boot-starter`；`appid` 与 `wx/wx-mini-inner/project.config.json` 一致；`secret` 仅环境 yml，勿进小程序前端）。

## 12. 改动原则（最小改动）

- 只改当前任务需要的文件；不做无关重构。
- 不主动改 `.gitignore`（除非任务明确要求）。
- 不在 `server-admin` 为 api 抽共用业务代码。
- 不需要的能力不提前引入依赖。

## 13. 租户与员工会话（D 已落地）

> 产品定调见 `d-prd/PRD_TENANT.md`。

### 13.1 包目录

| 包 | 内容 |
|----|------|
| `domain/mapper/service/tenant` | `Tenant` **全量**实体与只读服务 |
| `domain/mapper/service/controller/employee` | 员工、挂店、perm、登录会话 |
| `vo/tenant` | 轻量出参（如 `TenantOptionVO`） |
| `vo/employee` / `req/employee` | 登录会话 VO / 入参 |
| `common/enums/tenant` / `employee` | 状态与 perm 码（`SALE`/`RECYCLE`/`STOCK`/`FINANCE`/`SHOP`，与 Admin 对齐，各自维护） |

### 13.2 员工鉴权

| 项 | 约定 |
|----|------|
| Header | `X-Employee-Token` |
| 拦截 | `/employee/**`，放行 `/employee/login`、`/employee/wx-login` |
| 会话 | Redis：`ecotea_emp_session_{token}`，TTL 7 天；单点 `ecotea_emp_uid_{employeeId}` |
| 密码 | 与 Admin 一致：SHA-256 + salt + 1024（`EmployeePasswordUtil`，不引 admin jar） |

| 接口 | 鉴权 |
|------|------|
| `POST /employee/login` | 无 |
| `POST /employee/wx-login` | 无；body `{ code }` → 已绑定 openid 的员工发 token，再选店 |
| `POST /employee/logout` | token |
| `GET /employee/session` | token |
| `GET /employee/tenants` | token |
| `POST /employee/select-tenant` | token（写入工作 `tenant_id` + perm） |
| `POST /employee/change-password` | token；校验原密码后换 salt 写新密并踢会话，客户端须重新登录 |
| `POST /employee/wx-bind` | token；body `{ code }` → 绑定小程序 openid；`GET /session` 含 `wxBound` |
| `POST /employee/wx-unbind` | token；清除 openid / unionid |

- 公开 H5 `/chai/**` 仍匿名；**不**使用员工 token，也**不**使用 `X-EcoTea-Api-Key`。
- Admin 菜单发布等仍用 `X-EcoTea-Api-Key`，与员工会话分离。
- 后续写接口：须 token + 已选店，并按 `employee_tenant_perm` 校验；错误码预留 `TENANT_NOT_SELECTED=4031`。
- 租户隔离（库存加列）后置；**禁止**跨租户写。

## 14. 微信相关

- 包约定（分层优先，与 controller/service 一致）：`controller.wx`、`handler.wx`、`config` 中 Wx 配置、`service.wx`（有业务服务时再加）。
- 回调验签失败按微信要求返回纯文本 / XML，不用业务 `ApiResult`。
- 管理写操作（如 `POST /wx/mp/menu/publish`）须 Header `X-EcoTea-Api-Key`（`ecotea.security.api-key`），拦截器挂在 `/wx/mp/menu/**`。
- 可不落库 `wx_user`；加表需确认后再做。
- 小程序员工绑定 / 一键登录：
  - 配置见 §11 `wx.miniapp`；**勿**与 `wx.mp` 密钥混用。
  - 依赖：`wx-java-miniapp-spring-boot-starter`（与 mp 同 `${wxjava.version}`）。
  - 已落地：code2session；`wx-bind` / `wx-unbind` / `wx-login`；「我的」绑定/解绑；登录页「微信一键登录」。

## 15. 提交前自检

- [ ] 新类 DI 是否为 `@RequiredArgsConstructor` + `final`
- [ ] Controller 是否未直接依赖 Mapper（脚手架例外除外）
- [ ] 失败是否走 `BizException` / `ApiResult` / 全局校验处理
- [ ] 新错误码是否已写入 `ErrorCode`
- [ ] Redis key 是否带正确前缀 `ecotea_`，常量是否已进 `RedisConstant`
- [ ] 新热读主数据是否评估缓存；写路径是否约定 admin DEL / 踢会话
- [ ] 是否误改 admin 或引入两边共用业务模块
- [ ] 是否包含无关重构或密钥进公开文档
