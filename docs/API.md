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

`code` 为稳定错误码，便于前端按场景展示提示或做分支处理；`traceId` 可用于和服务端日志关联。所有 API 响应头都会包含 `X-Request-Id`，调用方也可以主动传入该请求头。常见错误码包括：

| 错误码 | 说明 |
|---|---|
| `AUTH_INVALID_PASSWORD` | 密码不正确 |
| `DEMAND_NOT_FOUND` | 需求不存在 |
| `APPLICATION_DUPLICATED` | 重复申请同一需求 |
| `VALIDATION_ERROR` | 请求参数不符合要求 |
| `ROUTE_NOT_FOUND` | 接口不存在 |
| `IMPORT_FILE_NOT_FOUND` | 导入文件不存在 |
| `IMPORT_INVALID_FILE` | 导入文件格式或内容不符合要求 |

## 基础

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api` | 服务状态 |
| GET | `/api/bootstrap` | 获取统计、科目、年级、区域、标签、演示账号 |
| GET | `/actuator/health` | 健康检查 |

健康检查响应：

```json
{
  "ok": true,
  "data": {
    "status": "UP",
    "time": "2026-06-26T15:45:00",
    "repository": "memory",
    "users": 4,
    "demands": 5,
    "orders": 1
  }
}
```

## 账号

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/register-teacher` | 教师注册 |

登录请求：

```json
{
  "username": "teacher",
  "password": "teacher123"
}
```

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
| `teacherId` | 计算匹配度时使用 |

导入请求：

```json
{
  "adminId": 101,
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
  "adminId": 100,
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
| GET | `/api/notifications?userId={id}` | 用户通知列表 |
