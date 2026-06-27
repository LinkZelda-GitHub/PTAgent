CREATE TABLE IF NOT EXISTS user_account (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role_type TINYINT NOT NULL,
  phone_number VARCHAR(20) NOT NULL UNIQUE,
  email VARCHAR(100) UNIQUE,
  is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teacher_profile (
  teacher_id BIGINT PRIMARY KEY,
  real_name VARCHAR(50) NOT NULL,
  gender TINYINT NOT NULL,
  education VARCHAR(50) NOT NULL,
  graduate_school VARCHAR(100),
  is_985 BOOLEAN NOT NULL DEFAULT FALSE,
  is_211 BOOLEAN NOT NULL DEFAULT FALSE,
  is_key_university BOOLEAN NOT NULL DEFAULT FALSE,
  subjects JSON NOT NULL,
  teaching_experience TEXT,
  expected_rate VARCHAR(50),
  available_time JSON,
  service_area JSON,
  personal_intro TEXT,
  avg_rating DECIMAL(3,2) NOT NULL DEFAULT 5.00,
  contact_phone VARCHAR(20),
  contact_wechat VARCHAR(50),
  has_teacher_cert BOOLEAN NOT NULL DEFAULT FALSE,
  normal_university BOOLEAN NOT NULL DEFAULT FALSE,
  competition_experience BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_teacher_user FOREIGN KEY (teacher_id) REFERENCES user_account(user_id)
);

CREATE TABLE IF NOT EXISTS teacher_resume (
  resume_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  resume_file_url VARCHAR(500),
  resume_json JSON,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_resume_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_profile(teacher_id),
  INDEX idx_resume_teacher_active (teacher_id, is_active)
);

CREATE TABLE IF NOT EXISTS demand (
  demand_id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
  teacher_gender TINYINT NOT NULL DEFAULT 3,
  basic_score VARCHAR(50),
  salary_range VARCHAR(50),
  salary_min INT,
  salary_max INT,
  remark TEXT,
  qualification_tags JSON,
  is_985_required BOOLEAN NOT NULL DEFAULT FALSE,
  is_211_required BOOLEAN NOT NULL DEFAULT FALSE,
  is_key_university_required BOOLEAN NOT NULL DEFAULT FALSE,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  close_time DATETIME NULL,
  INDEX idx_demand_query (status, subject, grade, region, create_time),
  CONSTRAINT fk_demand_admin FOREIGN KEY (admin_id) REFERENCES user_account(user_id)
);

CREATE TABLE IF NOT EXISTS demand_application (
  application_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  self_intro TEXT NOT NULL,
  apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0,
  confirm_time DATETIME NULL,
  confirm_admin_id BIGINT NULL,
  CONSTRAINT fk_application_demand FOREIGN KEY (demand_id) REFERENCES demand(demand_id),
  CONSTRAINT fk_application_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_profile(teacher_id),
  UNIQUE KEY uk_demand_teacher (demand_id, teacher_id)
);

CREATE TABLE IF NOT EXISTS course_order (
  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  demand_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  admin_id BIGINT NOT NULL,
  order_status TINYINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time DATETIME NULL,
  contact_shared BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_order_demand FOREIGN KEY (demand_id) REFERENCES demand(demand_id),
  CONSTRAINT fk_order_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_profile(teacher_id),
  INDEX idx_order_teacher_status (teacher_id, order_status)
);

CREATE TABLE IF NOT EXISTS teaching_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  lesson_date DATE NOT NULL,
  lesson_duration DECIMAL(4,2),
  content TEXT,
  student_performance TEXT,
  teacher_notes TEXT,
  CONSTRAINT fk_record_order FOREIGN KEY (order_id) REFERENCES course_order(order_id)
);

CREATE TABLE IF NOT EXISTS feedback (
  feedback_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  rating_score TINYINT NOT NULL,
  comment_text TEXT,
  feedback_source TINYINT NOT NULL,
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submit_admin_id BIGINT NOT NULL,
  CONSTRAINT fk_feedback_order FOREIGN KEY (order_id) REFERENCES course_order(order_id),
  CONSTRAINT chk_feedback_score CHECK (rating_score BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS notification (
  notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  message TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user_account(user_id),
  INDEX idx_notification_user_read (user_id, is_read, create_time)
);

CREATE TABLE IF NOT EXISTS audit_log (
  audit_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_id BIGINT NOT NULL,
  actor_role VARCHAR(30) NOT NULL,
  action VARCHAR(80) NOT NULL,
  target_type VARCHAR(50) NOT NULL,
  target_id BIGINT NOT NULL,
  detail TEXT,
  request_id VARCHAR(80),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_actor_time (actor_id, create_time),
  INDEX idx_audit_target (target_type, target_id)
);
