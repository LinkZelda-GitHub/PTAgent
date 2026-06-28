# API 文档

所有接口返回统一结构：

```json
{
  "ok": true,
  "data": {}
}
```

错误返回：

```json
{
  "ok": false,
  "code": "DEMAND_NOT_FOUND",
  "message": "错误信息",
  "traceId": "6f8f5e15-2b2c-438f-9f5a-4f7d6f457e4f"
}
```

`code` 为稳定错误码，便于前端按场景展示提示或做分支处理；`traceId` 可用于和服务端日志关联。所有 API 响应头都会包含 `X-Request-Id`。调用方传入的 Request ID 必须为 1 至 64 位且仅含字母数字及 `._:-`，否则服务端会重新生成。常见错误码包括：

| 错误码 | 说明 |
|---|---|
| `AUTH_LOGIN_METHOD_INVALID` | 登录方式不受支持 |
| `AUTH_IDENTITY_EXISTS` | 微信、QQ或手机号身份已绑定 |
| `AUTH_VERIFICATION_CODE_INVALID` | 手机验证码不存在或不正确 |
| `AUTH_VERIFICATION_CODE_EXPIRED` | 手机验证码已过期 |
| `AUTH_DISABLED` | 账号待审核或已被禁用 |
| `AUTH_PHONE_EXISTS` | 手机号已注册 |
| `AUTH_EMAIL_EXISTS` | 邮箱已注册 |
| `AUTH_REQUIRED` | 请求未携带登录令牌 |
| `AUTH_SESSION_INVALID` | 登录令牌不存在、已退出或已过期 |
| `ACCESS_DENIED` | 当前操作者没有权限执行该操作 |
| `DEMAND_NOT_FOUND` | 需求不存在 |
| `APPLICATION_DUPLICATED` | 重复申请同一需求 |
| `VALIDATION_ERROR` | 请求参数不符合要求 |
| `ROUTE_NOT_FOUND` | 接口不存在 |
| `IMPORT_FILE_NOT_FOUND` | 导入文件不存在 |
| `IMPORT_INVALID_FILE` | 导入文件格式或内容不符合要求 |
| `AUTH_RATE_LIMITED` | 登录或验证码操作过于频繁 |
| `AUTH_PROVIDER_UNAVAILABLE` | 演示认证已关闭且正式认证服务尚未接入 |
| `REQUEST_TOO_LARGE` | 请求体超过配置上限 |
| `UNSUPPORTED_MEDIA_TYPE` | 非 GET 请求未使用 `application/json` |
| `ORIGIN_NOT_ALLOWED` | 跨域来源不在允许列表 |

## HTTP 安全基线

- 非 GET API 必须使用 `Content-Type: application/json`。
- 请求体默认不超过 1 MB，可用 `PTAGENT_MAX_REQUEST_BYTES` 调整。
- CORS 默认只接受同主机来源，额外来源必须通过 `PTAGENT_ALLOWED_ORIGIN` 精确配置。
- API 和静态页面响应包含 CSP、`X-Content-Type-Options`、`X-Frame-Options`、Referrer Policy 和 Permissions Policy。
- 手机验证码有效期 5 分钟，60 秒内不可重复发送，每小时最多 5 次，单个验证码最多错误 5 次。
- 同一登录身份连续失败 5 次后临时限制 15 分钟。

## 基础

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api` | 服务状态 |
| GET | `/api/bootstrap` | 获取统计、科目、年级、区域、标签、演示账号 |
| GET | `/actuator/health` | 聚合健康状态 |
| GET | `/actuator/health/live` | 进程存活探针 |
| GET | `/actuator/health/ready` | 服务就绪探针 |
| GET | `/actuator/health/dependencies` | 外部依赖配置状态 |

健康检查响应：

```json
{
  "ok": true,
  "data": {
    "status": "UP",
    "version": "0.9.0-rc2",
    "environment": "local",
    "demoAuth": true,
    "time": "2026-06-26T15:45:00",
    "repository": "file+memory",
    "database": "E:\\PTAgent\\data\\ptagent-accounts.json",
    "users": 4,
    "demands": 5,
    "orders": 1
  }
}
```

上例为本地环境响应；`production` 环境会隐藏数据库路径和业务数量，避免公开内部部署细节。候选版尚未连接正式认证、MySQL 和 Redis，因此生产环境的就绪探针返回 HTTP 503；依赖探针中的 `CONFIGURED` 仅表示变量齐全，不表示连接成功。

## 权限与审计

当前 MVP 使用服务端内存会话。登录成功后返回 12 小时有效的令牌；除 `/api`、`/api/bootstrap`、登录、获取手机验证码和教师注册外，请求都需要携带：

```http
Authorization: Bearer <token>
```

所有写操作的操作者均来自 Bearer 登录会话。controller 会覆盖请求体中的 `adminId`、`teacherId`、`actorId` 或 `submitAdminId`，客户端不能通过伪造 ID 提升权限。教师数据列表也会按认证身份在服务端收敛范围。

教师调用 `/api/applications`、`/api/teachers`、`/api/resumes`、`/api/orders`、`/api/records` 和 `/api/notifications` 时只返回当前教师的数据；管理员按业务权限读取管理范围内的数据。

| 场景 | 权限要求 |
|---|---|
| 发布/关闭需求、导入 XLSX、录入评价 | `ADMIN` 或 `SUPER_ADMIN` |
| 审核接单申请、启用/禁用教师 | `SUPER_ADMIN` |
| 申请接单、提交授课记录 | `TEACHER`，授课记录必须由订单对应教师提交 |
| 更新教师资料、提交简历 | 本人或管理员 |
| 查看审计日志 | `ADMIN` 或 `SUPER_ADMIN` |

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/audit-logs` | 查看关键操作审计日志 |

审计日志响应示例：

```json
[
  {
    "id": 900,
    "actorId": 100,
    "actorRole": "SUPER_ADMIN",
    "action": "APPLICATION_REVIEW",
    "targetType": "APPLICATION",
    "targetId": 401,
    "detail": "APPROVED",
    "requestId": "match-review-401",
    "clientIp": "127.0.0.1",
    "userAgent": "Mozilla/5.0",
    "result": "SUCCESS",
    "createTime": "2026-06-26T16:20:00"
  }
]
```

持久化模式还会返回 `previousHash` 与 `hash`。失败的已认证写请求以 `HTTP_MUTATION` 和 `FAILURE` 记录，便于按 Request ID 关联错误响应、访问日志与审计日志。

## 账号

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 微信、QQ或手机号登录 |
| POST | `/api/auth/phone-code` | 获取手机验证码 |
| GET | `/api/auth/me` | 恢复当前登录会话 |
| POST | `/api/auth/logout` | 退出并作废当前令牌 |
| POST | `/api/auth/register-teacher` | 教师注册 |

微信登录请求：

```json
{
  "loginMethod": "WECHAT",
  "loginId": "ptagent_super"
}
```

QQ 登录只需将 `loginMethod` 改为 `QQ`。手机号登录应先调用 `/api/auth/phone-code`，再提交：

```json
{
  "loginMethod": "PHONE",
  "loginId": "13800000003",
  "verificationCode": "123456"
}
```

本地适配器会在验证码响应的 `demoCode` 字段中返回验证码，仅用于离线演示，生产环境必须移除该字段。

设置 `PTAGENT_DEMO_AUTH=false` 后，本地微信、QQ和验证码替代全部关闭，并返回 `AUTH_PROVIDER_UNAVAILABLE`；接入正式服务前不应把该状态作为可上线认证方案。

登录响应包含 `token`、`expiresAt`、`user`，教师账号还会包含 `profile`。令牌仅保存在服务端内存中，服务重启后需要重新登录。

教师注册请求示例：

```json
{
  "loginMethod": "PHONE",
  "loginId": "13912345678",
  "verificationCode": "123456",
  "realName": "李老师",
  "gender": 2,
  "phoneNumber": "13912345678",
  "email": "teacher@example.com",
  "education": "本科",
  "graduateSchool": "华南师范大学",
  "subjects": ["数学", "物理"],
  "serviceArea": ["天河区", "越秀区"],
  "hasTeacherCert": true,
  "personalIntro": "具备一对一教学经验"
}
```

注册成功返回 `PENDING_REVIEW`。账号与资料会保存到本地账号数据库，但必须由最高管理员启用后才能登录。

## 需求

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/demands` | 查询需求，支持筛选排序 |
| POST | `/api/demands` | 发布需求 |
| GET | `/api/demands/{id}` | 需求详情 |
| POST | `/api/demands/{id}/close` | 关闭需求 |
| POST | `/api/demands/{id}/applications` | 教师申请接单 |
| POST | `/api/import/demands/xlsx` | 按分区订单 xlsx 导入需求 |

查询参数：

| 参数 | 说明 |
|---|---|
| `status` | `OPEN` 或 `ALL` |
| `subject` | 科目 |
| `grade` | 年级 |
| `region` | 区域 |
| `tag` | 资质标签 |
| `sort` | `latest`、`distance`、`salaryHigh`、`salaryLow`、`match` |

教师登录时，匹配度使用当前会话中的教师身份自动计算，不接收客户端指定的 `teacherId`。

教师浏览未匹配需求时，家长姓名、电话、微信、详细地址和精确坐标会在服务端脱敏；匹配成功生成课程订单后才返回履约所需联系方式。

导入请求：

```json
{
  "filePath": "example.xlsx"
}
```

导入响应：

```json
{
  "ok": true,
  "data": {
    "sheetName": "最新可接分区细化看的订单",
    "importedCount": 13,
    "skippedCount": 0,
    "failedCount": 0,
    "items": []
  }
}
```

当前导入模式读取项目目录内的本地 `.xlsx` 文件，适配 `example.xlsx` 的第一行区域、单元格多行订单格式。后续迁移 Spring Boot 后可替换为 multipart 文件上传。

## 地图

地图能力在前端完成，不新增后端接口。需求数据复用 `/api/demands` 返回的 `longitude`、`latitude`、`address`、`region` 等字段。未配置高德 Key 时使用本地坐标板；配置高德 Web Key 后使用高德 JavaScript API 在浏览器端绘制真实地图。

## 申请与匹配

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/applications?status=ALL` | 查询申请 |
| POST | `/api/applications/{id}/review` | 审核申请 |

审核请求：

```json
{
  "status": "APPROVED"
}
```

`APPROVED` 会生成课程订单并通知教师；`REJECTED` 会发送未通过通知。

## 教师与简历

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/teachers` | 教师列表 |
| POST | `/api/teachers/{id}/enabled` | 启用/禁用教师 |
| POST | `/api/teachers/{id}/profile` | 更新教师资料 |
| GET | `/api/resumes` | 简历列表 |
| POST | `/api/resumes` | 投递简历 |
| POST | `/api/resumes/{id}/status` | 更新简历状态 |

## 课程与评价

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/orders` | 课程订单列表 |
| GET | `/api/records` | 授课记录列表 |
| POST | `/api/orders/{id}/records` | 新增授课记录 |
| GET | `/api/feedbacks` | 评价列表 |
| POST | `/api/feedbacks` | 录入评价 |

## 通知

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/notifications` | 当前登录用户的通知列表 |
