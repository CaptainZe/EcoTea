# 微信订阅号 · 销售端功能规划

> 状态说明：本文记录已拍板规划；**ChaiSku 价目 H5 试跑通过后再继续**未勾选项。

## 已拍板约定

| 项 | 约定 |
|----|------|
| 数据源 | **`chai_sku`**（非 tea_sku）；品牌来自 **`chai_brand`** |
| 关键词匹配 | 先品牌名**完全匹配**（`chai_brand.name`），未命中再按 SKU **名称模糊** |
| 列表条件 | `status=上架(1)` 且 `deleted=0`；仅销售价，不露回收价 |
| 被动回复 | 最多 **10** 条；更多/过长 → 引导 H5 链接 |
| H5 访问 | **不登录**，公开只读 |
| 客服配置 | **admin 后台**可配置；api 只读 |
| H5 实现 | **静态 HTML + JS 调 JSON API** |

## 进度

### 已完成（试跑）

- [x] `GET /chai/sku/sale/list` 上架 SKU 分页（keyword 品牌精确 / 名称模糊）
- [x] 销售 VO（不含回收价）
- [x] H5 页：`/h5/chai/price.html`（查询 + 列表 + 分页）

本机验证：

```text
http://localhost:8081/h5/chai/price.html
http://localhost:8081/h5/chai/price.html?keyword=瑞泉
http://localhost:8081/chai/sku/sale/list?keyword=瑞泉&page=1&size=20
```

### 待验证通过后继续

- [ ] 微信文本关键词命中 → 被动回复最多 10 条摘要 + H5 链接  
      （链接规划：`https://api.ecotea.cn/h5/chai/price.html?keyword=xxx`）
- [ ] 自定义菜单：茶叶价目 → 上述 H5；关于我们 → 客服页
- [ ] 关于我们 H5：多客服微信号 / 二维码展示
- [ ] admin：客服联系人配置（表或现有能力，实现前再定）
- [ ] 备案 + HTTPS `api.ecotea.cn` 后：公众平台服务器 URL、菜单、真机验收

## 包与接口约定（api）

```text
controller/chai/ChaiSkuSaleController
service/chai/ChaiSkuSaleQueryService
domain/chai/ChaiSku
mapper/chai/ChaiSkuMapper
vo/chai/ChaiSkuSaleItemVO、ChaiSkuSalePageVO
static/h5/chai/price.html
static/h5/assets/css/chai-price.css
static/h5/assets/js/chai-price.js
handler/wx/…          # 关键词回复后续改 TextMsgHandler
```

## 明确不做（本期销售规划外）

- H5 登录 / 会员价
- 对外暴露回收价、成本字段
- 抽 admin/api 共用业务 jar
