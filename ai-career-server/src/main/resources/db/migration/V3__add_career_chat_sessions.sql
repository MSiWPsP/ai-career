CREATE TABLE IF NOT EXISTS `career_chat_session` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `conversation_id` VARCHAR(100) NOT NULL,
    `title` VARCHAR(100) NOT NULL DEFAULT '新对话',
    `summary` TEXT DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `message_count` INT NOT NULL DEFAULT 0,
    `last_message` VARCHAR(255) DEFAULT NULL,
    `last_message_at` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_career_chat_session_conversation_id` (`conversation_id`),
    KEY `idx_career_chat_session_user_status` (`user_id`, `status`, `is_deleted`),
    KEY `idx_career_chat_session_user_updated` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `career_chat_message` (
    `id` BIGINT NOT NULL,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `client_message_id` VARCHAR(64) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `message_order` INT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_career_chat_message_request_role` (`session_id`, `client_message_id`, `role`),
    KEY `idx_career_chat_message_session_order` (`session_id`, `message_order`),
    KEY `idx_career_chat_message_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
