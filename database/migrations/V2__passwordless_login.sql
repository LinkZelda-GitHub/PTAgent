DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_passwordless_login$$

CREATE PROCEDURE migrate_passwordless_login()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_account'
      AND column_name = 'username'
  ) THEN
    ALTER TABLE user_account
      ADD COLUMN display_name VARCHAR(50) NULL AFTER user_id,
      ADD COLUMN login_method VARCHAR(16) NULL AFTER display_name,
      ADD COLUMN login_id VARCHAR(100) NULL AFTER login_method;

    UPDATE user_account
    SET display_name = username,
        login_method = 'PHONE',
        login_id = phone_number;

    ALTER TABLE user_account
      MODIFY display_name VARCHAR(50) NOT NULL,
      MODIFY login_method VARCHAR(16) NOT NULL,
      MODIFY login_id VARCHAR(100) NOT NULL,
      ADD UNIQUE KEY uk_user_login_identity (login_method, login_id),
      DROP COLUMN username,
      DROP COLUMN password_hash;
  END IF;
END$$

CALL migrate_passwordless_login()$$
DROP PROCEDURE migrate_passwordless_login$$

DELIMITER ;
