# 数据库设计

当前版本使用内存仓库存储数据。以下表结构用于后续迁移 MySQL 8.0。

```sql
CREATE TABLE user_account (
  user_id BIGINT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role_type TINYINT NOT NULL,
  phone_number VARCHAR(20) UNIQUE,
  email VARCHAR(50),
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  register_time DATETIME NOT NULL,
  last_login DATETIME
);

CREATE TABLE teacher_profile (
  teacher_id BIGINT PRIMARY KEY,
  real_name VARCHAR(50) NOT NULL,
  gender TINYINT NOT NULL,
  education VARCHAR(50),
  graduate_school VARCHAR(100),
  is_985 BOOLEAN DEFAULT FALSE,
  is_211 BOOLEAN DEFAULT FALSE,
  is_key_university BOOLEAN DEFAULT FALSE,
  subjects JSON,
  teaching_experience TEXT,
  expected_rate VARCHAR(50),
  available_time JSON,
  service_area JSON,
  personal_intro TEXT,
  avg_rating DECIMAL(3,2) DEFAULT 5.00,
  contact_phone VARCHAR(20),
  contact_wechat VARCHAR(50),
  has_teacher_cert BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (teacher_id) REFERENCES user_account(user_id)
);

CREATE TABLE teacher_resume (
  resume_id BIGINT PRIMARY KEY,
  teacher_id BIGINT NOT NULL,
  resume_file_url VARCHAR(500),
  resume_json JSON,
  is_active BOOLEAN DEFAULT TRUE,
  submit_time DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  FOREIGN KEY (teacher_id) REFERENCES teacher_profile(teacher_id),
  INDEX idx_resume_teacher_active (teacher_id, is_active)
);

CREATE TABLE demand (
  demand_id BIGINT PRIMARY KEY,
  admin_id BIGINT NOT NULL,
  parent_name VARCHAR(50) NOT NULL,
  parent_phone VARCHAR(20) NOT NULL,
  parent_wechat VARCHAR(50),
  address VARCHAR(200) NOT NULL,
  region VARCHAR(50) NOT NULL,
  longitude DECIMAL(10,7),
  latitude DECIMAL(10,7),
  subject VARCHAR(20) NOT NULL,
  grade VARCHAR(20) NOT NULL,
  teacher_gender TINYINT DEFAULT 3,
  basic_score VARCHAR(50),
  salary_range VARCHAR(50),
  salary_min INT,
  salary_max INT,
  remark TEXT,
  qualification_tags JSON,
  is_985_required BOOLEAN DEFAULT FALSE,
  is_211_required BOOLEAN DEFAULT FALSE,
  is_key_university_required BOOLEAN DEFAULT FALSE,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  close_time DATETIME,
  INDEX idx_demand_query (subject, grade, status, create_time),
  INDEX idx_demand_region (region)
);

CREATE TABLE demand_application (
  application_id BIGINT PRIMARY KEY,
  demand_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  self_intro TEXT NOT NULL,
  apply_time DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  confirm_time DATETIME,
  confirm_admin_id BIGINT,
  UNIQUE KEY uk_demand_teacher (demand_id, teacher_id)
);

CREATE TABLE course_order (
  order_id BIGINT PRIMARY KEY,
  demand_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  admin_id BIGINT NOT NULL,
  order_status TINYINT NOT NULL,
  create_time DATETIME NOT NULL,
  finish_time DATETIME,
  contact_shared BOOLEAN DEFAULT TRUE
);

CREATE TABLE teaching_record (
  record_id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  lesson_date DATE NOT NULL,
  lesson_duration DECIMAL(4,2),
  content TEXT,
  student_performance TEXT,
  teacher_notes TEXT
);

CREATE TABLE feedback (
  feedback_id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  rating_score TINYINT NOT NULL,
  comment_text TEXT,
  feedback_source TINYINT NOT NULL,
  submit_time DATETIME NOT NULL,
  submit_admin_id BIGINT
);
```

后续如接入空间索引，可将经纬度合并为 MySQL `POINT` 字段并建立 `SPATIAL INDEX`，用于距离查询和附近需求排序。
