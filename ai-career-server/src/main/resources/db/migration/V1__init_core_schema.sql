CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'student',
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `education` VARCHAR(50) DEFAULT NULL,
    `major` VARCHAR(100) DEFAULT NULL,
    `grade` VARCHAR(50) DEFAULT NULL,
    `graduation_year` INT DEFAULT NULL,
    `career_stage` VARCHAR(50) DEFAULT NULL,
    `target_position` VARCHAR(100) DEFAULT NULL,
    `target_city` VARCHAR(100) DEFAULT NULL,
    `target_time` VARCHAR(100) DEFAULT NULL,
    `daily_study_hours` DECIMAL(4,1) DEFAULT NULL,
    `career_goal` VARCHAR(500) DEFAULT NULL,
    `interest_description` VARCHAR(1000) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_profile_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_skill` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `skill_name` VARCHAR(100) NOT NULL,
    `skill_category` VARCHAR(100) NOT NULL,
    `level` TINYINT NOT NULL,
    `score` INT NOT NULL,
    `source` VARCHAR(50) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_skill_user_name` (`user_id`, `skill_name`),
    KEY `idx_user_skill_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `career_plan` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `version` INT NOT NULL,
    `target_position` VARCHAR(100) NOT NULL,
    `match_score` INT DEFAULT NULL,
    `summary` TEXT,
    `advantages` JSON DEFAULT NULL,
    `weaknesses` JSON DEFAULT NULL,
    `roadmap` JSON DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `source_interview_id` BIGINT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_career_plan_user_id` (`user_id`),
    KEY `idx_career_plan_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `career_task` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `career_plan_id` BIGINT NOT NULL,
    `stage_name` VARCHAR(100) NOT NULL,
    `task_name` VARCHAR(200) NOT NULL,
    `task_description` VARCHAR(1000) DEFAULT NULL,
    `task_type` VARCHAR(50) NOT NULL,
    `priority` TINYINT NOT NULL DEFAULT 2,
    `status` TINYINT NOT NULL DEFAULT 0,
    `start_date` DATE DEFAULT NULL,
    `deadline` DATE DEFAULT NULL,
    `finish_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_career_task_user_id` (`user_id`),
    KEY `idx_career_task_plan_id` (`career_plan_id`),
    KEY `idx_career_task_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `interview` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `target_position` VARCHAR(100) NOT NULL,
    `interview_type` VARCHAR(50) NOT NULL,
    `difficulty` VARCHAR(50) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `conversation_id` VARCHAR(100) DEFAULT NULL,
    `question_count` INT NOT NULL DEFAULT 0,
    `start_time` DATETIME DEFAULT NULL,
    `end_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_interview_user_id` (`user_id`),
    KEY `idx_interview_user_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `interview_message` (
    `id` BIGINT NOT NULL,
    `interview_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `question_category` VARCHAR(100) DEFAULT NULL,
    `question_level` VARCHAR(50) DEFAULT NULL,
    `message_order` INT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_interview_message_interview_id` (`interview_id`),
    KEY `idx_interview_message_order` (`interview_id`, `message_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `interview_report` (
    `id` BIGINT NOT NULL,
    `interview_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `total_score` INT DEFAULT NULL,
    `scores` JSON DEFAULT NULL,
    `advantages` JSON DEFAULT NULL,
    `weaknesses` JSON DEFAULT NULL,
    `suggestions` JSON DEFAULT NULL,
    `summary` TEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_report_interview_id` (`interview_id`),
    KEY `idx_interview_report_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ability_score` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `ability_name` VARCHAR(100) NOT NULL,
    `score` INT NOT NULL,
    `source_type` VARCHAR(50) NOT NULL,
    `source_id` BIGINT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_ability_score_user_ability` (`user_id`, `ability_name`),
    KEY `idx_ability_score_user_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
