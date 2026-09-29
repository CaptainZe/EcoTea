# SPU/SKU 条形码（barcode）待实现

> 状态：**待排期**；方案已定。  
> 目标：一 SPU 一码（SKU 继承）；支持 69 码录入 / 自生成 EAN-13（29 开头）；Admin 扫码枪查询与维护填入；标签打印；H5 摄像头扫码（内部优先）；公众号 `scanQRCode` 为可选增强。

## 背景与边界

| 点 | 结论 |
|----|------|
| 粒度 | **一 SPU 一码**；下属全部 SKU 继承同一 `barcode` |
| 唯一性 | **本期不做** DB 唯一索引（业务上尽量一码一货，冲突以后再收） |
| 国标 69 | 有则手填 / 扫枪填入维护框 |
| 店内自编码 | 无 69 时生成 **EAN-13**：`29` + `spuId` 补齐 10 位 + **校验位** |
| 编码库 | **ZXing**（Java 出图；H5 JS 解码）。不引入 Barcode4J |
| 录入入口 | **先独立维护弹层**（对齐关键词入口）；大表单可后续再加 |
| 查询 | 各需查货页：**独立「条形码」框**；命中 **1 条或多条均进二次确认列表** 再点选 |

### 设备

| 设备 | 型号 | 对接 |
|------|------|------|
| 扫码枪 | 得力 **14952W** 无线 | HID 键盘楔入（扫入≈键入+回车）；Admin **不需 SDK** |
| 标签机 | 得力 **DL-720W**（宽 20–80mm，高 25–300mm） | 第一期：**浏览器打印** + Windows 得力驱动；纸张尺寸先定（如 40×30 / 50×30） |

### H5 打开环境

| 类型 | 路径示例 | 扫码策略 |
|------|----------|----------|
| 内部 H5 | `inner-sale*` / `inner-recycle*` | **主路径**：摄像头 + ZXing JS（不依赖公众号） |
| 对外 H5 | `sale*` / `recycle*` | 本期一般不强制扫货；若加则同组件 |
| 公众号内打开 | 菜单进 H5 | 同上摄像头主路径；**可选** JSSDK `scanQRCode`（须配置，且真机测 EAN-13） |

须 **HTTPS**（或 localhost），否则摄像头常被禁；内网纯 `http://IP` 需有手动输入兜底。

---

## 字段设计

| 字段 | 表 | 谁写 | 含义 |
|------|-----|------|------|
| `barcode` | `chai_spu` | 独立维护入口 | 条形码字符串，建议 EAN-13（13 位数字） |
| `barcode` | `chai_sku` | **继承自 SPU** | 冗余便于列表 / H5 / 库存直查 |

- 类型建议：`varchar(32) NOT NULL DEFAULT ''`。  
- `copyFromSpu` / 同步共享字段 / `saveBatchForSpu` / 同步已删 SKU：强制带上 `barcode`。  
- 改 SPU 条码后同步该 SPU 下全部 SKU（含已删策略与 keywords 一致即可）。

---

## 自编码规则（EAN-13）

```text
29 + sprintf("%010d", spuId) + EAN-13校验位
例：spuId=1 → 29000000001X（X 为校验位）
```

- **不要**把业务串 `CHAI-00000001` 直接塞进中间 10 位；用数字 **`spuId`**。  
- 校验位：标准 EAN-13（奇数位×1、偶数位×3，再凑 10 的倍数）。可用自写工具类；出 PNG/标签图用 ZXing。  
- **生成时机**：必须已有 `spuId`（先保存 SPU 再生成）；新建未落库不可点生成。  
- 标签与扫枪按 **EAN-13** 识别；枪设置需开启 EAN-13。

---

## 交互约定

### 独立维护入口（Admin）

- SPU 列表入口（类似「关键词」）：弹层维护条码。  
- 输入框 **打开即 focus**，支持 **扫码枪自动填入**（HID）。  
- 操作：手填 / 扫入 69 或其它合法码；无码时「生成」→ 29 规则；保存后同步 SKU。  
- 可选：保存前校验 EAN-13 校验位（非 13 位可警告，是否强制以后定）。

### 独立条码查询框（Admin）

需支持的页面（凡要查货处）：

- SPU 列表、SKU 列表  
- 库存列表、开单选 SKU（`skuPick`）  
- 售卖 / 回收 / 内部视图（`skuView`）

行为：

1. 独立「条形码」输入框；扫枪或手输 + 回车。  
2. 后端按 `barcode` **精确匹配**（查 SKU 或 SPU，按页语义）。  
3. **无论命中 1 条或多条，都先出二次确认列表**，用户点选后再进入/带入。  
4. 一 SPU 多半年 SKU 共用一码时，确认列表需能区分年/批次（或「统一价」）。

### 标签打印（Admin）

- 入口：维护条码页 / SPU 列表「打印标签」。  
- 内容建议：品名、规格、条码图（EAN-13）、条码数字；可选价格 / 「统一价」。  
- 实现：后端 ZXing 出图或前端画码 + `window.print()` + `@media print`；纸张对齐 DL-720W。

### H5 扫码

- 公共组件（如 `chai-scan.js`）：开摄像头 → ZXing 解码 → 回调码字符串。  
- 内部列表优先挂「扫码」；结果走同一套查询 + **二次确认列表**。  
- 手动输入框作无摄像头 / 权限失败时的兜底。

---

## 依赖

| 端 | 库 | 用途 |
|----|-----|------|
| server-admin | `com.google.zxing:core` + `javase` | 生成 EAN-13 图（标签） |
| H5 | `@zxing/library`（或等价 zxing-js） | 摄像头实时解 EAN-13 |
| 校验位 | 自写小工具即可 | 不强制绑库 |

---

## 公众号 JSSDK `scanQRCode` 配置说明（可选增强）

> **主路径仍是页面摄像头 + ZXing。** 仅当页面在微信内打开且希望用微信原生「扫一扫」时做本节。  
> 当前工程尚无现成 jsapi 签名接口（仅有菜单等），落地前需新增签名 API，并 **真机验证能否稳定识别 EAN-13 一维码**；若不能，继续用摄像头方案。

### 配置步骤（微信公众平台）

1. **登录** [微信公众平台](https://mp.weixin.qq.com/) → 使用承载 H5 菜单的公众号（订阅号/服务号以实际为准；**JS-SDK 能力以账号类型与开通情况为准**，服务号通常更完整）。  
2. **设置与开发 → 公众号设置 → 功能设置**  
   - **JS 接口安全域名**：填 H5 实际域名（如 `h5.example.com`），按提示放验文件或配 DNS；**不要带** `http(s)://`、路径、端口。  
   - 域名须已备案（国内）；与用户打开 H5 的 host **完全一致**。  
3. **开发 → 基本配置**  
   - 记录 **AppID**、**AppSecret**（Secret 仅存服务端，禁止下发前端）。  
4. （若尚未做）确保服务器能调通微信：  
   - `access_token`：`https://api.weixin.qq.com/cgi-bin/token`  
   - `jsapi_ticket`：`https://api.weixin.qq.com/cgi-bin/ticket/getticket?type=jsapi&access_token=...`  
   - token / ticket **缓存**（约 7200s），避免触发频控。  
5. **后端提供签名接口**（建议挂 server-api，仅给自家 H5 用）  
   - 入参：当前页完整 URL（`location.href.split('#')[0]`，前端传给后端）。  
   - 出参：`appId`、`timestamp`、`nonceStr`、`signature`。  
   - 签名算法：对 `jsapi_ticket`、`noncestr`、`timestamp`、`url` 按微信文档字典序拼接后 **SHA1**。  
6. **前端**  
   - 引入 `jweixin`；先请求签名接口，再 `wx.config({…, jsApiList:['scanQRCode']})`。  
   - `wx.ready` 后调用：  
     `wx.scanQRCode({ needResult: 1, scanType: ['barCode','qrCode'], success: … })`  
   - `needResult: 1` 才能在 H5 拿到码内容。  
7. **验收**  
   - 微信内打开内部 H5 → 点扫码 → 扫 EAN-13 标签 / 69 码 → 是否返回正确 13 位。  
   - 失败则隐藏 JSSDK 入口，仅保留摄像头 + 手输。

### 注意

- 签名用的 `url` 必须与微信打开页一致（含 `https`、路径、query；**不含 hash**）。  
- 安全域名变更后可能需等待生效；本地 hosts / 未配域名的 IP 页 **无法** 用 JSSDK。  
- iOS/安卓微信表现可能不一致，两台真机都测。

---

## DDL（约定）

### 全量

- 更新 `db/SQL_CHAI_DOMAIN.sql`：`chai_spu`、`chai_sku` 增加 `barcode`。

### 增量（临时，发版用）

- `db/tmp/YYYYMMDD_chai_barcode.sql`

本期 **不加** `UNIQUE(barcode)`。

---

## 分步实现（按序）

### B1 DDL + 实体 + 继承

- [ ] 全量 / 增量 DDL  
- [ ] admin：`ChaiSpu`、`ChaiSku` 增加 `barcode`  
- [ ] api：`ChaiSku`（及列表 VO 如需）增加 `barcode`  
- [ ] `applySharedFromSpu` / `saveBatchForSpu` / 同步已删：继承 `barcode`  
- [ ] SPU 改码后批量同步下属 SKU  

### B2 EAN-13 工具 + 独立维护入口

- [ ] `ChaiBarcodeUtil`：校验位、`29`+`spuId` 生成、可选校验合法性  
- [ ] Admin：`/business/chai/spu/barcode/{id}` 弹层（展示当前码、输入框 focus、扫枪填入、生成、保存）  
- [ ] SPU 列表入口链接  
- [ ] Maven：ZXing（本步若仅字符生成可后置到 B4；建议 B2/B4 一并加上）  

### B3 Admin 条码查询 + 二次确认

- [ ] 独立「条形码」查询框：SPU / SKU / 库存 / skuPick / skuView（售卖·回收·内部）  
- [ ] 精确匹配 API 或复用列表条件  
- [ ] **一律**二次确认列表（1 条也确认）；多 SKU 同码展示年批次 / 统一价  
- [ ] 输入框适合扫枪（focus、Enter）  

### B4 标签打印（DL-720W）

- [ ] 标签模板页（尺寸对齐选定标签纸）  
- [ ] ZXing 生成 EAN-13 图  
- [ ] 浏览器打印；得力驱动下真机出纸验收  

### B5 H5 摄像头扫码（主路径）

- [ ] 公共扫码组件（ZXing JS + `getUserMedia`）  
- [ ] 内部价目 / 相关查询页接入；结果 → 查询 → 二次确认列表  
- [ ] HTTPS 与权限失败时的手输兜底  

### B6 公众号 `scanQRCode`（可选）

- [ ] 公众平台：JS 接口安全域名、AppID/Secret  
- [ ] server-api：`jsapi_ticket` 缓存 + 签名接口  
- [ ] H5：`wx.config` + `scanQRCode`；与摄像头入口并存或按环境切换  
- [ ] 真机验证 EAN-13；不通过则保持 B5  

---

## 刻意不做（本期）

- DB 层 `barcode` 唯一约束  
- 自编码以外的码制为主识别（如仅 Code128）  
- 得力私有打印 SDK / 本地打印代理（浏览器打印不够用再开）  
- 对外 C 端价目强制扫码  
- 与统一价、搜索关键词需求耦合（可并行，字段独立）  
