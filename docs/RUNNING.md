# 运行文档

## 环境要求

- JDK 17+
- PowerShell

当前版本不依赖 Maven、Gradle、Node/npm、MySQL、Redis，也不需要联网下载依赖。

## 启动

在项目根目录执行：

```powershell
.\scripts\run.ps1 -Port 8080
```

如果 PowerShell 提示脚本执行策略限制，可使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Port 8080
```

浏览器打开：

```text
http://localhost:8080
```

如 8080 被占用，可换端口：

```powershell
.\scripts\run.ps1 -Port 8090
```

## 健康检查

启动后可检查服务状态：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

API 响应头会包含 `X-Request-Id`。如果调用方传入同名请求头，服务端会沿用该值，便于和控制台访问日志关联。

## 账号数据库与注册

首次启动会自动创建：

```text
data/ptagent-accounts.json
```

教师可在登录页切换到“教师注册”，填写账号、联系方式、学历、科目、区域和教学资料。注册后账号默认处于待审核状态，需要最高管理员使用“教师简历”页面启用后才能登录。

如需修改数据库目录：

```powershell
$env:PTAGENT_DATA_DIR = "D:\ptagent-data"
.\scripts\run.ps1 -Port 8080
```

账号数据库只保存密码哈希。当前仍是本地 MVP，生产环境需要改用 BCrypt/Argon2 并迁移至 MySQL。

## 审计日志

管理员登录后可以在页面侧边栏打开“审计日志”。也可以直接调用 API 查看本地内存审计记录：

```powershell
$login = Invoke-RestMethod "http://localhost:8080/api/auth/login" `
  -Method Post -ContentType "application/json" `
  -Body '{"username":"super","password":"admin123"}'
$headers = @{ Authorization = "Bearer $($login.data.token)" }
Invoke-RestMethod "http://localhost:8080/api/audit-logs?actorId=100" -Headers $headers
```

当前 MVP 的审计日志暂存于内存，服务重启后会恢复为空；后续接入数据库后应持久化保存。

## 只编译

```powershell
.\scripts\build.ps1
```

编译产物位于：

```text
build\classes
```

## 运行测试

当前测试同样保持零外部依赖，会先编译主代码，再编译并运行服务层测试：

```powershell
.\scripts\test.ps1
```

如果 PowerShell 提示脚本执行策略限制，可使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test.ps1
```

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 最高管理员 | `super` | `admin123` |
| 普通管理员 | `admin` | `admin123` |
| 教师 | `teacher` | `teacher123` |

启动后系统会初始化示例教师、需求、申请、订单、授课记录和评价数据。需求、申请、订单等演示业务数据暂存于内存，服务重启后恢复为初始数据；注册账号、教师资料和审核启用状态保存在 `data/ptagent-accounts.json`，可跨重启恢复。

登录成功后浏览器会把会话令牌保存到 `localStorage`，刷新页面可恢复登录状态。令牌有效期为 12 小时；主动退出或服务重启后需要重新登录。
