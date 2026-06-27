# 依赖文档

## 当前本地版依赖

| 依赖 | 用途 | 版本 |
|---|---|---|
| JDK | 编译和运行 HTTP 服务 | 17+ |
| PowerShell | 执行构建/运行脚本 | Windows 自带 |
| 本地账号数据库 | JDK 文件与 JSON 能力，保存账号和教师资料 | 内置，无外部依赖 |

当前实现没有外部 jar、npm 包或 CDN 资源，适合在离线环境中运行和评审。运行时会自动创建 `data/ptagent-accounts.json`；该文件只保存密码哈希，不保存明文密码。

## Plan.md 中的生产技术栈

| 层级 | 推荐依赖 | 当前替代 |
|---|---|---|
| 后端框架 | Spring Boot | JDK `HttpServer` |
| ORM | MyBatis-Plus | Repository 接口 + 本地账号数据库 + 内存业务仓库 |
| 权限 | Spring Security | 内存 Bearer 会话 + `AccessGuard` 角色校验 |
| 数据库 | MySQL 8.0 | 账号/教师资料使用本地 JSON 数据库，其余业务数据使用内存 Map |
| 缓存 | Redis | 可由 `DemandService` 查询层迁移 |
| 前端 | Vue.js + Element UI | 原生 HTML/CSS/JS |
| 对象存储 | 阿里云 OSS | 简历文件 URL 字段占位 |

## 后续迁移建议

1. 用 Spring Boot Web 替换 `com.ptagent.web.ApiRouter`。
2. 保留 `domain` 与 `service` 包，给 `repository` 增加 MyBatis-Plus Mapper。
3. 将 `DemandService` 的筛选缓存接入 Redis，缓存键由筛选条件构成，TTL 30 秒。
4. 将 `public/` 前端迁移到 Vue 组件，继续复用当前 REST API 路径。

MySQL 8 客户端属于可选生产迁移依赖。安装后可运行 `scripts/init-database.ps1` 创建 `ptagent` 数据库及完整表结构。
