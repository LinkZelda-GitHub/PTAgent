# PTAgent 家教资源整合平台

这是根据 `Plan.md` 落地的本地可运行 Web 版 MVP。当前环境没有 Maven/Gradle/Node，因此项目采用 **JDK 17 零外部依赖** 实现：Java HTTP 服务提供 REST API 并托管原生 HTML/CSS/JS GUI，同时保持后端多层架构，方便后续迁移到 Spring Boot + Vue + Element UI。

## 快速启动

```powershell
.\scripts\run.ps1 -Port 8080
```

打开：

```text
http://localhost:8080
```

运行服务层测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test.ps1
```

演示账号：

| 角色 | 用户名 | 密码 |
|---|---|---|
| 最高管理员 | `super` | `admin123` |
| 普通管理员 | `admin` | `admin123` |
| 教师 | `teacher` | `teacher123` |

## 已实现范围

- 三角色登录、12 小时内存会话、刷新恢复与安全退出
- 教师资料、教师简历投递与管理员查看/标记
- 管理员发布需求、关闭需求
- 教师端需求广场，支持科目、年级、区域、资质标签筛选，以及最新、距离、薪酬、匹配度排序
- 教师申请接单，最高管理员审核，通过后生成课程订单与通知
- 课程订单、授课记录、家长回访评价
- `Repository` 接口抽象和核心服务层测试，便于后续替换 MySQL 数据层
- 按业务拆分的轻量 controller，以及包含 `code` 的统一错误响应
- 原生前端 ES Module 拆分、需求筛选本地持久化和表单提交中状态
- 需求广场地图支持本地坐标板，并可填写高德 Web Key 升级为真实地图
- 支持按 `example.xlsx` 的分区订单格式导入需求数据
- 健康检查 `/actuator/health`、API 请求追踪 ID 和基础访问日志
- 后端角色权限校验与关键操作审计日志，管理员可在“审计日志”页面查看
- 登录成功后自动隐藏右侧登录栏，左侧快捷入口使用图标按钮与悬浮说明
- 本地 GUI、REST API、运行/依赖/架构/API/开发备忘录文档

## 地图配置

需求广场默认使用本地坐标板，无需联网或 Key。若需要真实地图，可在页面的地图设置中填写高德 JavaScript API 的 Web Key 和安全密钥，点击“启用高德”后配置会保存到浏览器 `localStorage`。

注意：Key 和安全密钥不要提交到仓库。当前 MVP 仅做本地演示配置，后续生产版本应改为后端配置或环境变量下发。

## XLSX 需求导入格式

当前支持导入仓库根目录下的本地 `.xlsx` 文件，例如 `example.xlsx`。导入入口位于“需求发布与管理”页面，也可调用 API：

```json
{
  "adminId": 101,
  "filePath": "example.xlsx"
}
```

标准 xlsx 格式要求：

- 使用第一个工作表。
- 第一行为区域列名，例如 `南山区小初高`、`宝安区小初高`。
- 从第二行开始，每个非空单元格表示一条需求订单。
- 单元格内使用多行文本，字段标签保持一致。
- 系统会按订单号去重；重复导入同一个订单号会跳过。

标准表格示例：

| 南山区小初高 | 宝安区小初高 |
|---|---|
| `🎀🍭🍭SZ3914`<br>`联系地址：深圳#蛇口龙瑞佳园山海居`<br>`年级性别：五年级`<br>`辅导科目：语数英`<br>`学员成绩：无`<br>`时间安排：周二或周三晚上，周六上午`<br>`老师要求: 深大学在校优先；性格活泼开朗，有经验的女生`<br>`老师报酬：200/2小时` | `🌷🍭🍭SZ3882`<br>`联系地址：深圳#宝安沙井沙一村民幼儿园附近`<br>`年级性别：高一女孩`<br>`辅导科目：物化`<br>`学员成绩：一般`<br>`时间安排：暑假上课，每周2-3节课`<br>`老师要求: 女大优先，经验丰富`<br>`老师报酬：300-320/2小时` |

字段映射说明：

| XLSX 字段 | 导入到需求字段 |
|---|---|
| 第一行区域列名/联系地址 | `region`、`address` |
| 年级性别 | `grade`、`teacherGender` |
| 辅导科目 | `subject`，多科目时取第一个可识别科目，原文保留在备注 |
| 学员成绩 | `basicScore` |
| 时间安排、老师要求、原始内容 | `remark` |
| 老师报酬 | `salaryRange`，并尽量折算为每小时 `salaryMin/salaryMax` |
| 订单号，如 `SZ3914` | 写入备注，用于重复导入去重 |

## 文档

- [运行文档](docs/RUNNING.md)
- [依赖文档](docs/DEPENDENCIES.md)
- [架构文档](docs/ARCHITECTURE.md)
- [未来改进规划](docs/ROADMAP.md)
- [API 文档](docs/API.md)
- [数据库设计](docs/DATABASE.md)
- [开发备忘录](docs/DEVELOPMENT_NOTES.md)
