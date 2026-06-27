# 开发备忘录

## 地图 Key 管理

当前需求广场已支持可选接入高德 JavaScript API。MVP 阶段为了保持本地可运行，页面允许在浏览器中填写 Web Key 和安全密钥，并保存到 `localStorage`。

后续生产化时需要调整：

- 高德 Web Key、`securityJsCode` 不得提交到仓库。
- dev/test/prod 环境分别配置独立 Key，避免演示环境影响正式配额。
- 推荐由后端配置或环境变量下发前端运行时配置，例如 `/api/config/map`。
- 生产环境应限制 Key 的来源域名，并定期轮换。
- README 中只保留配置方式和占位示例，不出现真实 Key。

## XLSX 导入

`example.xlsx` 已作为当前需求导入格式的样例文件。后续迁移 Spring Boot 后，建议把本地路径导入替换为 multipart 上传，并在服务端保存导入批次、失败行和原始文件归档记录。

## 本地账号数据库

- `data/ptagent-accounts.json` 用于零依赖阶段的账号与教师资料持久化，已加入 `.gitignore`，不得提交真实注册信息。
- 可用 `PTAGENT_DATA_DIR` 把数据文件迁移到仓库外目录。
- 当前文件数据库不是 MySQL 的替代品；它不提供多进程并发、复杂查询、备份恢复和字段级迁移。
- 生产迁移使用 `database/migrations/V1__init.sql`，密码哈希同步升级为 BCrypt/Argon2。

## 权限与审计

当前零依赖 MVP 已增加服务端内存会话：登录签发 12 小时 Bearer Token，支持 `/api/auth/me` 恢复和 `/api/auth/logout` 失效。业务写操作仍通过请求体或查询参数中的 `adminId`、`teacherId`、`actorId`、`submitAdminId` 表示领域操作者，并用 `AccessGuard` 校验账号启用状态和角色权限。

后续生产化时需要调整：

- 操作者身份必须来自服务端认证上下文，不能信任前端传入的用户 ID。
- 当前令牌随服务重启失效，也未实现多实例共享、续期、CSRF 与登录限流；生产环境应迁移到 Spring Security Session/JWT，并使用 Redis 或数据库维护可撤销状态。
- `AccessGuard` 可迁移为 Spring Security 权限注解、拦截器或领域服务。
- `AuditLog` 当前只保存在内存，接入 MySQL 后需要持久化，关键字段包括操作者、角色、动作、目标、详情、请求追踪 ID 和时间。
- 审计日志应和 `X-Request-Id`、登录用户、IP/User-Agent 关联，便于定位线上问题。
