# 架构文档

## 目录结构

```text
backend/src/main/java/com/ptagent
  App.java                 应用启动入口
  common/                  JSON、密码哈希、XLSX 读取等基础工具
  domain/                  领域模型和状态枚举
  exception/               API 错误码与业务异常
  repository/              Repository 接口、内存数据仓库与初始化数据
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
| `domain` | 用户、教师资料、简历、需求、申请、订单、授课记录、评价等核心对象 |
| `repository` | 通过 `Repository` 接口隔离数据访问；当前实现使用内存 Map 保存数据并初始化示例数据 |
| `service` | 登录、需求筛选排序、申请审核、课程记录、评价回访等业务规则 |
| `web` | 解析 HTTP 请求、统一错误响应、分发到轻量 controller、静态文件访问 |
| `public` | 本地 Web GUI，面向教师、普通管理员、最高管理员；`app.js` 作为入口，具体逻辑拆分到 `public/js` |

## API 分发

`ApiRouter` 只负责 CORS、请求解析、统一异常处理和 controller 调度。具体业务路径已拆分到：

- `AuthController`
- `DashboardController`
- `DemandController`
- `ApplicationController`
- `TeacherController`
- `CourseController`
- `FeedbackController`
- `NotificationController`
- `ImportController`

错误响应统一包含 `code`、`message` 与 `traceId`，其中 `code` 由 `ErrorCode` 维护，便于前端和未来 Spring MVC 全局异常处理复用。

## 运维基础

- `HealthHandler` 提供 `/actuator/health` 健康检查，返回内存仓储、用户、需求和订单的基础状态。
- `ApiRouter` 会为每个 API 请求生成或沿用 `X-Request-Id`，写入响应头。
- API 异常响应包含 `traceId`，控制台访问日志以 JSON 字符串输出 `traceId`、方法、路径、状态码和耗时。

## 前端模块

当前仍保持零 Node/npm 依赖，使用浏览器原生 ES Module：

- `app.js`：初始化入口。
- `js/api.js`：REST 请求与查询参数拼装。
- `js/state.js`：全局页面状态、角色菜单和筛选字段定义。
- `js/data.js`：登录、启动数据、需求列表和全量数据刷新。
- `js/render.js`：页面渲染、空状态和加载状态。
- `js/events.js`：导航、筛选、表单和按钮事件。
- `js/map.js`：本地坐标板和可选高德地图加载。
- `js/view.js`：DOM 工具、主题/侧栏偏好、Toast、提交中状态。

主题、侧栏折叠和需求广场筛选条件均使用 `localStorage` 本地持久化。

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
