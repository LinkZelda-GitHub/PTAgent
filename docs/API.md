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
  "message": "错误信息"
}
```

## 基础

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api` | 服务状态 |
| GET | `/api/bootstrap` | 获取统计、科目、年级、区域、标签、演示账号 |

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
