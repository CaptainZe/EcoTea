# 管理后台（Admin）产品总览

> 工程：`server-admin`。编码边界见 [`docs/coding/CODING_ADMIN.md`](../coding/CODING_ADMIN.md)。  
> 多租户 / 三端关系见 [`PRD_TENANT.md`](PRD_TENANT.md)。  
> **跨端功能细则**见 [`features/`](features/)，不在本文展开。

## 1. 定位

内部运营与店务后台：商品主数据、库存与单据、条码与标签、微信/文案配置、组织与员工（平台能力）等。  
C 端获客走订阅号 + H5；店内作业主入口为内部小程序（见 `PRD_WX_MINI.md`）。

## 2. 模块地图（大方向）

| 域 | 能力概要 | 细文档 |
|----|----------|--------|
| 茶叶主数据 | 品牌、保质期、SPU/SKU、关键词/统一价（若启用） | 按需开 `features/*` |
| SPU 标记 | 对外隐藏 / 核心产品 / 品鉴款（可挂整装）；同步 SKU；列表批量 | [`features/SPU_MARK.md`](features/SPU_MARK.md) |
| 条码与标签 | SPU 条码维护、打印、工具箱批量打系统码；扫码查品 | [`features/BARCODE.md`](features/BARCODE.md) |
| 库存 | 分仓结存、出入库/调拨单据、选品（列表/扫码） | 待开 |
| 价目视图 | 售卖/回收/内部视图（后台查询） | 待开 |
| 微信配置 | 欢迎语、客服、H5 文案、菜单发布等 | 见 `PRD_WX_MP.md` |
| 组织 / 平台 | 店铺、Admin 数据范围、员工挂店与权限 | `PRD_TENANT.md` |
| 系统 | 用户角色菜单、字典（TIMO） | 编码规范 |

## 3. 与其它端边界

- **只共享** MySQL + Redis；不与 `server-api` 共 jar。  
- 主数据变更须按 [`docs/coding/CACHE.md`](../coding/CACHE.md) **写后 DEL**。  
- 商品展示与扫码查货：Admin 维护；H5 / 小程序走 API 只读查询。

## 4. 发版与 SQL

- 发版记录与**本版增量 SQL**：[`docs/changelog/CHANGELOG_ADMIN.md`](../changelog/CHANGELOG_ADMIN.md)  
- 全量建库脚本：[`docs/db/`](../db/)（不放版本增量；约定见 [`docs/README.md`](../README.md)）
