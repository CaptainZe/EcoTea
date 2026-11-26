# EcoTea 文档

| 目录 | 职责 |
|------|------|
| [`prd/`](prd/) | 产品大方向（`PRD_*.md`） |
| [`prd/features/`](prd/features/) | **跨端**功能细文档（二次开发主入口；**须含相关测试用例**） |
| [`coding/`](coding/) | 工程规范（`CODING_API` / `CODING_ADMIN` / `CODING_MINI`、`CACHE` 等） |
| [`changelog/`](changelog/) | 已发布版本记录 |
| [`todo/`](todo/) | **仅未完成**施工清单；做完合并进 `prd/features` 或 `coding` 后删除 |
| [`db/`](db/) | **仅全量** SQL（按域建库基线）；**不**放版本增量 |
| [`deploy/`](deploy/) | 部署与运维 |
| [`ui/`](ui/) | UI 规范 |
| [`scripts/`](scripts/) | 辅助脚本 |

## 文档生命周期

```text
todo（施工） → 定稿并入 prd/features 或 coding → 删除 todo → changelog 记发版
```

- `PRD_*`：端/通道边界与大图，不堆小需求细节。  
- `prd/features/*`：天然跨 Admin / API / H5 / 小程序的功能说明。  
- 不要按端拆 features 子目录。

## features 约定

定稿进 `prd/features/*.md` 时**必须**含文末 **「测试用例」** 节（手工冒烟即可）。

| 项 | 约定 |
|----|------|
| 格式 | 场景 + 步骤要点 + 期望结果；表格或编号列表均可 |
| 覆盖 | 主路径 + 关键边界 / 跨端（Admin / API / H5 / 小程序按实际触及） |
| 不做 | 不写自动化脚本；不把用例拆到按端的子目录 |
| 生命周期 | todo 施工时可先列草稿；定稿并入 features 时带齐；CHANGELOG 冒烟可写「按 features 用例」 |

示例见 [`prd/features/BARCODE.md`](prd/features/BARCODE.md) §测试用例。

## SQL 约定

| 位置 | 内容 |
|------|------|
| `docs/db/SQL_*.sql` | 全量表结构 / 域基线；**改表时同步更新**，保证新环境按脚本建库即最新 |
| `docs/changelog/CHANGELOG_*.md` 对应版本节 | **本版增量**（`ALTER` / 数据脚本 / 菜单赋权等可执行 SQL）+ 发布 checklist |

- **不再使用** `docs/db/tmp/`。  
- 发版：目标库只执行 CHANGELOG 本版增量；新环境只跑全量脚本。  
- 增量很长时，CHANGELOG 可摘要并链到施工 TODO；短脚本直接贴在版本节。
