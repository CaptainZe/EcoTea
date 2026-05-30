# EcoTea 管理端 — AI 编码规范（ADMIN_CONTEXT）

> **模块**：`server-admin`（`tea-admin`）  
> **用途**：约束 AI 在本项目中的**统一编码边界与约定**；非业务需求说明。  
> **业务细节**：见 `docs/business/*.md`（按领域拆分，另行维护）。

---

## 1. 核心原则

| 原则 | 说明 |
|------|------|
| **业务 vs 框架** | 与产品/领域相关的**后端与前端**代码，一律放在 `business`；其余为 **TIMO 自带模块**，迭代中**默认不修改**，除非需求明确说明 |
| **最小改动** | 优先在 `business` 内扩展；不重构 `common` / `component` / `system` / `devtools` |
| **沿用 TIMO 约定** | 分层、权限、字典、页面结构、响应格式与现有 `system` 模块保持一致 |
| **文档分工** | 本文 = 结构与规范；具体业务流程、状态机、字段含义 → 各业务 MD |

---

## 2. 项目结构

```
EcoTea/
└── server-admin/
    └── src/main/
        ├── java/com/appsinnova/admin/
        │   ├── AdminApplication.java
        │   ├── common/          # TIMO — 勿改
        │   ├── component/       # TIMO — 勿改
        │   ├── system/          # TIMO — 通用后台（用户/角色/菜单等）
        │   ├── devtools/        # TIMO — 代码生成器
        │   └── business/        # ★ 业务代码（后端）
        └── resources/
            ├── templates/
            │   ├── common/      # TIMO — 布局 fragment
            │   ├── system/      # TIMO — 系统管理页
            │   ├── login.html, main.html
            │   └── business/    # ★ 业务页面（前端）
            └── static/          # TIMO — Layui / css / js / lib
```

### 2.1 代码归属（AI 决策表）

| 变更类型 | 放置位置 |
|----------|----------|
| 新产品功能、领域模型、业务页 | `business` + `templates/business` |
| 用户/角色/菜单/字典/部门/操作日志 | `system`（仅配置性扩展，非重写） |
| 登录、Shiro、JWT、XSS、全局异常 | `common` / `component` — **禁止改** |
| 脚手架生成 | `devtools` — **禁止改** |

---

## 3. 技术栈（约束相关）

- **Java 8** + **Spring Boot 2.0.6** + **Spring Data JPA** + **MySQL**
- **Thymeleaf** 服务端渲染 + **Layui**（TIMO 页面 class：`timo-*`）
- **Apache Shiro**：管理端登录与 `@RequiresPermissions`
- **字典**：`sys_dict` + 模板 `mo:dict` / `#dicts` + `DictUtils`
- **Ajax 响应**：`ResultVo` / `ResultVoUtil`；异常：`ResultException`

---

## 4. TIMO 模块（默认不改动）

以下包及对应前端资源为框架自带，**需求迭代时不要修改**：

| 后端包 | 前端/资源 | 职责概要 |
|--------|-----------|----------|
| `common` | — | 工具类、全局配置、异常、XSS、数据源 |
| `component` | — | Shiro、JWT、Thymeleaf 方言、操作日志、Excel、本地上传 |
| `system` | `templates/system/` | RBAC、字典、部门、操作日志、系统文件 |
| `devtools` | `templates/devtools/` | 在线代码生成 |
| — | `templates/common/`、`static/` | 公共布局与静态资源 |

**例外**：需求文档明确要求调整框架行为（如全局过滤器、登录流程）时，方可动上述模块，且改动范围应最小化。

---

## 5. `system` 模块规范

> TIMO 通用后台；新业务**不要**写入 `system`。

### 5.1 分层

`controller` → `service` / `service.impl` → `repository` → `domain`  
校验：`validator/*Valid`；页面：`templates/system/{资源}/`

### 5.2 约定

- **URL**：`/system/{资源}`（如 `/system/user`）
- **权限**：`system:{资源}:{动作}`（`index` / `add` / `edit` / `detail` / `status` 等）
- **实体表前缀**：`sys_*`
- 写操作按需加 `@ActionLog`；Ajax 必须 `@RequiresPermissions`（登录接口除外）

---

## 6. `business` 模块规范

> 所有**业务相关**后端代码放此包；**业务页面**放 `templates/business/`。  
> **不描述**各子域的业务规则，仅规定结构与命名。

### 6.1 后端包结构

```
business/
├── controller/
│   ├── file/          # 通用：OSS 上传（跨业务复用）
│   ├── sys/           # 业务向系统配置（非 TIMO system）
│   └── {domain}/      # 领域 Controller，如 tea/
├── service/
│   ├── base/          # 通用：OSS、HTTP、飞书等
│   ├── sys/
│   └── {domain}/
├── repository/
│   ├── sys/
│   └── {domain}/
├── domain/
│   ├── sys/
│   └── {domain}/
├── vo/
│   ├── base/          # 跨领域 VO
│   └── {domain}/
├── common/            # 业务内共用（非 TIMO common）
│   ├── config/
│   ├── enums/         # 可含 {domain} 子包
│   └── utils/
└── redis/             # Redis/Jedis 封装（基础设施，非领域逻辑）
```

### 6.2 当前子域划分（仅结构索引）

| 子包 | 类型 | 说明 |
|------|------|------|
| `controller.file` | **通用** | 文件上传 API，见 §7 |
| `controller.sys` / `service.sys` / … | 配置类业务 | 应用通知、密钥配置、日序号等 |
| `controller.tea` / `service.tea` / … | 领域业务 | 茶叶相关；规则见 `docs/business/tea*.md`（待建） |
| `service.base` | **通用** | OSS、外部 HTTP、飞书等，供多领域调用 |

新增领域：增加 `{domain}` 子包（如 `controller.order`），**禁止**把领域逻辑塞进 `sys` 或 `file`。

### 6.3 前端（模板）结构

与后端子包对应，路径保持一致：

```
templates/business/
├── comm/              # 业务公共片段
├── sys/               # 对应 controller.sys
└── {domain}/          # 如 tea/{功能}/index.html
```

- 页面必须引用 `templates/common/template.html` 的 header/script fragment
- 字典下拉使用 `mo:dict`；展示使用 `#dicts.keyValue`
- **禁止**在 `templates/system/` 下新增业务页

### 6.4 业务模块编码约定

| 项 | 规范 |
|----|------|
| **URL 前缀** | `/business/{域}/{资源}`，与 `sys_menu` 配置一致 |
| **权限前缀** | `business:{域}:{资源}:{动作}` |
| **分层** | 与 `system` 相同：Controller → Service → Repository → Domain |
| **实体表** | 业务表**不得**使用 `sys_` 前缀 |
| **字典** | 业务码表优先 `sys_dict` + 字典 key；领域枚举放 `business.common.enums` |
| **依赖** | 可调 `system` 的 Service（如 `UserService`、`RoleService`、`DictService`）；**禁止**反向依赖 |
| **上传** | 统一走 `UploadFileController`（§7），不重复造上传接口 |
| **配置密钥** | 第三方配置走 `app_secret_key` 等实体 + 配置项，**禁止硬编码**密钥 |

---

## 7. 通用：`business.controller.file`

业务与系统页共用的 **OSS 上传**入口，视为 `business` 下的基础设施（非 TIMO `system.UploadController` 本地上传）。

| 项 | 值 |
|----|-----|
| 类 | `UploadFileController` |
| 接口 | `POST /business/upload/file` |
| 实现 | `OssService` |
| 响应 | `ResultVo`，`code=0`，`data`: `{ src, size, md5 }` |

---

## 8. AI 编码 Checklist

**开始前**

- [ ] 确认需求属于业务还是框架；业务 → 仅改 `business` + `templates/business`
- [ ] 查是否有对应 `docs/business/*.md`；有则以其为准，不在此文档臆造规则

**编码时**

- [ ] 不修改 `common` / `component` / `devtools`，不改 `system` 除非明确为后台配置能力
- [ ] 新 Controller：`@RequestMapping` + `@RequiresPermissions` 与菜单权限字符串一致
- [ ] 新页面：放 `templates/business/...`，沿用 TIMO/Layui 结构
- [ ] Ajax 返回 `ResultVo`；不引入新的全局响应格式
- [ ] 不提交、不写死任何环境密钥或密码

**完成后**

- [ ] 业务规则变更是否需同步更新对应 `docs/business/*.md`（由人维护）

---

## 9. 文档索引

| 文档 | 内容 |
|------|------|
| **ADMIN_CONTEXT.md**（本文） | 项目结构、技术栈、TIMO/business 边界、编码规范 |
| `docs/business/*.md` | 各业务域需求与流程（如 tea 报价、SKU 等） |

---

*基于 `server-admin` 现状整理；业务域随迭代在 `business` 下扩展，框架层保持稳定。*
