USE ptagent;

ALTER TABLE audit_log
  ADD COLUMN client_ip VARCHAR(64) NULL AFTER request_id,
  ADD COLUMN user_agent VARCHAR(256) NULL AFTER client_ip,
  ADD COLUMN result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' AFTER user_agent,
  ADD COLUMN previous_hash CHAR(64) NULL AFTER result,
  ADD COLUMN content_hash CHAR(64) NULL AFTER previous_hash,
  ADD INDEX idx_audit_request_id (request_id),
  ADD INDEX idx_audit_result_time (result, create_time);
