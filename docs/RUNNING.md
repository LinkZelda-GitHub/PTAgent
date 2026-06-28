# 运行文档

## 环境要求

- JDK 17+
- PowerShell

当前版本不依赖 Maven、Gradle、Node/npm、MySQL、Redis，也不需要联网下载依赖。

版本号：`0.9.0-rc1`。

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

## 运行配置

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PTAGENT_ENV` | `local` | 运行环境标识 |
| `PTAGENT_DEMO_AUTH` | `true` | 是否启用本地微信/QQ/短信替代；生产必须关闭 |
| `PTAGENT_ALLOWED_ORIGIN` | 空 | 额外允许的跨域来源，必须精确匹配 |
| `PTAGENT_MAX_REQUEST_BYTES` | `1048576` | API 请求体上限，范围 16 KB 至 10 MB |
| `PTAGENT_DATA_DIR` | `data` | 本地账号数据库目录 |
| `PORT` | `8080` | 未通过命令参数指定时的监听端口 |

配置占位模板位于 `config/application.env.example`。该文件只用于列出变量，不会被程序自动加载；启动服务前应由操作系统、容器或部署平台注入环境变量。

启动保护：当 `PTAGENT_ENV=production` 时若 `PTAGENT_DEMO_AUTH` 仍为 `true`，或允许来源包含 `*`，程序会拒绝启动。

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

教师可在登录页切换到“教师注册”，选择微信、QQ或手机号作为登录方式，并填写联系方式、学历、科目、区域和教学资料。注册后账号默认处于待审核状态，需要最高管理员使用“教师简历”页面启用后才能登录。

如需修改数据库目录：

```powershell
$env:PTAGENT_DATA_DIR = "D:\ptagent-data"
.\scripts\run.ps1 -Port 8080
```

账号数据库使用 schema v2，只保存登录方式、登录标识、角色和资料，不保存密码或密码哈希。读取 schema v1 文件时会自动迁移旧账号。

当前微信/QQ使用本地授权适配，手机号验证码会直接回传给本地页面，便于离线演示。生产环境必须替换为微信开放平台、QQ互联和短信服务商接口，不能信任客户端提交的第三方身份标识或回传验证码。

## 审计日志

管理员登录后可以在页面侧边栏打开“审计日志”。也可以直接调用 API 查看本地内存审计记录：

```powershell
$login = Invoke-RestMethod "http://localhost:8080/api/auth/login" `
  -Method Post -ContentType "application/json" `
  -Body '{"loginMethod":"WECHAT","loginId":"ptagent_super"}'
$headers = @{ Authorization = "Bearer $($login.data.token)" }
Invoke-RestMethod "http://localhost:8080/api/audit-logs" -Headers $headers
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

构建脚本会先清理旧 `.class` 文件，避免已删除功能的字节码残留。

## 运行测试

当前测试同样保持零外部依赖，会先编译主代码，再编译并运行服务层测试：

```powershell
.\scripts\test.ps1
```

如果 PowerShell 提示脚本执行策略限制，可使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test.ps1
```

## 演示身份

| 角色 | 登录方式 | 登录标识 |
|---|---|---|
| 最高管理员 | 微信 | `ptagent_super` |
| 普通管理员 | QQ | `10001001` |
| 教师 | 手机号 | `13800000003` |

启动后系统会初始化示例教师、需求、申请、订单、授课记录和评价数据。需求、申请、订单等演示业务数据暂存于内存，服务重启后恢复为初始数据；注册账号、教师资料和审核启用状态保存在 `data/ptagent-accounts.json`，可跨重启恢复。

登录成功后浏览器会把会话令牌保存到 `localStorage`，刷新页面可恢复登录状态。令牌有效期为 12 小时；主动退出或服务重启后需要重新登录。

## 生成预集成交付包

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package.ps1
```

脚本会运行测试并生成 `dist/PTAgent-0.9.0-rc1-release.zip`。压缩包只包含运行字节码、静态页面、数据库迁移、文档、配置模板和发布启动脚本，不包含 `data`、测试字节码、Git 元数据或真实密钥。

解压后运行：

```powershell
.\scripts\run-release.ps1 -Port 8080 -DataDirectory "D:\ptagent-data"
```
