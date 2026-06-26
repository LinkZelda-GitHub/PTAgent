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

- 三角色登录与教师注册雏形
- 教师资料、教师简历投递与管理员查看/标记
- 管理员发布需求、关闭需求
- 教师端需求广场，支持科目、年级、区域、资质标签筛选，以及最新、距离、薪酬、匹配度排序
- 教师申请接单，最高管理员审核，通过后生成课程订单与通知
- 课程订单、授课记录、家长回访评价
- `Repository` 接口抽象和核心服务层测试，便于后续替换 MySQL 数据层
- 本地 GUI、REST API、运行/依赖/架构/API 文档

## 文档

- [运行文档](docs/RUNNING.md)
- [依赖文档](docs/DEPENDENCIES.md)
- [架构文档](docs/ARCHITECTURE.md)
- [未来改进规划](docs/ROADMAP.md)
- [API 文档](docs/API.md)
- [数据库设计](docs/DATABASE.md)
