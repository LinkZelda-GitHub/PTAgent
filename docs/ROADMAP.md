# 未来改进规划

## 规划目标

当前系统是一个 JDK 17 零外部依赖的本地可运行 MVP，核心价值是验证家教资源撮合平台的业务闭环和页面交互。后续改进应围绕三个目标推进：

- 保留现有清晰分层：`domain -> repository -> service -> web/public`
- 平滑迁移到生产技术栈：Spring Boot、Vue、MySQL、Redis、对象存储
- 逐步补齐真实业务所需的安全性、可维护性、性能和运营能力

## 当前架构基线

| 模块 | 当前状态 | 后续方向 |
|---|---|---|
| `domain` | 已有核心实体和状态枚举 | 保留并增强校验、审计字段、领域行为 |
| `repository` | 内存 Map + 示例数据 | 替换为 MyBatis-Plus/JPA + MySQL |
| `service` | 已沉淀核心业务规则 | 保持稳定，继续作为业务中枢 |
| `web` | JDK `HttpServer` 手写路由 | 替换为 Spring MVC Controller |
| `public` | 原生 HTML/CSS/JS GUI | 演进为 Vue 组件化前端 |
| 文档 | 已有运行、依赖、API、数据库、架构文档 | 增加部署、测试、运维、接口契约文档 |

## 阶段一：夯实 MVP 可维护性

优先级：高  
目标周期：1-2 周

### 后端

- 为 `service` 层补充单元测试，覆盖登录、需求筛选、申请审核、订单生成、评价计算。
- 给核心请求增加参数校验，避免空值、非法枚举、重复申请等边界漏出到前端。
- 将 `ApiRouter` 中的路径分发拆成多个轻量 controller 类，降低单文件复杂度。
- 抽象 `Repository` 接口，让内存实现和未来数据库实现可以并存切换。
- 增加统一错误码，例如 `AUTH_INVALID_PASSWORD`、`DEMAND_NOT_FOUND`、`APPLICATION_DUPLICATED`。

### 前端

- 将 `app.js` 按业务拆分为 API、状态、渲染、事件四类模块。
- 增加加载态、空状态、错误提示和表单提交中状态。
- 为侧栏折叠、明暗模式、筛选条件增加本地持久化。
- 优化移动端导航，避免后台功能在窄屏下拥挤。

### 验收标准

- 核心服务方法有测试覆盖。
- 手动验证教师、普通管理员、最高管理员三类流程均可完成。
- 代码结构支持未来替换数据层，而不改业务服务层调用方式。

## 阶段二：迁移到 Spring Boot 后端

优先级：高  
目标周期：2-3 周

### 技术改造

- 引入 Maven/Gradle 项目结构。
- 使用 Spring Boot Web 替代 JDK `HttpServer`。
- 将 `ApiRouter` 迁移为 REST Controller：
  - `AuthController`
  - `DemandController`
  - `TeacherController`
  - `ApplicationController`
  - `CourseController`
  - `FeedbackController`
  - `DashboardController`
- 使用 Bean Validation 做请求 DTO 校验。
- 使用 Spring Security + JWT/Session 做真实认证与权限隔离。

### 建议包结构

```text
com.ptagent
  controller/
  service/
  repository/
  mapper/
  domain/
  dto/
  config/
  security/
  exception/
```

### 验收标准

- 现有 REST API 路径尽量保持兼容。
- 三角色权限由后端强制控制。
- 登录态不再依赖前端保存的演示用户对象。
- 应用可通过 `mvn spring-boot:run` 或 jar 包启动。

## 阶段三：落地 MySQL 数据持久化

优先级：高  
目标周期：2 周

### 数据层

- 使用 MySQL 8.0 保存用户、教师、简历、需求、申请、订单、授课记录、评价、通知。
- 使用 Flyway/Liquibase 管理数据库迁移脚本。
- 基于 `docs/DATABASE.md` 建表，并补齐以下字段：
  - `created_at`
  - `updated_at`
  - `created_by`
  - `updated_by`
  - `deleted`
- 需求经纬度后续可升级为 `POINT` 字段和空间索引。

### 查询优化

- 对需求广场建立联合索引：`status + subject + grade + region + create_time`。
- 对申请建立唯一索引：`demand_id + teacher_id`。
- 对课程订单建立索引：`teacher_id + order_status`。
- 对简历建立索引：`teacher_id + is_active + submit_time`。

### 验收标准

- 服务重启后数据不丢失。
- 示例数据通过 seed 脚本导入，而不是写死在 repository。
- 需求列表在 1,000 条数据内查询响应小于 1 秒。

## 阶段四：前端组件化与体验升级

优先级：中  
目标周期：3-4 周

### 技术选型

- Vue 3 + Vite
- Pinia 管理前端状态
- Vue Router 管理角色工作台页面
- Element Plus 或 Naive UI 作为基础组件库

### 页面拆分

```text
views/
  LoginView.vue
  TeacherPlazaView.vue
  TeacherProfileView.vue
  AdminDemandView.vue
  ApplicationReviewView.vue
  CourseOrderView.vue
  FeedbackView.vue
components/
  AppSidebar.vue
  ThemeToggle.vue
  DemandCard.vue
  TeacherCard.vue
  MetricCard.vue
  DataTable.vue
```

### 体验改进

- 需求广场增加保存筛选方案。
- 需求详情改为抽屉式面板，减少页面跳转。
- 后台列表支持搜索、分页、批量操作。
- 表单拆成分步填写：家长信息、课程需求、定位与标签、确认发布。
- 增加操作审计提示，关键操作二次确认。

### 验收标准

- 前端可独立构建并由后端托管静态产物。
- 同一账号刷新页面后可恢复登录态和主题设置。
- 各角色只能看到自己有权限的菜单。

## 阶段五：缓存、消息与文件服务

优先级：中  
目标周期：2-3 周

### Redis 缓存

- 缓存需求广场筛选结果，TTL 30 秒。
- 缓存基础字典：科目、年级、区域、标签。
- 对热门需求详情做短缓存。

### 消息通知

- 当前内存通知升级为数据库通知表。
- 增加站内未读数、已读状态。
- 后续可接入短信、企业微信或公众号模板消息。

### 简历文件

- 本地路径字段升级为对象存储 URL。
- 接入阿里云 OSS 或兼容 S3 的对象存储。
- 简历上传增加文件类型、大小、病毒扫描和访问权限控制。

### 验收标准

- 需求广场重复筛选请求命中缓存。
- 简历上传后管理员可下载，非授权用户无法访问。
- 通知可持久化，并支持按用户查询未读消息。

## 阶段六：匹配算法与运营能力

优先级：中  
目标周期：持续迭代

### 匹配算法

当前匹配分由科目、年级、标签、距离组成。后续可以逐步引入：

- 教师历史接单成功率
- 教师评分与回访结果
- 可授课时间匹配
- 薪酬期望匹配
- 区域通勤偏好
- 最近活跃时间

建议保留可解释评分：

```text
总分 = 科目分 + 年级分 + 标签分 + 距离分 + 时间分 + 质量分
```

每个需求详情中展示“为什么推荐该教师”，方便最高管理员做最终判断。

### 运营后台

- 需求漏斗：发布、申请、审核、成单、授课、评价。
- 教师画像：科目分布、区域覆盖、评分、接单率。
- 家长来源统计：渠道、地区、年级、科目。
- 回访工单：待回访、已回访、投诉、仲裁记录。

### 验收标准

- 最高管理员能看到推荐教师列表和推荐理由。
- 能通过数据看出需求积压、教师供给不足和转化问题。

## 阶段七：安全、合规与运维

优先级：高  
目标周期：贯穿全程

### 安全

- 密码改为 BCrypt/Argon2，不再使用简单 SHA-256。
- 后端强制角色权限，前端只做展示控制。
- 所有敏感字段脱敏展示：手机号、微信号、家长姓名。
- 关键操作增加审计日志：审核教师、通过匹配、关闭需求、导出简历。
- 防重复提交、防 CSRF、防暴力登录。

### 合规

- 增加隐私政策和用户协议。
- 明确平台仅做信息撮合，不参与教学交付和费用结算。
- 教师资质证件上传、审核、到期提醒。
- 家长和教师联系方式仅在匹配成功后展示。

### 运维

- 增加健康检查：`/actuator/health`。
- 增加结构化日志和请求追踪 ID。
- 增加错误监控和慢查询告警。
- Docker 化部署，拆分 dev/test/prod 配置。

### 验收标准

- 安全扫描无高危问题。
- 生产配置不包含明文密码、密钥或演示账号。
- 线上异常可以通过日志定位到请求和用户操作。

## 推荐里程碑

| 里程碑 | 内容 | 结果 |
|---|---|---|
| M1 | 清理 MVP 架构、补测试、拆分前端 JS | 本地版可持续维护 |
| M2 | Spring Boot + MySQL | 后端生产化雏形 |
| M3 | Vue 组件化前端 | 前端可扩展 |
| M4 | Redis + OSS + 通知 | 接近真实业务运行 |
| M5 | 匹配算法 + 运营后台 | 支撑运营决策 |
| M6 | 安全合规 + Docker 部署 | 可进入试运行 |

## 优先级建议

近期最应该先做：

1. 抽象 repository 接口，避免业务服务绑定内存 Map。
2. 为 `DemandService` 和 `ApplicationService` 补测试。
3. 迁移 Spring Boot Controller，但保持现有 API 兼容。
4. 接入 MySQL，并用 seed 脚本替代硬编码演示数据。
5. 前端组件化，保留当前 GUI 的视觉方向和交互能力。

### 当前迭代记录

2026-06-26：

- 已抽象 `Repository` 接口，服务层依赖接口而不是内存实现。
- 已为 `DemandService` 和 `ApplicationService` 增加零外部依赖服务层测试。
- 已增加 `scripts/test.ps1`，用于本地编译并运行核心服务测试。
- 已将 `ApiRouter` 的业务路径拆分为轻量 controller，并增加统一错误码响应。
- 已将前端 `app.js` 拆分为 API、状态、数据、渲染、事件和视图工具模块，并增加筛选条件持久化与提交中状态。
- 已支持按照 `example.xlsx` 的分区订单格式导入需求数据，并在 README 中补充标准 xlsx 格式示例。
- 已为需求广场增加可用地图能力：默认本地坐标板，可选高德 JavaScript API 真实地图。
- 已新增开发备忘录，记录高德 Key 生产化处理；并补充 `/actuator/health`、`X-Request-Id` 与基础访问日志。

不建议过早投入：

- 复杂智能推荐模型
- 小程序端
- 在线支付
- 大规模消息推送

这些能力应在需求、教师、申请、订单和评价的主链路稳定后再推进。
