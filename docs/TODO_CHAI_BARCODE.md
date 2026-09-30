# SPU/SKU 条形码（barcode）

> 状态：B1～B5、B6（H5 三页）已完成；B3b 暂缓；B4 真机微调 / B6 真机验收 / B7 待做。  
> 目标：一 SPU 一码（SKU 继承）；69 码录入或自生成内部 EAN-13（22～29 开头）；维护与标签打印同一页；Admin 列表条码精确查 + 码类型筛选；运营工具箱批量打系统码；**H5 价目摄像头扫码**（inner-sale / sale / inner-recycle）；公众号扫一扫可选。  
> **本期不耦合「关键词」「统一价」**，保持现有机制。

## 背景与边界

| 点 | 结论 |
|----|------|
| 粒度 | **一 SPU 一码**；下属 SKU 继承 |
| 唯一性 | **本期不做** DB 唯一索引 |
| 国标 69 | 有则手填 / 扫枪填入；**产品自带国标码一般不贴标** |
| 系统码 | 无 69 时生成 **EAN-13 内部码**：前缀 **29**（属 **22～29** 码段）+ `spuId` 10 位 + **ZXing 校验位**；**需打印粘贴** |
| 是否系统生成 | **不加** `barcode_source` 等标识位；以 **`29` 前缀** 视为系统码，`69` 视为国标 |
| 编码库 | **ZXing**（校验位 + 出图 + H5 解码）。**不自写校验公式** |
| 录入 | 独立维护页（含单张标签预览/打印）；大表单不加条码字段 |
| 查询 | 列表工具栏「条形码搜索」→ 弹层大输入框扫码/回车；**仅 `barcode` 精确匹配，清空其它筛选**；不做「搜到后再确认选品」弹层 |
| 批量贴标 | **运营工具箱**（见下），与开发刷库工具分离 |

### 设备

| 设备 | 型号 | 对接 |
|------|------|------|
| 扫码枪 | 得力 **14952W** 无线 | HID 键盘楔入；Admin 不需 SDK |
| 标签机 | 得力 **DL-720W** | 第一期浏览器打印 + Windows 驱动 |
| 标签纸 | **横版 80mm × 60mm** | 单张维护页与批量打印 **同一套** `@page` / 标签尺寸 |

### H5

内部 H5：摄像头 + ZXing JS（HTTPS）。公众号 `scanQRCode` 可选，见文末。对外价目本期不强求扫码。

---

## 字段

| 字段 | 表 | 谁写 |
|------|-----|------|
| `barcode` | `chai_spu` | 独立维护页 |
| `barcode` | `chai_sku` | 继承 SPU |

- `varchar(32) NOT NULL DEFAULT ''`。  
- `applySharedFromSpu` / `saveBatchForSpu` / 同步已删 SKU 带上。  
- 改 SPU 码后同步全部 SKU。  
- 大表单保存 **保留** 已有 barcode。  
- 复制 SPU **不复制** 条码（新建后再生成/录入）。  
- **不增加** 来源标识字段；筛系统码用 `barcode LIKE '29%'`。

---

## 自编码（EAN-13 内部码）

GS1：**20～29** 为店内/内部码段。本系统：

- **生成固定前缀 `29`**（落在 22～29）。  
- 手填 / 扫入：允许 **69…** 国标，或 **22～29…** 内部码。  
- 结构：`29` + `sprintf("%010d", spuId)` + **ZXing 标准校验位**。  
- 用数字 `spuId`，不用业务串 `CHAI-00000001`。  
- 须已落库才可生成。

校验位与条码图一律走 ZXing（如 `EAN13Writer`、标准 EAN/UPC checksum API），**不手写奇偶加权**。

业务含义：

| 前缀 | 含义 | 贴标 |
|------|------|------|
| `69…` | 国标（产品自带） | 一般不贴 |
| `29…` | 系统码 | 需打印粘贴 |
| 空 | 尚未录入/生成 | 可先生成再打 |

---

## 交互

### 维护 + 单张标签（同一页）

SPU 列表「条码」入口 → `/business/chai/spu/barcode/{id}`。页内：

1. 输入框打开即 focus，扫枪填入；手填 69 / 22～29；「生成」出 29 内部码；保存同步 SKU。  
2. 标签预览 + 打印，纸张 **横版 80mm × 60mm**，内容：

```
{品牌} · {品名}
等级：{等级}
规格：{规格展示}
批次：{年}年{生产批次}
保质期：{保质期名}
官方价：{价}元
[EAN-13 条码图]
[13 位数字]
```

- 非卖品官方价显示「非卖品」。  
- 批次用现有年 + `CHAI_PROD_BATCH`，**不引入统一价展示**。  
- 单张与批量打印共用同一套标签尺寸 CSS。

### Admin 条码查询

需支持的页面：

- SPU / SKU 列表  
- 库存列表  
- 开单选品 `skuPick`  
- 售卖 / 回收 / 内部视图（`skuView`）

#### 已完成（B3）：筛选区条码框 + 码类型

- 筛选区独立「条形码」输入框；后端 `barcode` **精确匹配**。  
- SPU 列表「码类型」下拉：`ChaiBarcodeKind`（全部 / 无码 / 国标码 / 系统码）；有精确条码时忽略码类型。  
- 系统码严格 `29%`（与工具箱统计一致）。  
- 回车 / 点搜索刷新列表（可与其它条件组合）。  
- 实现时曾自动 focus 条码框，易与其它筛选抢焦点，扫枪场景一般。

#### 待做（B3b）：工具栏弹层扫码（运营主路径）— **暂缓**

> **先做 B6（H5）**，Admin 弹层扫码稍后。

解决「枪常扫错框 / 筛选区太乱」：

1. 各列表**工具栏**增加按钮「条形码搜索」。  
2. 点击 → `layer.open` **大输入框**，打开即 focus，供扫枪或手输。  
3. 扫入或回车 → 关闭弹层 → **刷新当前列表**：  
   - 仅带 `barcode=…`（**精确匹配**）；  
   - **清空其它筛选条件**（品牌、名称、状态等一律不要）。  
4. **不新增**后端接口；仍走现有列表 URL + `genCondition` 精确 equal。  
5. **不做**「搜到后再确认选哪条」的二次确认（含 skuPick）；一码多 SKU（多年份）时列表出多行，用户在表中点选。  
6. 筛选区原「条形码」框：实现 B3b 时可保留作手输备用，或一并去掉（实现时再定）。

### 运营工具箱 + 批量打印系统码

> 给**内部运营**使用，与开发刷库工具（`/business/tools/chai`、`ChaiToolsController`）**分开**。

| 项 | 约定 |
|----|------|
| Controller | `ChaiToolboxController`（包：`controller/chai`） |
| 路径前缀 | `/business/chai/toolbox` |
| 权限 | 新建如 `business:chai:toolbox:index`（菜单挂茶业下，给运营） |
| 模板 | `templates/business/chai/toolbox/…` |

工具首页展示（仅未删 SPU）：

- **系统码总数**（`barcode` 以 `29` 开头）  
- **按品牌的系统码数量**（列表/表格）  
- 可选展示：国标码数（`69…`）、无码数（便于后续批量生成）

批量打印：

1. **品牌**：多选；可不选。  
2. **范围**：所选品牌（或不限品牌）下、`barcode LIKE '29%'` 的未删 SPU。  
3. **排序**：按**品牌**排序后依次出标；未选或多选品牌时同样按品牌顺序排（再品名 / id），保证同品牌连在一起便于贴标。  
4. **打印页**：多张标签一页接一页（`page-break-after`），尺寸与单张相同（80×60 横版）；条码图复用现有 image 接口；`window.print()`。  
5. 建议单次上限（如 300 张），超出提示缩小品牌范围。  
6. 第一期可不做「已打印」标记；需要防漏打/重打时再加。

### H5 摄像头扫码（B6）

> 主路径：页面摄像头 + ZXing JS。微信原生 `scanQRCode` 见 B7（可选）。

#### 范围与优先级

1. **先做**内部在售价目 `inner-sale`，再扩 `sale` + `inner-recycle`。  
2. **B3b（Admin 弹层）暂缓**，不阻塞 B6。  
3. 对外 C 端价目已接扫码（`sale`）；其它 C 端页不强求。

#### 交互（与 Admin B3b 规则对齐）

```
搜索栏：[ 品牌/品名…… ] [搜索]  [内联 SVG 扫码按钮]
                              ↓
                    全屏扫码层（取景 + 提示「对准条码」）
                    · 识别成功 → 关层 → 刷新列表
                    · 无权限 / 非 HTTPS / 失败 → 展示「手动输入条码」
```

| 点 | 约定 |
|----|------|
| 识别成功 | 仅带 `barcode` **精确查**；**清空** keyword、品牌、茶类、同款、仓、新回收等其它筛选 |
| 一码多年份 | 列表出多行，点进详情/同款 |
| Icon | **内联 SVG**（条码/取景框），不单独找素材 |
| 环境 | 须 HTTPS（或 localhost）；权限失败用手输兜底 |
| 后端 | 售卖列表 API 增加 `barcode` 参数，SKU 精确 equal；有 barcode 时以条码为准 |

#### 实现要点

- 公共组件：`chai-barcode-scan.js` + CSS（ZXing + `getUserMedia`）  
- `inner-sale`：搜索栏旁扫码按钮 → `onResult` 写 `state.barcode` 并 reload  
- URL 可带 `?barcode=`，便于刷新保留  

---

## 依赖

| 端 | 库 | 用途 |
|----|-----|------|
| server-admin | `com.google.zxing:core` + `javase` | 校验位、EAN-13 图 |
| H5 | `@zxing/library` | 摄像头解码 |

---

## 公众号 JSSDK `scanQRCode`（可选，B7）

> 主路径仍是页面摄像头 + ZXing。仅微信内可选原生扫一扫。

### 配置步骤

1. 公众平台 → JS 接口安全域名（与 H5 host 一致，勿带协议/路径）。  
2. 记录 AppID / AppSecret（Secret 仅服务端）。  
3. 服务端缓存 `access_token`、`jsapi_ticket`，按当前页 URL（不含 hash）做 SHA1 签名。  
4. 前端 `wx.config` → `wx.scanQRCode({ needResult: 1, scanType: ['barCode','qrCode'] })`。  
5. 真机验收 EAN-13；不通过则只用摄像头方案。

---

## DDL

### 全量

- 更新 `db/SQL_CHAI_DOMAIN.sql`：`chai_spu`、`chai_sku` 增加 `barcode`。

### 增量

- `db/tmp/20260929_chai_barcode.sql`

本期 **不加** `UNIQUE(barcode)`，**不加** 条码来源字段。

---

## 分步实现（按序）

### B1 DDL + 实体 + 继承

- [x] 全量 / 增量 DDL  
- [x] admin：`ChaiSpu`、`ChaiSku` 增加 `barcode`  
- [x] api：`ChaiSku` 增加 `barcode`  
- [x] `applySharedFromSpu` / `saveBatchForSpu` / 同步已删：继承 `barcode`  
- [x] SPU 改码后批量同步下属 SKU（`saveBarcode` + `syncBarcodeFromSpu`）  
- [x] 主表单保存保留 barcode；复制不带 barcode  

### B2 维护页 + 标签打印（同一页）

- [x] Maven 引入 ZXing  
- [x] `ChaiBarcodeUtil`：拼码 + 调 ZXing 校验位/出图（不自写公式）  
- [x] `/business/chai/spu/barcode/{id}`：维护输入 + 标签预览/打印  
- [x] SPU 列表「条码」入口  

### B3 Admin 条码查询（列表直查）

- [x] 独立条码框：SPU / SKU / 库存 / skuPick / skuView  
- [x] 精确匹配条件；扫入/回车直接查列表  
- [x] 输入框适合扫枪（focus、Enter）  
- [x] SPU 列表「码类型」：`ChaiBarcodeKind`（无码 / 国标码 / 系统码 `29%`）  

### B3b 条形码搜索弹层（运营扫枪主路径）— **暂缓**

> 先做 B6；本项稍后。后端不改接口，仅前端交互。

- [ ] 列表工具栏按钮「条形码搜索」：SPU / SKU / 库存 / skuPick / skuView  
- [ ] `layer.open` 大输入框，打开即 focus  
- [ ] 扫入/回车 → 关弹层 → 仅 `barcode` 精确查，**清空其它筛选**  
- [ ] 不做搜到后的选品二次确认；一码多行由表内点选  
- [ ] 筛选区原条码框：保留或移除（实现时定）  

### B4 标签纸 80×60mm（横版）

- [x] 单张维护页：预览区与 `@page` 按 **80mm × 60mm 横版** 调整（打印机未到也可先做）  
- [ ] DL-720W + 真机纸张微调边距/字号（到货后）  
- [x] 批量打印页复用同一套尺寸（随 B5）  

### B5 运营工具箱 + 批量打印系统码

- [x] `ChaiToolboxController`：`/business/chai/toolbox`（运营用；**不**挂 `/business/tools/chai`）  
- [x] 权限 / 菜单：`business:chai:toolbox:index`（增量 SQL：`db/tmp/20260930_chai_toolbox_menu.sql`，需填父菜单 id 并赋权）  
- [x] 工具首页：系统码总数 + 各品牌系统码数量；品牌多选  
- [x] 打印页：按品牌排序输出 `29…` 标签；浏览器连续打印  
- [x] 单次数量上限与空结果提示  

### B6 H5 摄像头扫码（inner-sale / sale / inner-recycle）

- [x] 售卖列表 API：`barcode` 精确匹配  
- [x] 回收列表 API：`barcode` 精确匹配  
- [x] 公共组件 `chai-barcode-scan`（ZXing + getUserMedia + 内联 SVG；失败可手输）  
- [x] `inner-sale` 接入：扫码 → 仅 barcode 筛选并清空其它条件  
- [x] `sale` 接入（同交互）  
- [x] `inner-recycle` 接入（同交互）  
- [ ] HTTPS / 真机权限与扫码效果验收  

### B7 公众号 `scanQRCode`（可选）

- [ ] 安全域名 + 签名接口 + 前端接入  
- [ ] 真机测 EAN-13  

---

## 刻意不做（本期）

- DB 层 `barcode` 唯一约束  
- `barcode_source` / 「是否系统生成」标识位（用 `29` / `69` 前缀区分）  
- 自写 EAN-13 校验位算法（用 ZXing）  
- 关键词 / 统一价联动  
- 扫码后「再确认选哪条」弹层（含选品）；**允许** B3b 的输入用弹层  

- 得力私有打印 SDK / 本地打印代理  
- 对外其它 C 端页强制扫码（`sale` 已接）  
- 把运营批量贴标挂到开发工具箱 `/business/tools/chai`  
- 第一期「已打印」状态字段（有需要再加）  
