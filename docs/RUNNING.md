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

## 只编译

```powershell
.\scripts\build.ps1
```

编译产物位于：

```text
build\classes
```

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 最高管理员 | `super` | `admin123` |
| 普通管理员 | `admin` | `admin123` |
| 教师 | `teacher` | `teacher123` |

启动后系统会初始化示例教师、需求、申请、订单、授课记录和评价数据。数据暂存于内存，服务重启后恢复为初始演示数据。
