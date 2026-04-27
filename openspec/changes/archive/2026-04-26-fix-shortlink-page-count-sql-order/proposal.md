## Why

访问短链接分页列表接口（`GET /api/short-link/admin/v1/page`）时，后端返回 `B000001 系统执行出错`。根本原因是 MyBatis-Plus 分页插件自动为 `pageLink` 查询生成 `COUNT(*) AS total` 的 count SQL 时，保留了原 SQL 中完整的 `ORDER BY` 子句。该子句引用了 `LEFT JOIN t_link_stats_today s` 所产生的别名 `s.today_pv` 等，而自动生成的 count SQL 中**不包含此 JOIN**，从而引发 `Unknown column 's.today_pv' in 'order clause'` 语法错误。

## What Changes

- 在 `LinkMapper.xml` 中为 `pageLink` 查询添加 `countId="pageLinkCount"` 属性，指向自定义的 count SQL。
- 新增 `<select id="pageLinkCount">` 语句，该语句仅对 `t_link` 表进行简单计数，不包含 `LEFT JOIN` 及 `ORDER BY`，从根本上消除别名引用问题。

## Capabilities

### New Capabilities

- 无。本次为纯技术性 Bug 修复。

### Modified Capabilities

- 无。不涉及产品功能规格变更。

## Impact

- **受影响的文件**：`main/src/main/resources/mapper/LinkMapper.xml`
- **受影响的功能**：管理后台短链接分页列表（`pageShortLink`）
- **Breaking Changes**：无。
