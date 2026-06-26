# 架构文档

## 目录结构

```text
backend/src/main/java/com/ptagent
  App.java                 应用启动入口
  common/                  JSON、密码哈希等基础工具
  domain/                  领域模型和状态枚举
  repository/              Repository 接口、内存数据仓库与初始化数据
  service/                 业务服务层
  web/                     HTTP API 路由与静态资源托管
backend/src/test/java/      零外部依赖的服务层测试入口
public/                    Web GUI
scripts/                   构建与运行脚本
docs/                      项目文档
```

## 分层说明

| 层 | 职责 |
|---|---|
| `domain` | 用户、教师资料、简历、需求、申请、订单、授课记录、评价等核心对象 |
| `repository` | 通过 `Repository` 接口隔离数据访问；当前实现使用内存 Map 保存数据并初始化示例数据 |
| `service` | 登录、需求筛选排序、申请审核、课程记录、评价回访等业务规则 |
| `web` | REST API 分发、JSON 响应、静态文件访问 |
| `public` | 本地 Web GUI，面向教师、普通管理员、最高管理员 |

## 核心业务流

教师端：

```text
登录 -> 浏览需求广场 -> 筛选/排序 -> 申请接单 -> 查看匹配通知 -> 维护课程记录/简历
```

管理员端：

```text
登录 -> 发布需求 -> 查看简历 -> 标记简历状态 -> 录入回访评价
```

最高管理员端：

```text
登录 -> 审核教师启用 -> 审核接单申请 -> 通过后生成课程订单 -> 互推联系方式
```

## 匹配评分

`DemandService` 按 `Plan.md` 的权重思想实现：

- 科目匹配：40 分
- 年级适配：25 分
- 资质标签匹配：20 分
- 距离适配：15 分

筛选逻辑先按多条件 AND 过滤，再执行排序。

## 生产化方向

当前版本优先保证本地可运行和多文件架构完整。迁移到生产栈时，建议保持 `service` 层接口不变，将 `web` 替换为 Spring MVC Controller，将 `repository` 替换为 MyBatis-Plus Mapper，并引入 MySQL/Redis/OSS。
