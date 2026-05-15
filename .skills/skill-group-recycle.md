# 分组与回收站管理技能

## 技能描述
处理短链接分组管理和回收站相关任务，包括分组的增删改查排序，以及回收站的保存、恢复、移除操作。

## 适用场景
- 短链接分组管理功能开发
- 分组排序逻辑调整
- 回收站功能开发
- 短链接软删除与恢复
- 短链接永久删除

## 项目上下文

### 分组管理核心文件
- **Controller**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/controller/GroupController.java`
- **Service 接口**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/service/GroupService.java`
- **Service 实现**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/service/impl/GroupServiceImpl.java`
- **DAO 实体**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/dao/entity/GroupDO.java`
- **唯一标识实体**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/dao/entity/GroupUniqueDO.java`
- **Mapper**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/dao/mapper/GroupMapper.java`, `GroupUniqueMapper.java`, `LinkGroupMapper.java`

### 回收站核心文件
- **Controller**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/controller/RecycleBinController.java`
- **Service 接口**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/service/RecycleBinService.java`
- **Service 实现**: `shortlink-main/main/src/main/java/com/nym/shortlink/core/service/impl/RecycleBinServiceImpl.java`

### 分组 API 端点
| 方法 | 路径 | 说明 | 限流 QPS |
|------|------|------|----------|
| POST | `/api/short-link/admin/v1/group` | 新增分组 | 5 |
| GET  | `/api/short-link/admin/v1/group` | 查询分组集合 | 20 |
| PUT  | `/api/short-link/admin/v1/group` | 修改分组名称 | 5 |
| DELETE | `/api/short-link/admin/v1/group` | 删除分组 | 5 |
| POST | `/api/short-link/admin/v1/group/sort` | 排序分组 | 10 |

### 回收站 API 端点
| 方法 | 路径 | 说明 | 限流 QPS |
|------|------|------|----------|
| POST | `/api/short-link/admin/v1/recycle-bin/save` | 保存到回收站 | 5 |
| GET  | `/api/short-link/admin/v1/recycle-bin/page` | 分页查询回收站 | 20 |
| POST | `/api/short-link/admin/v1/recycle-bin/recover` | 恢复短链接 | 5 |
| POST | `/api/short-link/admin/v1/recycle-bin/remove` | 移除短链接 | 5 |

### 关键 DTO
- **分组请求**: `ShortLinkGroupSaveReqDTO`, `ShortLinkGroupUpdateReqDTO`, `ShortLinkGroupSortReqDTO`
- **分组响应**: `ShortLinkGroupRespDTO`, `ShortLinkGroupCountQueryRespDTO`
- **回收站请求**: `RecycleBinSaveReqDTO`, `RecycleBinRecoverReqDTO`, `RecycleBinRemoveReqDTO`, `ShortLinkRecycleBinPageReqDTO`

### 分组设计要点
- 每个分组有唯一的 `gid`（分组标识），通过 `GroupUniqueDO` 生成
- 分组支持排序，排序字段存储在 `GroupDO` 的 `sortOrder` 中
- 删除分组时需要同时清理该分组下的所有短链接
- 分组与用户关联，通过 `UserContext` 获取当前用户的分组

### 回收站设计要点
- 短链接移入回收站是软删除操作，修改 `enable_flag` 标记
- 恢复操作将 `enable_flag` 改回可用状态
- 移除操作是永久删除，需同时清理数据库记录和 Redis 缓存
- 回收站中的短链接不参与跳转逻辑

## 代码规范
- 分组操作限流 QPS 较低（5-10），属于管理类操作
- 回收站操作限流 QPS 为 5，防止误操作
- 删除分组使用 `@DeleteMapping`，修改使用 `@PutMapping`
- 回收站保存和恢复使用 `@PostMapping`
- 分组排序通过传入排序列表批量更新

## 常见任务指南

### 新增分组相关功能
1. 在 `GroupService` 接口添加方法声明
2. 在 `GroupServiceImpl` 中实现方法
3. 在 `GroupController` 中添加 API 端点
4. 注意分组操作需要获取当前用户上下文

### 修改回收站逻辑
1. 关注 `enable_flag` 字段的状态变更
2. 恢复操作需同步恢复 Redis 缓存
3. 永久删除需清理所有关联数据（统计数据、跳转缓存等）
4. 考虑删除分组时如何处理分组下的短链接

### 分组与短链接联动
1. 创建短链接时必须指定 `gid`
2. 修改短链接分组时需获取分布式锁（`LOCK_GID_UPDATE_KEY`）
3. 查询分组短链接数量通过 `listGroupShortLinkCount` 方法
4. 分组删除前需检查是否存在有效短链接
