# 依赖文档

## 当前本地版依赖

| 依赖 | 用途 | 版本 |
|---|---|---|
| JDK | 编译和运行 HTTP 服务 | 17+ |
| PowerShell | 执行构建/运行脚本 | Windows 自带 |
| 本地文件数据库 | JDK 文件、JSON 与 SHA-256 能力，保存账号、教师资料和链式审计日志 | 内置，无外部依赖 |

当前实现没有外部 jar、npm 包或 CDN 资源，适合在离线环境中运行和评审。运行时会自动创建 `data/ptagent-accounts.json` 和写操作触发的 `data/ptagent-audit.jsonl`；账号文件不保存密码或密码哈希。

`0.9.0-rc2` 可生成零依赖预集成交付 ZIP，但这不代表已经满足生产基础设施、外部认证、短信、数据库、对象存储和集中监控要求。

## Plan.md 中的生产技术栈

| 层级 | 推荐依赖 | 当前替代 |
|---|---|---|
| 后端框架 | Spring Boot | JDK `HttpServer` |
| ORM | MyBatis-Plus | Repository 接口 + 本地账号数据库 + 内存业务仓库 |
| 权限 | Spring Security | 内存 Bearer 会话 + `ApiRequest` 认证上下文 + `AccessGuard` |
| 第三方登录 | 微信开放平台、QQ互联 | 本地身份标识适配 |
| 手机验证码 | 短信服务商 SDK/API | 内存验证码与 `demoCode` |
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

外部服务接入状态、凭据要求和安全注意事项统一记录在 `docs/DEVELOPMENT_NOTES.md` 的“待接入外部服务”章节。
