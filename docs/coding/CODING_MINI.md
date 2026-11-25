# 内部小程序编码规范（CODING_MINI）

> **工程**：`wx/wx-mini-inner/`（`miniprogram/`）  
> **用途**：约束本小程序的目录、请求、会话、环境与权限边界；非产品说明。  
> **产品**：[`docs/prd/PRD_WX_MINI.md`](../prd/PRD_WX_MINI.md)  
> **UI**：[`docs/ui/UI_SPEC_WX_MINI_INNER.md`](../ui/UI_SPEC_WX_MINI_INNER.md)（先规范、后实现）  
> **发版**：[`docs/changelog/CHANGELOG_WX_MINI_INNER.md`](../changelog/CHANGELOG_WX_MINI_INNER.md)  
> **后端**：只调 `server-api`；员工会话与踢下线见 [`CODING_API.md`](CODING_API.md)、[`CACHE.md`](CACHE.md)。

---

## 1. 范围与边界

- **仅**内部作业小程序；C 端或其它小程序另立工程与文档，不混用本规范 / UI。  
- **禁止**直连 `server-admin`、禁止在前端存放 AppSecret / 数据库凭证。  
- 与 API：**共享业务约定与 HTTPS 域名**；鉴权使用员工 token（`X-Employee-Token`），不是 Admin Shiro。  
- 视觉与通用组件变更：先改 `UI_SPEC_WX_MINI_INNER.md`，再改代码。

---

## 2. 技术栈

| 项 | 约定 |
|----|------|
| 框架 | 微信原生小程序 |
| 语言 | TypeScript（`miniprogram/**/*.ts`） |
| UI | 原生组件；token / 色板见 UI 规范与 `styles/` |
| 网络 | 统一 `utils/request.ts` → `wx.request` |
| 后端 | `server-api`（`ApiResult<code, message, data>`） |

---

## 3. 目录约定

```text
wx/wx-mini-inner/
├── miniprogram/
│   ├── app.ts / app.json
│   ├── config/env.ts          # API 环境（dev / prod）
│   ├── pages/                 # 页面（boot、login、选店、工作台、价目…）
│   ├── services/              # 接口与会话（*-api.ts、session、token）
│   ├── utils/                 # 请求封装、guard、格式化、存图/报价图等
│   └── styles/                # 设计令牌等
├── typings/
└── project.config.json        # AppID；勿把 secret 写入仓库前端
```

| 层 | 放什么 | 不要放什么 |
|----|--------|------------|
| `pages/` | 页面状态、交互、跳转 | 裸 `wx.request`、写死正式域名 |
| `services/` | 按域拆的 API、token/session | 页面私有 UI 逻辑 |
| `utils/` | 纯工具、权限 guard、图片生成 | 新的业务 HTTP（应进 services） |
| `config/` | 环境基址 | 业务文案、密钥 |

新增业务页：先在 `app.json` 注册；工作台入口按 perm 显隐。

---

## 4. 请求与错误

- 一律经 `utils/request.ts`：拼 `getApiBaseUrl()`、默认 JSON、按需带 `X-Employee-Token`。  
- `auth: false` 仅用于登录等无需 token 的接口。  
- 业务码：`code === 0` 为成功；非 0 仍 resolve，由页面展示 `message`（与 API 约定一致）。  
- 网络失败 reject；401 / 会话失效时清本地 token 并回到登录（沿用现有 session 流程）。  
- **禁止**在页面内散落 `wx.request` 或硬编码 `https://api.ecotea.cn`。

---

## 5. 环境（`config/env.ts`）

| 值 | 用途 |
|----|------|
| `dev` | 默认；局域网 API（真机需同网段、放行端口） |
| `prod` | `https://api.ecotea.cn`；**提审 / 正式包必须** |

- 开发保持 `CURRENT_ENV = 'dev'`。  
- 上传体验/正式前改为 `prod`，上传后改回 `dev`（见 changelog checklist）。  
- 合法域名、隐私（相册/摄像头）在微信公众平台配置，不在本文展开。

---

## 6. 登录、选店与会话

- 启动分流：`pages/boot` → `services/session.resolveLaunchTarget`（无 token → 登录；未选店 → 选店；否则首页）。  
- Token 存本地（`services/token`）；选店后会话含当前 `tenantId` 与 `permCodes`。  
- Admin 改员工密码 / 状态 / 挂店 / perm 后，服务端会踢会话（`CACHE.md`）；小程序须能接受旧 token 失效并重新登录。  
- 退出：清 token 与本地会话附属数据，再进登录页。

---

## 7. 权限（perm）

- 权限码来自当前店会话的 `permCodes`（如 `SALE` / `RECYCLE` / `INFO` / `SHOP`）。  
- **工作台 / 首页**：按 perm 过滤入口（无权限不展示）。  
- **业务页**：进入时用对应 `*-guard` 或页内校验（如 `assertInfoAccess`、`assertRecycleAccess`、售卖页查 `SALE`）；无权限提示并返回，不依赖「藏入口」 alone。  
- 新增模块：同时补工作台配置 + 页级 guard +（如需）服务端接口鉴权。

---

## 8. UI 与资源

- 色板、间距、组件形态以 `UI_SPEC_WX_MINI_INNER.md` 为准；代码侧 token 与文档同步。  
- 不对齐对内 H5 的 1:1 排版；可复用字段与信息架构。  
- 静态资源与存图：走现有 `utils/assets`、`utils/album` 等约定，注意隐私声明。

---

## 9. 禁止与注意

- 禁止把 `secret`、数据库口令写入小程序或 `project.config.json` 可公开字段。  
- 禁止绕过 `request` / `env` 私接 API。  
- 扫码查货等跨端规则以 [`docs/prd/features/`](../prd/features/) 为准（如条码），本规范不重复产品细则。  
- 报价图分页、列表筛选等交互变更：产品看 PRD，视觉看 UI_SPEC，发版记 changelog。

---

## 10. 自检清单（改码前）

- [ ] 是否只改 `wx-mini-inner`，未误改其它小程序工程  
- [ ] 新接口是否进 `services/*`，页面是否仍走 `request`  
- [ ] 是否需要新 perm / guard / 工作台入口  
- [ ] UI 变更是否已更新或符合 `UI_SPEC`  
- [ ] 是否误把 `CURRENT_ENV` 提交成 `prod`（日常 MR 应为 `dev`）  

---

## 11. 文档索引

| 文档 | 内容 |
|------|------|
| **CODING_MINI.md**（本文） | 小程序工程约定 |
| `CODING_API.md` | 后端 API、员工登录与会话 |
| `CACHE.md` | Redis；含踢小程序会话 |
| `PRD_WX_MINI.md` | 产品能力 |
| `UI_SPEC_WX_MINI_INNER.md` | UI 规范 |
| `CHANGELOG_WX_MINI_INNER.md` | 版本与提审 checklist |
| `prd/features/*.md` | 跨端功能（如条码） |
