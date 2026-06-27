# 数据库设计

## 当前本地数据库

当前版本会自动创建 `data/ptagent-accounts.json`，持久化以下数据：

- 用户账号、密码哈希、角色、启用状态、注册与登录时间
- 教师姓名、联系方式、学历、院校、科目、区域、资质和教学简介

写入采用临时文件替换，尽量避免进程中断造成半文件。可通过环境变量 `PTAGENT_DATA_DIR` 修改存储目录。需求、申请、订单等演示业务数据目前仍保存在内存中。

## MySQL 8 迁移

完整可执行表结构位于 `database/migrations/V1__init.sql`。已安装 MySQL 8 客户端时可执行：

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

`V1__init.sql` 是 MySQL 表结构的唯一事实来源，包含主键自增、唯一约束、外键和常用查询索引。后续结构变更应新增版本化迁移文件，不在本文复制一份可能失效的 SQL。

后续如接入空间索引，可将经纬度合并为 MySQL `POINT` 字段并建立 `SPATIAL INDEX`，用于距离查询和附近需求排序。
