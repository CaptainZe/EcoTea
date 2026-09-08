# 微信订阅号 · 产品需求说明（PRD）

> 文档名：`PRD_WX_MP.md`（Product Requirements Document）  
> 范围：EcoTea **微信订阅号**正式能力（api + admin 配置 + H5）。  
> 与 `server-admin` **仅共享** MySQL / Redis；不抽共用业务 jar。  
> **不做** `wx_user` 表（本期统计依赖微信公众平台后台）。

相关：`DEPLOY_API.md`、`DEPLOY_NGINX_SSL.md`、`CODING_API.md`、`TODO_REDIS_CACHE.md`（业务缓存暂缓）。

---

## 1. 目标与定位

对外定位：**茶叶二手回收 + 二次销售**（文案中不出现 EcoTea 字样）。

| 触点 | 作用 |
|------|------|
| 关注欢迎语 | 文章引导 + 回收/销售说明 + 功能说明（纯文本） |
| 自定义菜单（3） | 在售价目 / 茶叶回收 / 联系我们 → 三个 H5 |
| 关键词 | 品牌/品名查价；「客服」 |
| 同域简单官网 | `https://ecotea.cn/`（反代 api 静态 H5；业务链仍可用 `api.ecotea.cn`） |

---

## 2. 已具备（基础设施）

- [x] 域名 ICP、HTTPS（`api.ecotea.cn` / `admin.ecotea.cn`）
- [x] tea-api systemd 部署、Nginx 反代
- [x] 微信消息推送（AES）、关注/文本回调可用
- [x] H5 通用备案页脚（`GET /site/beian` + `site-footer.js`）
- [x] 销售只读查询能力（`GET /chai/sku/sale/list`，后续按本 PRD 扩展库存）

---

## 3. 数据与配置

### 3.1 `wx_global_config`（通用配置）

按 `type` 唯一一行；`config` 为**尽量扁平**的 JSON。  
DDL 由运维自行执行（见下方建表语句）。枚举：admin / api 均为 `WxGlobalConfigType`。

| type | 枚举 | 用途 | config（扁平） |
|------|------|------|----------------|
| 1 | `SUBSCRIBE_WELCOME` | 关注欢迎语 | `{"text":"..."}` |
| 2 | `RECYCLE_DESC` | 回收说明 | `{"title","body","cta_text","cta_url"}` |
| 3 | `CUSTOMER_SERVICE` | 客服 | `{"items":[{"wechat_id","qr_image_url"},...]}`（微信号=二维码说明，均必填，可多项） |
| 4 | `SALE_H5_COPY` | 销售 H5 文案 | `{"notice_title","notice_body","banner_text"}` |

```sql
CREATE TABLE `wx_global_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '配置类型',
  `config` text COMMENT '配置内容',
  `operator` varchar(255) NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_key` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='微信-通用配置表';
```

- **admin**：按 type 编辑；**无**「发布到微信菜单」按钮。  
  - 页面：`/business/wx/globalConfig/index`  
  - 权限：`business:wx:globalConfig:index` / `edit` / `delete`  
  - 字典：`WX_GLOBAL_CONFIG_TYPE`（须在系统字典配置，见下）  
  - 各 type 使用 `@Transient` 强类型配置对象（如 `subscribeWelcomeConfig` / `saleH5CopyConfig`），表单分块编辑后序列化进 `config`（JsonUtils SNAKE_CASE）  
- **api**：只读；关注/H5 使用。  
  - 销售文案：`GET /wx/globalConfig/saleH5Copy`  
- 欢迎语为**纯文本**（可用【】、—— 分段）；微信被动文本**不支持** HTML 加粗/变色，但可用 `<a href="https://...">文案</a>` 做可点击文字链（不露完整 URL）。

**上线配置（admin 手工一次）：**

1. 系统字典新增 `WX_GLOBAL_CONFIG_TYPE`，值示例：  
   `1:关注欢迎语,2:回收说明,3:客服,4:销售H5文案`
2. 菜单：业务 → 微信 → 「通用配置」  
   - 菜单 URL：`/business/wx/globalConfig/index`，权限 `business:wx:globalConfig:index`  
   - 按钮：编辑 `business:wx:globalConfig:edit`；删除 `business:wx:globalConfig:delete`  
3. 角色勾选上述菜单后刷新权限

### 3.2 `wx_mp_menu`（自定义菜单，独立表）

建表语句如下（自行在库中执行）。

```sql
CREATE TABLE `wx_mp_menu` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` varchar(255) NOT NULL COMMENT 'AppID',
  `app_name` varchar(255) NOT NULL COMMENT '订阅号名',
  `config` text COMMENT '配置内容',
  `operator` varchar(255) NOT NULL DEFAULT '' COMMENT '最后操作人',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '更新时间',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_key` (`app_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='微信-订阅号自定义菜单配置';
```

- `config`：微信 `menu/create` 所需 JSON（含 `button` 数组）。  
- **admin 独立页面**维护；「发布到微信」→ admin 服务端 HTTP 调 api（Header `X-EcoTea-Api-Key`）→ api 读同表 `config` 后 `menuCreate`。  
- 不放入 `wx_global_config`。

**admin：**

- 页面：`/business/wx/mpMenu/index`  
- 权限：`business:wx:mpMenu:index` / `edit` / `publish` / `delete`  
- 新建默认填入三个 view：在售价目 / 茶叶回收 / 联系我们（§4 URL）

**api：**

- `POST /wx/mp/menu/publish`（body：`{ "id" }` 或 `{ "appId" }`），须 Api-Key  
- 配置：`ecotea.security.api-key`；admin：`ecotea.api.base-url` + `ecotea.api.api-key`

**上线配置（admin 手工一次）：**

1. 菜单：业务 → 微信 → 「自定义菜单」  
   - URL：`/business/wx/mpMenu/index`，权限 `business:wx:mpMenu:index`  
   - 按钮：编辑 `business:wx:mpMenu:edit`；发布 `business:wx:mpMenu:publish`；删除 `business:wx:mpMenu:delete`  
2. 角色勾选后刷新权限；两侧配置同一 Api-Key 后保存菜单再点发布  

### 3.3 `wx_user`

**本期不做。**

---

## 4. 三个 H5 + 菜单

| 菜单名 | 类型 | URL |
|--------|------|-----|
| 在售价目 | view | `https://api.ecotea.cn/h5/chai/sale.html` |
| 茶叶回收 | view | `https://api.ecotea.cn/h5/chai/recycle.html` |
| 联系我们 | view | `https://api.ecotea.cn/h5/about.html` |

### 4.1 销售 `sale.html`

- 条件：`status=上架`、`deleted=0`、**全仓 `totalQty > 0`**  
- 排序：与 admin skuView 一致 — `year DESC, prodBatch DESC, id DESC`  
- 展示：现有销售信息 + **库存总数**；若 `damageQty > 0` 则展示**破损数量**  
- **看同款**：`sameSpuSaleCount > 1` 时展示；`?spuId=` 筛选同 SPU 有货 SKU  
- 不露回收价  
- 关键词匹配：品牌名完全匹配 → 否则 SKU 名称模糊  
- 弹窗/顶栏文案来自 `wx_global_config` type=4（`notice_title`/`notice_body` 弹窗，`banner_text` 顶栏）  
- API：`GET /chai/sku/sale/list?keyword=&spuId=&page=&size=`；文案 `GET /wx/globalConfig/saleH5Copy`  
- 页面：`https://api.ecotea.cn/h5/chai/sale.html`  
- 旧试跑页 `price.html`：**已删除**，不再保留  

### 4.2 回收 `recycle.html`

- 回收说明 + 引流（加微信 / 跳转联系我们）  
- 文案来自 `wx_global_config` type=2：`title` / `body` / `cta_text` / `cta_url`  
- API：`GET /wx/globalConfig/recycleDesc`  
- 页面：`https://api.ecotea.cn/h5/chai/recycle.html`  

### 4.3 联系我们 `about.html`

- 客服微信号 / 二维码（配置驱动，type=3 `items`）  
- API：`GET /wx/globalConfig/customerService`  
- 页面：`https://api.ecotea.cn/h5/about.html`  

### 4.4 官网

- `https://api.ecotea.cn/h5/index.html` 或根域 `https://ecotea.cn/`（Nginx 反代 api，见 `DEPLOY_NGINX_SSL.md`）  
- 同域、不新开业务域名证书以外：根域另需 `ecotea.cn` 证书（公安备案）；介绍 + 入口链到回收/联系 H5 + 备案页脚（`site-footer.js`）  
- 静态页：`static/h5/index.html` + `assets/css/site-home.css`；文案不出现 EcoTea；首页**不**放在售价目入口  

三页及官网均挂备案页脚组件。

---

## 5. 欢迎语与关键词

### 5.1 关注欢迎语

须覆盖：

1. 公众号**文章**引导  
2. **回收 + 销售**定位说明  
3. **功能说明**（发品牌/品名查价、点菜单）  

被动回复仅一条 TEXT；客服二维码放「联系我们」H5（或关键词「客服」），不塞进欢迎语 IMAGE（避免挤掉长文案）。

### 5.2 关键词

| 输入 | 行为 |
|------|------|
| 品牌全称 / 品名 | 查可售列表，被动回复最多 **10** 条摘要；更多/过长 → a 标签链到 `sale.html?keyword=` |
| 客服（等约定词） | 短文案 + a 标签链到「联系我们」H5 |

实现（api）：

- `TextMsgHandler` → `WxKeywordReplyService`
- 客服约定词（整句精确）：`客服` / `联系我们` / `人工` / `联系客服` / `人工客服`
- 查价复用 `ChaiSkuSaleQueryService`（有货）；链接根地址 `ecotea.site.public-base-url`（默认 `https://api.ecotea.cn`）
- 客服回复可读 `wx_global_config` type=3（微信号/备注）+ `/h5/about.html`（页在步骤 6）

---

## 6. api 鉴权

| 接口 | 鉴权 |
|------|------|
| `/wx/mp/**` | 微信签名 |
| 公开只读（销售列表、beian、H5 静态、公开配置读） | 可不登录 |
| 管理写操作（如发布菜单） | **必须**：如 Header `X-EcoTea-Api-Key`（与 env / 密钥配置一致）；**禁止**浏览器直连微信 |

admin「发布菜单」：admin 服务端（或受控请求）调用 api，由 api 持有微信 token 执行 `menu/create`。

---

## 7. 明确不做（本期）

- `wx_user`  
- H5 登录 / 会员价  
- 对外暴露回收价、成本  
- 抽 admin/api 共用业务 jar  
- 业务 Redis 缓存（见 `TODO_REDIS_CACHE.md`，另排期）  
- 另开营销独立域名/新站点（根域 `ecotea.cn` 仅作官网入口与公安备案，内容仍为 api H5）  
- 欢迎语 HTML 富文本推送到微信  

---

## 8. 分步实现

| 步骤 | 内容 | 状态 |
|------|------|------|
| 0 | 本 PRD + 清理试跑文档/无用 Controller/`price.html` | 已完成 |
| 1 | 建表 `wx_global_config`、`wx_mp_menu`；定 type 枚举 | 已完成 |
| 2 | admin：全局配置（先欢迎语） | 已完成 |
| 3 | api：`SubscribeHandler` 读欢迎语配置 | 已完成 |
| 4 | 销售 API 扩展库存/破损 + `sale.html` | 已完成 |
| 5 | 关键词：品牌/品名 + 客服 | 已完成 |
| 6 | `recycle.html` + `about.html` + 对应配置 | 已完成 |
| 7 | admin 菜单页 + api 鉴权发布 | 已完成 |
| 8 | `h5/index.html` 同域官网 | 已完成 |
| 9 | 业务 Redis 缓存（暂缓） | 暂缓 |

依赖：`4 → 5`；`6 → 7`（三 URL 齐后再正式发菜单）；`2 → 3`。

---

## 9. 验收要点（上线后）

1. 关注欢迎语三段信息齐全、无 EcoTea、真机可读。  
2. `sale.html` 仅有库存 SKU，破损数在有破损时可见。  
3. 关键词查价 ≤10 条并带 H5 链接；「客服」可达联系页。  
4. 三菜单跳转正确；admin 发布后真机菜单更新。  
5. `h5/index.html` 与各 H5 页脚备案号正常。  
6. 发布菜单等管理接口无密钥不可调。  
