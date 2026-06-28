# 数据库设计

## 当前本地数据库

当前版本会自动创建 `data/ptagent-accounts.json`，持久化以下数据：

- 微信/QQ/手机号登录身份、展示名称、角色、启用状态、注册与登录时间
- 教师姓名、联系方式、学历、院校、科目、区域、资质和教学简介

写入采用临时文件替换，尽量避免进程中断造成半文件。可通过环境变量 `PTAGENT_DATA_DIR` 修改存储目录。需求、申请、订单等演示业务数据目前仍保存在内存中。

本地 JSON 文件不具备并发事务、访问控制、加密、自动备份和灾难恢复能力，只用于预集成验收。正式部署前必须迁移全部业务数据到受控数据库，并完成备份恢复演练。

## MySQL 8 迁移

完整可执行表结构位于 `database/migrations`。初始化脚本会按文件名顺序执行全部迁移；已安装 MySQL 8 客户端时可执行：

```powershell
.\scripts\init-database.ps1 -Server 127.0.0.1 -Port 3306 -Username root -Password "你的密码"
```

迁移脚本包含以下 10 张表：

| 领域 | 表 |
|---|---|
| 账号与教师 | `user_account`、`teacher_profile`、`teacher_resume` |
| 需求与接单 | `demand`、`demand_application` |
| 履约与评价 | `course_order`、`teaching_record`、`feedback` |
| 消息与审计 | `notification`、`audit_log` |

`V1__init.sql` 提供新数据库基线，`V2__passwordless_login.sql` 将旧账号表迁移为无密码身份模型。`user_account` 通过 `(login_method, login_id)` 唯一约束避免重复绑定。后续结构变更继续新增版本化迁移文件。

后续如接入空间索引，可将经纬度合并为 MySQL `POINT` 字段并建立 `SPATIAL INDEX`，用于距离查询和附近需求排序。
