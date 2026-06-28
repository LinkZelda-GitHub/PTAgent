# 架构文档

## 目录结构

```text
backend/src/main/java/com/ptagent
  App.java                 应用启动入口
  common/                  运行配置、版本、JSON、XLSX 读取等基础工具
  domain/                  领域模型和状态枚举
  exception/               API 错误码与业务异常
  repository/              Repository 接口、账号文件数据库与内存业务仓库
  service/                 业务服务层
  web/                     HTTP API 请求解析、controller 分发与静态资源托管
  web/controller/          按业务拆分的轻量 controller
backend/src/test/java/      零外部依赖的服务层测试入口
public/                    Web GUI
public/js/                 原生前端模块：API、状态、渲染、事件、数据加载
scripts/                   构建与运行脚本
docs/                      项目文档
```

## 分层说明

| 层 | 职责 |
|---|---|
| `domain` | 用户、教师资料、简历、需求、申请、订单、授课记录、评价、审计日志等核心对象 |
| `repository` | 通过 `Repository` 接口隔离数据访问；账号和教师资料使用本地文件数据库，其余演示数据使用内存 Map |
| `service` | 登录、需求筛选排序、申请审核、课程记录、评价回访等业务规则 |
| `web` | 解析 HTTP 请求、统一错误响应、分发到轻量 controller、静态文件访问 |
| `public` | 本地 Web GUI，面向教师、普通管理员、最高管理员；`app.js` 作为入口，具体逻辑拆分到 `public/js` |

## API 分发

`ApiRouter` 负责 CORS、请求解析、内存会话校验、统一异常处理和 controller 调度。具体业务路径已拆分到：

- `AuthController`
- `DashboardController`
- `DemandController`
- `ApplicationController`
- `TeacherController`
- `CourseController`
- `FeedbackController`
- `NotificationController`
- `ImportController`
- `AuditController`

错误响应统一包含 `code`、`message` 与 `traceId`，其中 `code` 由 `ErrorCode` 维护，便于前端和未来 Spring MVC 全局异常处理复用。

## 权限与审计

- `AuthService` 统一处理微信、QQ、手机号验证码登录，成功后签发 12 小时内存令牌；`ApiRouter` 对非公开 API 统一校验 Bearer Token。
- `ApiRouter` 校验令牌后把登录用户写入 `ApiRequest` 认证上下文；controller 会用该身份覆盖写请求中的操作者字段。
- `AccessGuard` 集中处理 MVP 阶段的角色校验，服务层只接收 Web 边界注入的可信操作者 ID。
- 关键写操作会写入内存 `AuditLog`：发布/关闭需求、XLSX 导入、申请接单、审核申请、启用/禁用教师、简历提交/标记、授课记录和评价。
- `AuditController` 提供 `/api/audit-logs`，仅管理员和最高管理员可查看；请求日志同步记录认证用户 ID。
- 当前会话只保存在单进程内存中，服务重启后失效；迁移 Spring Security/JWT 时可将轻量认证上下文替换为框架安全上下文。

## 预发布安全边界

- `AppConfig` 统一读取环境、演示认证、允许来源和请求大小配置；`AppVersion` 提供发布版本号。
- `SecurityHeaders` 为 API、健康检查和静态资源统一设置浏览器安全响应头，并只允许同主机或精确配置的 CORS 来源。
- `ApiRouter` 只接受 JSON 请求，采用有界读取避免大请求耗尽内存，错误统一返回稳定错误码。
- `AuthService` 在单进程内限制验证码发送频率、验证码错误次数和登录失败次数；这些状态接入 Redis 后才能支持多实例。
- `Demand` 按调用角色输出运营视图或教师脱敏视图，未匹配教师拿不到家长完整联系方式和精确地址。

## 账号持久化

- `AccountDatabase` 使用 JDK 文件 API 和项目内置 JSON 工具维护 schema v2 的 `data/ptagent-accounts.json`，旧密码账号读取后自动转换为第三方登录身份。
- `AppRepository` 启动时先载入示例数据，再用账号数据库恢复注册用户、教师资料和审核状态。
- 用户创建、登录时间更新、资料修改、评分变化和账号启用状态都会触发原子快照写入。
- 服务测试默认使用 `new AppRepository(false)` 隔离本地数据库，持久化测试使用临时目录。
- `database/migrations` 保存 MySQL Repository 的版本化结构基线和无密码账号迁移。

## 运维基础

- `HealthHandler` 提供 `/actuator/health` 健康检查；本地环境返回诊断详情，`production` 环境隐藏数据库路径和业务数量。
- `ApiRouter` 会为每个 API 请求生成或沿用 `X-Request-Id`，写入响应头。
- API 异常响应包含 `traceId`，控制台访问日志以 JSON 字符串输出 `traceId`、方法、路径、状态码和耗时。

## 前端模块

当前仍保持零 Node/npm 依赖，使用浏览器原生 ES Module：

- `app.js`：初始化入口。
- `js/api.js`：REST 请求、Bearer Token 注入与查询参数拼装。
- `js/state.js`：全局页面状态、角色菜单和筛选字段定义。
- `js/data.js`：登录、教师注册、会话恢复/退出、启动数据、需求列表和全量数据刷新。
- `js/render.js`：页面渲染、空状态和加载状态。
- `js/events.js`：导航、筛选、表单和按钮事件。
- `js/map.js`：本地坐标板和可选高德地图加载。
- `js/view.js`：DOM 工具、主题/侧栏偏好、Toast、提交中状态。

主题、侧栏折叠、登录令牌和需求广场筛选条件均使用 `localStorage` 本地持久化。登录成功后右侧登录栏会从布局中隐藏，左侧提供按角色展示的图标文字导航；折叠后可通过悬浮提示识别入口。

地图默认使用本地坐标板；填写高德 Web Key 后，前端通过高德 JavaScript API Loader 加载真实地图并绘制需求标记。地图 Key 与安全密钥仅保存于浏览器 `localStorage`，不进入仓库。

## XLSX 导入

`DemandImportService` 使用 JDK 自带 ZIP/XML 能力读取 `.xlsx`，不引入 Apache POI 等外部依赖。当前导入流程：

```text
ImportController -> DemandImportService -> XlsxReader -> Repository
```

支持 `example.xlsx` 的第一张工作表格式：第一行是区域列名，第二行开始每个非空单元格是一条多行订单。导入时会解析订单号、地址、年级性别、科目、成绩、时间、老师要求和报酬，并按订单号跳过重复导入。

## 核心业务流

教师端：

```text
注册 -> 等待最高管理员审核启用 -> 登录 -> 浏览需求广场 -> 筛选/排序 -> 申请接单 -> 查看匹配通知 -> 维护课程记录/简历
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

`scripts/package.ps1` 生成外部服务接入前的预集成包。它不是正式生产镜像；生产发布仍需完成 `docs/DEVELOPMENT_NOTES.md` 中的全部阻断项。
