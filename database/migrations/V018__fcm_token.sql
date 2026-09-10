-- Migration: FCM device tokens for the native parent app (push notifications).
-- Run-order: V018

CREATE TABLE IF NOT EXISTS fcm_token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_type VARCHAR(20) NOT NULL DEFAULT 'padre',
    token VARCHAR(255) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'android',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_fcm_token (token),
    KEY idx_fcm_token_user (user_id),
    CONSTRAINT fk_fcm_token_user FOREIGN KEY (user_id) REFERENCES usuario(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- end migration
