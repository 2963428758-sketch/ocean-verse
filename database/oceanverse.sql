-- =====================================================
-- OceanVerse — 智慧海洋探索平台 完整数据库初始化脚本
-- 合并自: oceanverse.sql, new.sql, migration-permissions-v2.sql, species_distribution
-- 生成时间: 2026-06-24
-- =====================================================

-- =====================================================
-- OceanVerse — 智慧海洋探索平台 完整数据库初始化脚本
-- 合并自: oceanverse.sql, migration-observation-deleted.sql,
--         community-manager.sql, init-permission-tables.sql,
--         test-species-media.sql, test-species-extra.sql,
--         test-observation-data.sql, community_data.sql
-- 生成时间: 2026-06-17
-- =====================================================
DROP TABLE IF EXISTS `sys_operation_log`;

CREATE TABLE `sys_operation_log` (
                                     `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     `module` VARCHAR(50) DEFAULT NULL COMMENT '模块名',
                                     `operation_type` VARCHAR(30) DEFAULT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/QUERY/EXPORT',
                                     `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
                                     `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法: GET/POST/PUT/DELETE',
                                     `request_params` TEXT COMMENT '请求参数JSON',
                                     `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
                                     `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
                                     `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
                                     `execution_time` INT UNSIGNED DEFAULT NULL COMMENT '执行耗时(ms)',
                                     `operation_result` TINYINT DEFAULT 1 COMMENT '操作结果: 1-成功, 0-失败',
                                     `error_message` TEXT COMMENT '错误信息',
                                     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_operator` (`operator_id`),
                                     KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

ALTER TABLE observation MODIFY COLUMN location_id BIGINT UNSIGNED DEFAULT NULL;

CREATE DATABASE IF NOT EXISTS `oceanverse` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `oceanverse`;

SET FOREIGN_KEY_CHECKS = 0;

UPDATE observation SET deleted = UNIX_TIMESTAMP() WHERE deleted = 1;
UPDATE species SET deleted = UNIX_TIMESTAMP() WHERE deleted = 1;
UPDATE ecosystem SET deleted = UNIX_TIMESTAMP() WHERE deleted = 1;
UPDATE observation_location SET deleted = UNIX_TIMESTAMP() WHERE deleted = 1;


INSERT INTO sys_notification (user_id, title, content, type, is_read, create_time)
VALUES (1, 'AI 识别次数即将用尽', '今日剩余识别次数：2 次，请合理安排使用。', 'QUOTA_WARNING', 0, NOW());

ALTER TABLE sys_user DROP COLUMN email;
ALTER TABLE sys_user DROP COLUMN phone;

-- =====================================================
-- 一、建表语句
-- =====================================================

-- =====================================================
-- 1. 用户权限模块 (成员A)
-- =====================================================

CREATE TABLE `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `background_url` VARCHAR(500) DEFAULT NULL COMMENT '背景图URL',
    `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常, 2-锁定',
    `data_scope` INT DEFAULT 2 COMMENT '数据权限范围: 1-仅本人, 2-全部',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE `sys_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE `sys_user_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE `sys_permission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级',
    `name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `perm_code` VARCHAR(100) NOT NULL COMMENT '权限标识，如 user:list',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: MENU-菜单, BUTTON-按钮, API-接口',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

CREATE TABLE `sys_role_permission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `permission_id` BIGINT UNSIGNED NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE `sys_login_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID（登录失败时可能为空）',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '登录用户名',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
    `status` TINYINT NOT NULL COMMENT '登录状态: 1-成功, 0-失败',
    `message` VARCHAR(200) DEFAULT NULL COMMENT '结果说明',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- =====================================================
-- 2. 物种管理模块 (成员B)
-- =====================================================

CREATE TABLE `species` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物种ID',
    `species_code` VARCHAR(50) NOT NULL COMMENT '物种编码',
    `scientific_name` VARCHAR(200) NOT NULL COMMENT '学名',
    `common_name` VARCHAR(200) DEFAULT NULL COMMENT '通用名',
    `chinese_name` VARCHAR(200) DEFAULT NULL COMMENT '中文名',
    `protection_level` VARCHAR(20) DEFAULT NULL COMMENT '保护等级: 1-一级, 2-二级, 3-三级',
    `iucn_status` VARCHAR(20) DEFAULT NULL COMMENT 'IUCN状态',
    `kingdom` VARCHAR(50) DEFAULT NULL,
    `phylum` VARCHAR(50) DEFAULT NULL,
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '纲 (class是MySQL保留字)',
    `order_name` VARCHAR(50) DEFAULT NULL COMMENT '目',
    `family` VARCHAR(50) DEFAULT NULL COMMENT '科',
    `genus` VARCHAR(100) DEFAULT NULL COMMENT '属',
    `species` VARCHAR(100) DEFAULT NULL COMMENT '种',
    `description` TEXT COMMENT '描述',
    `morphology` TEXT COMMENT '形态特征',
    `ecology` TEXT COMMENT '生态习性',
    `video_url` VARCHAR(500) DEFAULT NULL,
    `conservation_status_id` BIGINT UNSIGNED DEFAULT NULL,
    `is_endemic` TINYINT NOT NULL DEFAULT 0 COMMENT '是否特有种',
    `is_invasive` TINYINT NOT NULL DEFAULT 0 COMMENT '是否入侵物种',
    `data_quality` VARCHAR(20) DEFAULT 'VERIFIED',
    `source` VARCHAR(100) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT UNSIGNED DEFAULT NULL,
    `update_by` BIGINT UNSIGNED DEFAULT NULL,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_species_code` (`species_code`, `deleted`),
    UNIQUE KEY `uk_scientific_name` (`scientific_name`, `deleted`),
    KEY `idx_family` (`family`),
    KEY `idx_chinese_name` (`chinese_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种信息表';

CREATE TABLE `species_distribution` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `species_id` BIGINT UNSIGNED NOT NULL,
    `distribution_type` VARCHAR(20) NOT NULL COMMENT 'NATIVE/INTRODUCED/MIGRATORY',
    `region_name` VARCHAR(100) DEFAULT NULL,
    `country` VARCHAR(100) DEFAULT NULL,
    `province` VARCHAR(100) DEFAULT NULL,
    `latitude` DECIMAL(10, 6) DEFAULT NULL,
    `longitude` DECIMAL(10, 6) DEFAULT NULL,
    `depth_min` DECIMAL(8, 2) DEFAULT NULL,
    `depth_max` DECIMAL(8, 2) DEFAULT NULL,
    `habitat_type` VARCHAR(100) DEFAULT NULL,
    `is_primary` TINYINT NOT NULL DEFAULT 0,
    `population_estimate` VARCHAR(100) DEFAULT NULL,
    `distribution_status` VARCHAR(20) DEFAULT 'COMMON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_species_id` (`species_id`),
    KEY `idx_lat_lng` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种分布表';

CREATE TABLE `species_media` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `species_id` BIGINT UNSIGNED NOT NULL,
    `media_type` VARCHAR(20) NOT NULL COMMENT 'IMAGE/VIDEO/AUDIO',
    `file_name` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(500) NOT NULL,
    `file_size` BIGINT UNSIGNED DEFAULT NULL,
    `thumbnail_url` VARCHAR(500) DEFAULT NULL,
    `media_title` VARCHAR(200) DEFAULT NULL,
    `media_description` TEXT,
    `is_primary` TINYINT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_species_id` (`species_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种媒体资源表';

-- =====================================================
-- 3. 生态系统与观测模块
--     (已合并 migration-observation-deleted.sql:
--      deleted 列统一为 BIGINT, 唯一索引含 deleted)
-- =====================================================

CREATE TABLE `ecosystem` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `ecosystem_code` VARCHAR(50) NOT NULL,
    `ecosystem_name` VARCHAR(200) NOT NULL,
    `ecosystem_type` VARCHAR(50) NOT NULL COMMENT 'MARINE/COASTAL/CORAL/DEEP_SEA',
    `description` TEXT,
    `area_estimate` DECIMAL(12, 2) DEFAULT NULL,
    `depth_min` DECIMAL(8, 2) DEFAULT NULL,
    `depth_max` DECIMAL(8, 2) DEFAULT NULL,
    `temperature_range` VARCHAR(50) DEFAULT NULL,
    `threat_factors` TEXT,
    `conservation_status` VARCHAR(50) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ecosystem_code` (`ecosystem_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生态系统表';

CREATE TABLE `observation_location` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `location_code` VARCHAR(50) NOT NULL,
    `location_name` VARCHAR(200) NOT NULL,
    `location_type` VARCHAR(30) NOT NULL,
    `latitude` DECIMAL(10, 6) NOT NULL,
    `longitude` DECIMAL(10, 6) NOT NULL,
    `country` VARCHAR(100) DEFAULT NULL,
    `province` VARCHAR(100) DEFAULT NULL,
    `city` VARCHAR(100) DEFAULT NULL,
    `ecosystem_id` BIGINT UNSIGNED DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_location_code` (`location_code`, `deleted`),
    KEY `idx_lat_lng` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观测地点表';

CREATE TABLE `observation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `observation_code` VARCHAR(50) NOT NULL,
    `observation_type` VARCHAR(30) NOT NULL COMMENT 'DIVE/SURVEY/SIGHTING/TRACKING',
    `observation_date` DATE NOT NULL,
    `observation_time` TIME DEFAULT NULL,
    `duration_minutes` INT UNSIGNED DEFAULT NULL,
    `location_id` BIGINT UNSIGNED DEFAULT NULL,
    `ecosystem_id` BIGINT UNSIGNED DEFAULT NULL,
    `latitude` DECIMAL(10, 6) DEFAULT NULL,
    `longitude` DECIMAL(10, 6) DEFAULT NULL,
    `depth` DECIMAL(8, 2) DEFAULT NULL,
    `water_temperature` DECIMAL(5, 2) DEFAULT NULL,
    `salinity` DECIMAL(5, 2) DEFAULT NULL,
    `ph` DECIMAL(4, 2) DEFAULT NULL,
    `weather_condition` VARCHAR(50) DEFAULT NULL,
    `sea_condition` VARCHAR(50) DEFAULT NULL,
    `observer_id` BIGINT UNSIGNED DEFAULT NULL,
    `observer_name` VARCHAR(100) DEFAULT NULL,
    `organization` VARCHAR(200) DEFAULT NULL,
    `equipment_used` VARCHAR(200) DEFAULT NULL,
    `notes` TEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_observation_code` (`observation_code`, `deleted`),
    KEY `idx_location_id` (`location_id`),
    KEY `idx_observer` (`observer_id`),
    KEY `idx_observation_date` (`observation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观测记录表';

-- =====================================================
-- 4. AI 智能服务模块 (成员B/E)
-- =====================================================

CREATE TABLE `image_recognition` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `recognition_code` VARCHAR(50) NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `thumbnail_url` VARCHAR(500) DEFAULT NULL,
    `file_name` VARCHAR(255) DEFAULT NULL,
    `file_size` BIGINT UNSIGNED DEFAULT NULL,
    `ai_model_version` VARCHAR(50) DEFAULT NULL,
    `recognition_result` TEXT COMMENT '识别结果JSON',
    `predicted_species_id` BIGINT UNSIGNED DEFAULT NULL,
    `predicted_species_name` VARCHAR(200) DEFAULT NULL,
    `confidence_score` DECIMAL(5, 4) DEFAULT NULL,
    `top_predictions` TEXT COMMENT 'Top预测结果JSON',
    `recognition_type` VARCHAR(30) DEFAULT 'AUTO',
    `verification_status` VARCHAR(20) DEFAULT 'PENDING',
    `verified_by` BIGINT UNSIGNED DEFAULT NULL,
    `verified_at` DATETIME DEFAULT NULL,
    `observation_id` BIGINT UNSIGNED DEFAULT NULL,
    `user_id` BIGINT UNSIGNED DEFAULT NULL,
    `processing_time_ms` INT UNSIGNED DEFAULT NULL,
    `error_message` TEXT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recognition_code` (`recognition_code`),
    KEY `idx_predicted_species` (`predicted_species_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图像识别记录表';

CREATE TABLE `qa_history` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `question_code` VARCHAR(50) NOT NULL,
    `question_type` VARCHAR(30) NOT NULL COMMENT 'SPECIES/ECOLOGY/CONSERVATION/GENERAL',
    `question_text` TEXT NOT NULL,
    `question_category` VARCHAR(50) DEFAULT NULL,
    `user_id` BIGINT UNSIGNED DEFAULT NULL,
    `session_id` VARCHAR(100) DEFAULT NULL,
    `ai_model_version` VARCHAR(50) DEFAULT NULL,
    `answer_text` TEXT,
    `source_references` TEXT,
    `confidence_score` DECIMAL(5, 4) DEFAULT NULL,
    `answer_tokens` INT UNSIGNED DEFAULT NULL,
    `processing_time_ms` INT UNSIGNED DEFAULT NULL,
    `feedback` TINYINT DEFAULT NULL COMMENT '1-满意, 0-不满意',
    `feedback_text` TEXT,
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_code` (`question_code`),
    KEY `idx_user` (`user_id`),
    KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能问答历史表';

-- =====================================================
-- 5. 社区互动模块 (成员D)
-- =====================================================

CREATE TABLE `community_post` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发布者ID',
    `content` TEXT NOT NULL COMMENT '动态内容',
    `post_type` VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL/OBSERVATION/RECOGNITION',
    `related_species_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联物种',
    `related_observation_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联观测',
    `image_urls` TEXT COMMENT '图片列表JSON',
    `like_count` INT UNSIGNED DEFAULT 0,
    `comment_count` INT UNSIGNED DEFAULT 0,
    `favorite_count` INT UNSIGNED DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '0-隐藏, 1-正常, 2-置顶',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_post_type` (`post_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区动态表';

CREATE TABLE `community_comment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
    `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID(回复)',
    `content` TEXT NOT NULL,
    `like_count` INT UNSIGNED DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

CREATE TABLE `community_like` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `target_type` VARCHAR(20) NOT NULL COMMENT 'POST/COMMENT/SPECIES',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
    KEY `idx_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

CREATE TABLE `community_favorite` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `target_id` BIGINT UNSIGNED NOT NULL,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'SPECIES/POST/OBSERVATION',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

CREATE TABLE `community_follow` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `follow_user_id` BIGINT UNSIGNED NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- =====================================================
-- 6. 系统支撑表
-- =====================================================

CREATE TABLE `user_points` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `points` INT NOT NULL DEFAULT 0 COMMENT '积分变动',
    `action_type` VARCHAR(30) NOT NULL COMMENT 'OBSERVATION/RECOGNITION/POST/DAILY_LOGIN',
    `description` VARCHAR(200) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

CREATE TABLE `sys_notification` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` VARCHAR(30) COMMENT 'SYSTEM/LIKE/COMMENT/FOLLOW',
    `is_read` TINYINT DEFAULT 0 COMMENT '0-未读, 1-已读',
    `related_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联ID(评论ID/通知ID等)',
    `target_post_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '目标帖子ID',
    `from_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

CREATE TABLE `sys_operation_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `module` VARCHAR(50) DEFAULT NULL COMMENT '模块名',
    `operation_type` VARCHAR(30) DEFAULT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/QUERY/EXPORT',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法: GET/POST/PUT/DELETE',
    `request_params` TEXT COMMENT '请求参数JSON',
    `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    `execution_time` INT UNSIGNED DEFAULT NULL COMMENT '执行耗时(ms)',
    `operation_result` TINYINT DEFAULT 1 COMMENT '操作结果: 1-成功, 0-失败',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================
-- 二、初始数据
-- =====================================================

-- ── 角色 ──
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `status`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有所有权限', 1),
(2, 'ADMIN', '管理员', '系统管理员', 1),
(3, 'RESEARCHER', '研究员', '研究人员', 1),
(4, 'OBSERVER', '观测员', '野外观测员', 1),
(5, 'VIEWER', '访客', '普通访客', 1);

-- ── 管理员账号 admin / admin123 ──
INSERT INTO `sys_user` (`id`, `username`, `password`, `email`, `real_name`, `status`) VALUES
(1, 'admin', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', 'admin@oceanverse.com', '系统管理员', 1),
(2, 'researcher', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', 'researcher@oceanverse.com', '张研究员', 1),
(3, 'observer', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', 'observer@oceanverse.com', '李观测员', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 3), (3, 4);

-- ── 生态系统初始数据 ──
INSERT INTO `ecosystem` (`ecosystem_code`, `ecosystem_name`, `ecosystem_type`, `description`, `area_estimate`, `depth_min`, `depth_max`, `temperature_range`, `threat_factors`, `conservation_status`) VALUES
('ECO001', '珊瑚礁生态系统', 'CORAL', '热带浅海珊瑚礁生态系统，生物多样性极其丰富', 5000.00, 0.50, 30.00, '22-30℃', '海水升温、酸化、过度捕捞', 'VULNERABLE'),
('ECO002', '红树林生态系统', 'COASTAL', '海岸潮间带红树林湿地生态系统', 200.00, 0.00, 5.00, '18-28℃', '围垦开发、水污染', 'ENDANGERED'),
('ECO003', '深海生态系统', 'DEEP_SEA', '深海海洋生态系统', 128000.00, 200.00, 6000.00, '1-4℃', '深海采矿、气候变化', 'STABLE');

-- ── 初始物种 (SP001-SP003) ──
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `iucn_status`, `family`, `genus`, `description`, `data_quality`) VALUES
('SP001', 'Chelonia mydas', 'Green Sea Turtle', '绿海龟', 'EN', 'Cheloniidae', 'Chelonia', '绿海龟是海龟科中体型较大的一种，广泛分布于热带和亚热带海域。', 'VERIFIED'),
('SP002', 'Neophocaena phocaenoides', 'Finless Porpoise', '江豚', 'VU', 'Phocoenidae', 'Neophocaena', '长江江豚是中国特有的淡水鲸类动物，被称为"水中大熊猫"。', 'VERIFIED'),
('SP003', 'Hippocampus kuda', 'Yellow Seahorse', '海马', 'VU', 'Syngnathidae', 'Hippocampus', '海马是一种小型海洋鱼类，以其独特的马头形状而闻名。', 'VERIFIED');


-- =====================================================
-- 三、权限种子数据
-- =====================================================

-- 一级菜单
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(1,  0, '用户管理', 'system:user',    'MENU', 1),
(2,  0, '角色管理', 'system:role',    'MENU', 2),
(3,  0, '物种管理', 'species',        'MENU', 3),
(4,  0, '社区管理', 'community',      'MENU', 4),
(5,  0, '数据可视化', 'visual',       'MENU', 5),
(6,  0, '日志管理', 'system:log',     'MENU', 6);

-- 用户管理 — 按钮/接口权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(10, 1, '用户列表',   'user:list',    'BUTTON', 1),
(11, 1, '用户详情',   'user:info',    'BUTTON', 2),
(12, 1, '用户新增',   'user:create',  'BUTTON', 3),
(13, 1, '用户编辑',   'user:update',  'BUTTON', 4),
(14, 1, '用户删除',   'user:delete',  'BUTTON', 5),
(15, 1, '分配角色',   'user:assign-role', 'BUTTON', 6),
(16, 1, '强制下线',   'user:force-logout','BUTTON', 7),
(17, 1, '启用/禁用',  'user:status',  'BUTTON', 8);

-- 角色管理 — 按钮/接口权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(20, 2, '角色列表',   'role:list',    'BUTTON', 1),
(21, 2, '角色新增',   'role:create',  'BUTTON', 2),
(22, 2, '角色编辑',   'role:update',  'BUTTON', 3),
(23, 2, '角色删除',   'role:delete',  'BUTTON', 4),
(24, 2, '分配权限',   'role:assign-perm', 'BUTTON', 5);

-- 物种管理 — 按钮/接口权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(30, 3, '物种列表',   'species:list',   'BUTTON', 1),
(31, 3, '物种新增',   'species:create', 'BUTTON', 2),
(32, 3, '物种编辑',   'species:update', 'BUTTON', 3),
(33, 3, '物种删除',   'species:delete', 'BUTTON', 4);

-- 社区管理 — 按钮/接口权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(40, 4, '动态列表',   'community:post:list',   'BUTTON', 1),
(41, 4, '动态审核',   'community:post:audit',  'BUTTON', 2),
(42, 4, '动态删除',   'community:post:delete', 'BUTTON', 3);

-- 日志管理 — 按钮/接口权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(60, 6, '登录日志',   'log:login',    'BUTTON', 1),
(61, 6, '操作日志',   'log:operate',  'BUTTON', 2);

-- =====================================================
-- 四、角色权限分配
-- =====================================================

-- 超级管理员分配全部权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`;

-- 管理员分配: 用户管理(查看+启用禁用)、角色管理(查看)、日志管理
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 1), (2, 10), (2, 11), (2, 14),
(2, 2), (2, 20),
(2, 6), (2, 60), (2, 61);

-- 研究员分配: 物种管理(全部)、社区管理(查看)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 3), (3, 30), (3, 31), (3, 32), (3, 33),
(3, 4), (3, 40);

-- 观测员分配: 物种管理(查看)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4, 3), (4, 30);

-- SUPER_ADMIN 绑定全部权限 (兜底，避免遗漏)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0), p.id
FROM sys_permission p;

-- 用户 ID=1 分配 SUPER_ADMIN
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 1, id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0;


-- =====================================================
-- 五、扩展物种数据 (SP004-SP018)
-- =====================================================

-- SP004 大白鲨
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP004', 'Carcharodon carcharias', 'Great White Shark', '大白鲨', '2', 'VU', 'Animalia', 'Chordata', 'Chondrichthyes', 'Lamniformes', 'Lamnidae', 'Carcharodon', 'C. carcharias',
'大白鲨是世界上最大的掠食性鱼类之一，广泛分布于全球温带和热带海域。它们是海洋食物链的顶级捕食者，对维持海洋生态平衡起着至关重要的作用。',
'大白鲨体长可达6米，体重可达2吨。身体呈流线型，背部深灰色，腹部白色。拥有300颗锋利的锯齿状牙齿，咬合力极强。',
'大白鲨是独居动物，主要在近海和远洋活动。以海豹、海狮、鱼类和其他鲨鱼为食。具有极强的嗅觉和电感知能力。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP005 鲸鲨
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP005', 'Rhincodon typus', 'Whale Shark', '鲸鲨', '2', 'EN', 'Animalia', 'Chordata', 'Chondrichthyes', 'Orectolobiformes', 'Rhincodontidae', 'Rhincodon', 'R. typus',
'鲸鲨是世界上最大的鱼类，体长可达18米。尽管体型庞大，它们却是温和的滤食性动物，以浮游生物和小型鱼类为食。广泛分布于热带和暖温带海域。',
'鲸鲨身体呈深灰色，布满白色斑点和条纹，每只鲸鲨的斑点图案都是独一无二的。头部宽大扁平，嘴巴可达1.5米宽。',
'鲸鲨通常独居或成小群活动，常在表层水域缓慢游动滤食。具有长距离迁徙习性，会跟随浮游生物的季节性分布移动。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP006 中华白海豚
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP006', 'Sousa chinensis', 'Indo-Pacific Humpback Dolphin', '中华白海豚', '1', 'EN', 'Animalia', 'Chordata', 'Mammalia', 'Artiodactyla', 'Delphinidae', 'Sousa', 'S. chinensis',
'中华白海豚是中国国家一级保护动物，主要分布于中国东南沿海及东南亚海域。它们以粉灰色的身体和背部的驼峰状突起为特征，被誉为"海上大熊猫"。',
'成年中华白海豚体长2-2.8米，体重约200-260公斤。幼年时身体呈深灰色，随年龄增长逐渐变为粉白色。背鳍低矮呈驼峰状。',
'中华白海豚通常以小群活动，喜欢在浅海近岸区域觅食。以小型鱼类和头足类为食，具有良好的回声定位能力。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP007 蓝鲸
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP007', 'Balaenoptera musculus', 'Blue Whale', '蓝鲸', '2', 'EN', 'Animalia', 'Chordata', 'Mammalia', 'Artiodactyla', 'Balaenopteridae', 'Balaenoptera', 'B. musculus',
'蓝鲸是地球上有史以来体型最大的动物，体长可达30米，体重可达180吨。它们广泛分布于全球各大洋，是须鲸科中最大的成员。',
'蓝鲸身体呈流线型，背部蓝灰色带有斑点花纹，腹部颜色较浅。头部约占体长的四分之一，喉腹褶可达60条以上。',
'蓝鲸通常独居或以2-3头小群活动，以磷虾为主要食物。每天可消耗约4吨磷虾，具有长距离季节性迁徙习性。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP008 虎鲸
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP008', 'Orcinus orca', 'Orca', '虎鲸', '2', 'DD', 'Animalia', 'Chordata', 'Mammalia', 'Artiodactyla', 'Delphinidae', 'Orcinus', 'O. orca',
'虎鲸是海豚科中体型最大的成员，也是海洋中最聪明的捕食者之一。它们以标志性的黑白配色和高度社会化的群体行为闻名于世。',
'虎鲸体长6-9米，体重3-6吨。身体黑白分明，眼后有一白色椭圆形斑块，背鳍高大呈三角形，雄性背鳍可达1.8米。',
'虎鲸高度社会化，以母系家族群为单位生活。不同生态型有独特的捕猎策略和方言，以鱼类、海豹甚至其他鲸类为食。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP009 蝠鲼
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP009', 'Mobula birostris', 'Giant Oceanic Manta Ray', '蝠鲼', '2', 'EN', 'Animalia', 'Chordata', 'Chondrichthyes', 'Myliobatiformes', 'Mobulidae', 'Mobula', 'M. birostris',
'蝠鲼是世界上最大的鳐鱼，翼展可达7米以上。它们以优雅的水下滑翔姿态闻名，是潜水爱好者最向往遇见的海洋生物之一。',
'蝠鲼身体扁平呈菱形，胸鳍宽大如翅膀。背部深灰至黑色，腹部白色带有独特斑点图案。头前有两只可活动的头鳍用于引导食物。',
'蝠鲼是滤食性动物，以浮游生物和小型鱼类为食。常在清洁站接受小鱼清洁，具有高度发达的大脑和好奇心。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP010 小丑鱼
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP010', 'Amphiprion ocellaris', 'Clownfish', '小丑鱼', '3', 'LC', 'Animalia', 'Chordata', 'Actinopterygii', 'Perciformes', 'Pomacentridae', 'Amphiprion', 'A. ocellaris',
'小丑鱼因电影《海底总动员》而家喻户晓。它们与海葵形成著名的互利共生关系，是珊瑚礁生态系统中最具代表性的鱼类之一。',
'小丑鱼体长7-13厘米，身体橙黄色，有三条白色横带，边缘为黑色。胸鳍和尾鳍呈圆形，色彩鲜艳。',
'小丑鱼终生与宿主海葵共生，身上覆盖的特殊黏液可以保护它们不被海葵的触手蜇伤。具有雌性先熟的性别转变能力。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP011 蓝环章鱼
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP011', 'Hapalochlaena lunulata', 'Blue-ringed Octopus', '蓝环章鱼', '3', 'LC', 'Animalia', 'Mollusca', 'Cephalopoda', 'Octopoda', 'Octopodidae', 'Hapalochlaena', 'H. lunulata',
'蓝环章鱼是世界上毒性最强的海洋生物之一，其毒液可在数分钟内致人死亡。尽管体型小巧，它们鲜艳的蓝环花纹是一种强烈的警告色。',
'蓝环章鱼体长仅12-20厘米，身体黄褐色，全身布满50-60个亮蓝色圆环。受到威胁时蓝色环纹会变得更加鲜艳。',
'蓝环章鱼生活在浅海珊瑚礁和岩石区域，以小型甲壳类和鱼类为食。通常独居且夜行性，使用河豚毒素进行防御和捕猎。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP012 儒艮
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP012', 'Dugong dugon', 'Dugong', '儒艮', '1', 'VU', 'Animalia', 'Chordata', 'Mammalia', 'Sirenia', 'Dugongidae', 'Dugong', 'D. dugon',
'儒艮是海牛目中唯一以海草为主食的物种，也是中国古代"美人鱼"传说的原型。它们性情温顺，在全球热带浅海区域有零星分布。',
'儒艮体长2.5-4米，体重230-500公斤。身体呈纺锤形，皮肤灰褐色，尾鳍呈新月形。上唇宽大扁平，布满粗硬的触须。',
'儒艮通常在浅海海草床觅食，每天需消耗约30公斤海草。性情温和，行动缓慢，通常独居或母子对活动。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP013 翻车鱼
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP013', 'Mola mola', 'Ocean Sunfish', '翻车鱼', '3', 'VU', 'Animalia', 'Chordata', 'Actinopterygii', 'Tetraodontiformes', 'Molidae', 'Mola', 'M. mola',
'翻车鱼是世界上体型最重的硬骨鱼类，体重可达2.3吨。它们奇特的外形像是被截断了尾巴，是远洋海域中令人着迷的奇异生物。',
'翻车鱼体长可达3.3米，身体扁平呈圆盘状，银灰色皮肤粗糙如砂纸。没有真正的尾鳍，取而代之的是由背鳍和臀鳍愈合形成的"假尾"。',
'翻车鱼主要以水母为食，也吃浮游生物和小型鱼类。经常在表层水域侧身晒太阳，被认为是为了驱除体表寄生虫或让海鸟啄食寄生虫。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP014 锤头鲨
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP014', 'Sphyrna mokarran', 'Great Hammerhead', '锤头鲨', '2', 'CR', 'Animalia', 'Chordata', 'Chondrichthyes', 'Carcharhiniformes', 'Sphyrnidae', 'Sphyrna', 'S. mokarran',
'锤头鲨是双髻鲨科中体型最大的成员，以其独特的T形头锤而闻名。它们是热带和亚热带海域的高效捕食者，尤其擅长捕食魟鱼。',
'锤头鲨体长可达6米，体重约230公斤。头部呈宽阔的T形锤状，眼睛位于锤头两端，提供接近360度的视野。身体灰褐色，第一背鳍高大。',
'锤头鲨通常独居，在海底搜寻魟鱼和章鱼为食。头部两侧的电感受器能精准探测猎物发出的电信号，具有极高的捕猎效率。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP015 海月水母
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP015', 'Aurelia aurita', 'Moon Jellyfish', '海月水母', '3', 'LC', 'Animalia', 'Cnidaria', 'Scyphozoa', 'Semaeostomeae', 'Ulmaridae', 'Aurelia', 'A. aurita',
'海月水母是最常见的水母种类之一，广泛分布于全球温带和热带海域。它们半透明的伞状身体在阳光下如月亮般美丽，因此得名。',
'海月水母伞径可达40厘米，身体透明至乳白色，内有四个马蹄形生殖腺呈淡紫色。口腕短小，触手细密，整体呈优雅的水母形态。',
'海月水母随水流漂浮，以浮游生物、小型甲壳类和鱼卵为食。它们没有大脑和心脏，通过简单的神经网络感知环境。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP016 玳瑁
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP016', 'Eretmochelys imbricata', 'Hawksbill Sea Turtle', '玳瑁', '1', 'CR', 'Animalia', 'Chordata', 'Reptilia', 'Testudines', 'Cheloniidae', 'Eretmochelys', 'E. imbricata',
'玳瑁是一种极度濒危的海龟，因其美丽的龟甲花纹而长期遭到过度捕猎。它们是珊瑚礁生态系统中重要的维护者，以海绵为主要食物。',
'玳瑁体长60-100厘米，体重45-75公斤。背甲呈覆瓦状排列，具有琥珀色、棕色和黄色的华丽花纹。喙部尖锐如鹰嘴，因此得名。',
'玳瑁主要栖息在热带珊瑚礁区域，以海绵为主食。它们能食用含毒素的海绵而不受影响，对维持珊瑚礁海绵群落平衡有重要作用。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- SP017 狮子鱼
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP017', 'Pterois volitans', 'Red Lionfish', '狮子鱼', '3', 'LC', 'Animalia', 'Chordata', 'Actinopterygii', 'Scorpaeniformes', 'Scorpaenidae', 'Pterois', 'P. volitans',
'狮子鱼以其华丽的鳍条和鲜艳的条纹著称，是珊瑚礁中最引人注目的鱼类之一。然而它们在大西洋已成为危害严重的入侵物种。',
'狮子鱼体长可达38厘米，红白相间的条纹布满全身。胸鳍宽大如扇，背鳍和臀鳍的棘条含有剧毒。外形如同一只水下展翅的雄狮。',
'狮子鱼原产于印度-太平洋海域，在大西洋和加勒比海为入侵物种。它们是伏击型捕食者，能吞下相当于自身三分之二大小的猎物。', 0, 1, 'VERIFIED', 'IUCN Red List');

-- SP018 鹦嘴鱼
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP018', 'Chlorurus spilurus', 'Pacific Parrotfish', '鹦嘴鱼', '3', 'LC', 'Animalia', 'Chordata', 'Actinopterygii', 'Perciformes', 'Scaridae', 'Chlorurus', 'C. spilurus',
'鹦嘴鱼因其鹦鹉般的嘴部而得名，是珊瑚礁生态系统中最重要的"清洁工"。它们啃食珊瑚并排泄出白色沙粒，是热带海滩沙子的重要来源。',
'鹦嘴鱼体长30-50厘米，体色随性别和年龄变化。雄性呈鲜艳的青绿色至蓝绿色，雌性颜色较暗淡。牙齿融合成喙状，能轻松咬碎珊瑚。',
'鹦嘴鱼白天在珊瑚礁觅食藻类和珊瑚，夜间分泌黏液茧包裹自身以防寄生虫。每只鹦嘴鱼每年可产生约100公斤白沙。', 0, 0, 'VERIFIED', 'IUCN Red List');


-- =====================================================
-- 六、物种媒体数据 (SP001-SP018)
-- =====================================================

-- 绿海龟 (SP001)
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP001' AND deleted = 0 LIMIT 1), 'IMAGE', 'green_sea_turtle_1.png', '/test-images/green_sea_turtle_1.png', 1302117, '绿海龟在珊瑚礁海域游泳', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP001' AND deleted = 0 LIMIT 1), 'IMAGE', 'green_sea_turtle_2.png', '/test-images/green_sea_turtle_2.png', 1144183, '绿海龟浮出水面', 0, 1);

-- 江豚 (SP002)
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP002' AND deleted = 0 LIMIT 1), 'IMAGE', 'finless_porpoise_1.png', '/test-images/finless_porpoise_1.png', 995356, '长江江豚在水中游动', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP002' AND deleted = 0 LIMIT 1), 'IMAGE', 'finless_porpoise_2.png', '/test-images/finless_porpoise_2.png', 986628, '两只江豚结伴而行', 0, 1);

-- 海马 (SP003)
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP003' AND deleted = 0 LIMIT 1), 'IMAGE', 'yellow_seahorse_1.png', '/test-images/yellow_seahorse_1.png', 1088956, '海马附着在海草上', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP003' AND deleted = 0 LIMIT 1), 'IMAGE', 'yellow_seahorse_2.png', '/test-images/yellow_seahorse_2.png', 1216051, '一对海马共同游动', 0, 1);

-- 大白鲨 (SP004)
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP004' AND deleted = 0 LIMIT 1), 'IMAGE', 'great_white_shark_1.png', '/test-images/great_white_shark_1.png', 1008527, '大白鲨在深蓝海水中游弋', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP004' AND deleted = 0 LIMIT 1), 'IMAGE', 'great_white_shark_2.png', '/test-images/great_white_shark_2.png', 1053568, '大白鲨展示白色腹部和牙齿', 0, 1);

-- 鲸鲨 (SP005)
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP005' AND deleted = 0 LIMIT 1), 'IMAGE', 'whale_shark_1.png', '/test-images/whale_shark_1.png', 1127237, '鲸鲨展示独特的斑点花纹', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP005' AND deleted = 0 LIMIT 1), 'IMAGE', 'whale_shark_2.png', '/test-images/whale_shark_2.png', 1186589, '鲸鲨与潜水员同框展示巨大体型', 0, 1);

-- SP006 中华白海豚
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP006' AND deleted = 0 LIMIT 1), 'IMAGE', 'humpback_dolphin_1.png', '/test-images/humpback_dolphin_1.png', 1024576, '中华白海豚在清澈海水中游泳', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP006' AND deleted = 0 LIMIT 1), 'IMAGE', 'humpback_dolphin_2.png', '/test-images/humpback_dolphin_2.png', 1156096, '一群中华白海豚跃出水面', 0, 1);

-- SP007 蓝鲸
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP007' AND deleted = 0 LIMIT 1), 'IMAGE', 'blue_whale_1.png', '/test-images/blue_whale_1.png', 1245184, '蓝鲸在深蓝海水中游弋', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP007' AND deleted = 0 LIMIT 1), 'IMAGE', 'blue_whale_2.png', '/test-images/blue_whale_2.png', 1098752, '蓝鲸浮出水面呼吸', 0, 1);

-- SP008 虎鲸
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP008' AND deleted = 0 LIMIT 1), 'IMAGE', 'orca_1.png', '/test-images/orca_1.png', 1134592, '虎鲸展示标志性的黑白花纹', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP008' AND deleted = 0 LIMIT 1), 'IMAGE', 'orca_2.png', '/test-images/orca_2.png', 1267712, '虎鲸母子在深海中同游', 0, 1);

-- SP009 蝠鲼
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP009' AND deleted = 0 LIMIT 1), 'IMAGE', 'manta_ray_1.png', '/test-images/manta_ray_1.png', 1189888, '蝠鲼在蓝色深海中优雅滑翔', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP009' AND deleted = 0 LIMIT 1), 'IMAGE', 'manta_ray_2.png', '/test-images/manta_ray_2.png', 1312768, '蝠鲼翻转身体滤食浮游生物', 0, 1);

-- SP010 小丑鱼
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP010' AND deleted = 0 LIMIT 1), 'IMAGE', 'clownfish_1.png', '/test-images/clownfish_1.png', 945152, '小丑鱼在海葵触手中探头', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP010' AND deleted = 0 LIMIT 1), 'IMAGE', 'clownfish_2.png', '/test-images/clownfish_2.png', 1078272, '两只小丑鱼在海葵中嬉戏', 0, 1);

-- SP011 蓝环章鱼
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP011' AND deleted = 0 LIMIT 1), 'IMAGE', 'blue_ringed_octopus_1.png', '/test-images/blue_ringed_octopus_1.png', 878592, '蓝环章鱼展示鲜艳的蓝色环纹', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP011' AND deleted = 0 LIMIT 1), 'IMAGE', 'blue_ringed_octopus_2.png', '/test-images/blue_ringed_octopus_2.png', 912384, '蓝环章鱼在水中游动', 0, 1);

-- SP012 儒艮
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP012' AND deleted = 0 LIMIT 1), 'IMAGE', 'dugong_1.png', '/test-images/dugong_1.png', 1167360, '儒艮在海草床上觅食', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP012' AND deleted = 0 LIMIT 1), 'IMAGE', 'dugong_2.png', '/test-images/dugong_2.png', 1089536, '儒艮缓慢游过浅海海草区', 0, 1);

-- SP013 翻车鱼
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP013' AND deleted = 0 LIMIT 1), 'IMAGE', 'ocean_sunfish_1.png', '/test-images/ocean_sunfish_1.png', 1234944, '翻车鱼在水面侧身晒太阳', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP013' AND deleted = 0 LIMIT 1), 'IMAGE', 'ocean_sunfish_2.png', '/test-images/ocean_sunfish_2.png', 1156096, '翻车鱼在珊瑚礁接受清洁鱼服务', 0, 1);

-- SP014 锤头鲨
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP014' AND deleted = 0 LIMIT 1), 'IMAGE', 'hammerhead_shark_1.png', '/test-images/hammerhead_shark_1.png', 1098752, '锤头鲨展示独特的T形头锤', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP014' AND deleted = 0 LIMIT 1), 'IMAGE', 'hammerhead_shark_2.png', '/test-images/hammerhead_shark_2.png', 1245184, '锤头鲨群在深海中编队游弋', 0, 1);

-- SP015 海月水母
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP015' AND deleted = 0 LIMIT 1), 'IMAGE', 'moon_jellyfish_1.png', '/test-images/moon_jellyfish_1.png', 786432, '海月水母在水中优雅漂浮', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP015' AND deleted = 0 LIMIT 1), 'IMAGE', 'moon_jellyfish_2.png', '/test-images/moon_jellyfish_2.png', 845824, '多只海月水母群聚漂浮', 0, 1);

-- SP016 玳瑁
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP016' AND deleted = 0 LIMIT 1), 'IMAGE', 'hawksbill_turtle_1.png', '/test-images/hawksbill_turtle_1.png', 1189888, '玳瑁在珊瑚礁上空游泳', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP016' AND deleted = 0 LIMIT 1), 'IMAGE', 'hawksbill_turtle_2.png', '/test-images/hawksbill_turtle_2.png', 1267712, '玳瑁在珊瑚礁中觅食海绵', 0, 1);

-- SP017 狮子鱼
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP017' AND deleted = 0 LIMIT 1), 'IMAGE', 'lionfish_1.png', '/test-images/lionfish_1.png', 945152, '狮子鱼展开华丽的鳍条', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP017' AND deleted = 0 LIMIT 1), 'IMAGE', 'lionfish_2.png', '/test-images/lionfish_2.png', 1012736, '狮子鱼在珊瑚礁上捕猎', 0, 1);

-- SP018 鹦嘴鱼
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP018' AND deleted = 0 LIMIT 1), 'IMAGE', 'parrotfish_1.png', '/test-images/parrotfish_1.png', 1078272, '鹦嘴鱼啃食珊瑚上的藻类', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP018' AND deleted = 0 LIMIT 1), 'IMAGE', 'parrotfish_2.png', '/test-images/parrotfish_2.png', 1134592, '鹦嘴鱼展示鲜艳的青绿色鳞片', 0, 1);


-- =====================================================
-- 七、观测地点数据
-- =====================================================

INSERT INTO observation_location (location_code, location_name, location_type, latitude, longitude, country, province, city, ecosystem_id) VALUES
('LOC001', '三亚蜈支洲岛', 'ISLAND', 18.310000, 109.760000, '中国', '海南省', '三亚市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1)),
('LOC002', '西沙永兴岛', 'ISLAND', 16.830000, 112.330000, '中国', '海南省', '三沙市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1)),
('LOC003', '深圳大鹏湾', 'COASTAL', 22.560000, 114.490000, '中国', '广东省', '深圳市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO002' AND deleted = 0 LIMIT 1)),
('LOC004', '厦门鼓浪屿', 'COASTAL', 24.445000, 118.065000, '中国', '福建省', '厦门市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO002' AND deleted = 0 LIMIT 1)),
('LOC005', '青岛胶州湾', 'BAY', 36.070000, 120.230000, '中国', '山东省', '青岛市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO003' AND deleted = 0 LIMIT 1)),
('LOC006', '大连老虎滩', 'COASTAL', 38.870000, 121.320000, '中国', '辽宁省', '大连市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO003' AND deleted = 0 LIMIT 1)),
('LOC007', '涠洲岛', 'ISLAND', 21.050000, 109.110000, '中国', '广西壮族自治区', '北海市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1)),
('LOC008', '南沙美济礁', 'REEF', 9.920000, 115.520000, '中国', '海南省', '三沙市',
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1));


-- =====================================================
-- 八、观测记录数据
-- =====================================================

INSERT INTO observation (observation_code, observation_type, observation_date, observation_time, duration_minutes,
    location_id, ecosystem_id, latitude, longitude, depth, water_temperature, salinity, ph,
    weather_condition, sea_condition, observer_name, organization, equipment_used, notes) VALUES
('OBS001', 'DIVE', '2025-03-15', '09:30:00', 120,
    (SELECT id FROM observation_location WHERE location_code = 'LOC001' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1),
    18.310000, 109.760000, 15.50, 26.80, 35.20, 8.15,
    'SUNNY', 'CALM', '张研究员', '中国科学院海洋研究所', 'SCUBA设备、水下相机',
    '在蜈支洲岛珊瑚礁区域进行潜水观测，记录到多种热带鱼类和活珊瑚群落，珊瑚覆盖率约65%。'),

('OBS002', 'SURVEY', '2025-04-02', '07:00:00', 240,
    (SELECT id FROM observation_location WHERE location_code = 'LOC002' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1),
    16.830000, 112.330000, 8.00, 28.50, 34.80, 8.20,
    'CLOUDY', 'MODERATE', '李观测员', '国家海洋局南海分局', '样方框、GPS定位仪、水质分析仪',
    '西沙永兴岛周边珊瑚礁生态调查，使用1m×1m样方框进行底栖生物调查。'),

('OBS003', 'SIGHTING', '2025-02-20', '14:00:00', 60,
    (SELECT id FROM observation_location WHERE location_code = 'LOC003' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO002' AND deleted = 0 LIMIT 1),
    22.560000, 114.490000, NULL, 19.20, 33.50, 8.05,
    'OVERCAST', 'SLIGHT', '王博士', '深圳大学海洋生物研究中心', '双筒望远镜、高速相机',
    '在大鹏湾沿岸观测到一群中华白海豚（约8头），群体中有2头幼豚。'),

('OBS004', 'TRACKING', '2025-05-10', '06:00:00', 480,
    (SELECT id FROM observation_location WHERE location_code = 'LOC004' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO002' AND deleted = 0 LIMIT 1),
    24.445000, 118.065000, 3.20, 22.10, 34.10, 8.10,
    'SUNNY', 'CALM', '陈教授', '厦门大学海洋与地球学院', '声学追踪器、水听器阵列',
    '对厦门海域标记的3只绿海龟进行声学追踪，记录其活动轨迹和潜水行为。'),

('OBS005', 'DIVE', '2024-11-08', '10:00:00', 90,
    (SELECT id FROM observation_location WHERE location_code = 'LOC005' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO003' AND deleted = 0 LIMIT 1),
    36.070000, 120.230000, 12.00, 14.50, 31.80, 7.95,
    'CLOUDY', 'MODERATE', '张研究员', '中国科学院海洋研究所', 'ROV水下机器人、CTD温盐深仪',
    '胶州湾深水区域使用ROV进行底栖生物观测，发现大面积牡蛎礁群落。'),

('OBS006', 'SURVEY', '2024-08-22', '08:30:00', 180,
    (SELECT id FROM observation_location WHERE location_code = 'LOC006' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO003' AND deleted = 0 LIMIT 1),
    38.870000, 121.320000, 5.50, 22.30, 30.50, 8.00,
    'SUNNY', 'SLIGHT', '赵助理', '大连海洋大学', '拖网、浮游生物网、多参数水质仪',
    '大连老虎滩海域夏季浮游生物调查，采集浮游植物和浮游动物样品。'),

('OBS007', 'SIGHTING', '2025-01-15', '16:30:00', 45,
    (SELECT id FROM observation_location WHERE location_code = 'LOC007' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1),
    21.050000, 109.110000, NULL, 18.60, 34.50, 8.12,
    'SUNNY', 'CALM', '王博士', '深圳大学海洋生物研究中心', '无人机、长焦相机',
    '涠洲岛海域空中观测到一头布氏鲸在水面活动，持续时间约20分钟。'),

('OBS008', 'DIVE', '2024-06-18', '09:00:00', 150,
    (SELECT id FROM observation_location WHERE location_code = 'LOC008' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1),
    9.920000, 115.520000, 25.00, 29.80, 36.20, 8.25,
    'SUNNY', 'CALM', '李观测员', '国家海洋局南海分局', '深海潜水器、采样机械臂',
    '南沙美济礁深水区域深潜观测，记录到多种深海珊瑚和底栖鱼类。'),

('OBS009', 'SURVEY', '2025-06-01', '06:30:00', 300,
    (SELECT id FROM observation_location WHERE location_code = 'LOC001' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1),
    18.310000, 109.760000, 10.00, 28.20, 35.00, 8.18,
    'CLOUDY', 'SLIGHT', '陈教授', '厦门大学海洋与地球学院', '样线法设备、水下摄像机',
    '蜈支洲岛珊瑚礁年度健康评估调查，采用50m样线法记录珊瑚覆盖率和鱼类密度。'),

('OBS010', 'TRACKING', '2024-12-05', '05:00:00', 720,
    (SELECT id FROM observation_location WHERE location_code = 'LOC003' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO002' AND deleted = 0 LIMIT 1),
    22.560000, 114.490000, NULL, 16.80, 33.20, 7.90,
    'OVERCAST', 'ROUGH', '赵助理', '大连海洋大学', '卫星标签、Argos接收器',
    '大鹏湾海域为2头中华白海豚安装卫星追踪标签，监测冬季迁徙路线。'),

('OBS011', 'DIVE', '2025-04-20', '08:00:00', 100,
    (SELECT id FROM observation_location WHERE location_code = 'LOC007' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO001' AND deleted = 0 LIMIT 1),
    21.050000, 109.110000, 18.00, 24.50, 35.80, 8.08,
    'SUNNY', 'CALM', '张研究员', '中国科学院海洋研究所', 'SCUBA设备、水下摄影灯',
    '涠洲岛夜间潜水观测，记录到多种夜行性海洋生物包括龙虾和章鱼。'),

('OBS012', 'SIGHTING', '2025-05-28', '10:30:00', 90,
    (SELECT id FROM observation_location WHERE location_code = 'LOC004' AND deleted = 0 LIMIT 1),
    (SELECT id FROM ecosystem WHERE ecosystem_code = 'ECO002' AND deleted = 0 LIMIT 1),
    24.445000, 118.065000, NULL, 23.40, 34.00, 8.10,
    'SUNNY', 'SLIGHT', '王博士', '深圳大学海洋生物研究中心', '观鲸船、水听器',
    '鼓浪屿附近海域观测到一群约15头的印太江豚，群体活动状态良好。');


-- =====================================================
-- 九、社区测试数据
-- =====================================================

-- ── 帖子 (community_post) ──
INSERT INTO `community_post` (`user_id`, `content`, `post_type`, `image_urls`, `like_count`, `comment_count`, `favorite_count`, `status`, `create_time`) VALUES

-- admin 的帖子
(1, '今天在西沙群岛拍到了绿海龟！海水清澈见底，珊瑚礁生态保持得很好。', 'OBSERVATION',
 '["https://images.unsplash.com/photo-1591025207163-942350e47db2?w=800","https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800","https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?w=800"]',
 12, 3, 5, 1, NOW() - INTERVAL 5 DAY),

(1, '分享一组深海生物的照片，深海的世界真的太神奇了！', 'NORMAL',
 '["https://images.unsplash.com/photo-1551244072-5d12893278ab?w=800","https://images.unsplash.com/photo-1546026423-cc4642628d2b?w=800"]',
 8, 2, 3, 1, NOW() - INTERVAL 3 DAY),

(1, '识别到一只江豚，正在长江中下游水域活动。种群数量似乎有所恢复，是个好兆头。', 'RECOGNITION',
 '["https://images.unsplash.com/photo-1568430462989-44163eb1752f?w=800"]',
 15, 4, 7, 1, NOW() - INTERVAL 1 DAY),

-- researcher 的帖子
(2, '完成了本月的珊瑚礁调查报告。南海区域的珊瑚白化现象有所缓解，但仍需持续关注。', 'OBSERVATION',
 '["https://images.unsplash.com/photo-1546026423-cc4642628d2b?w=800","https://images.unsplash.com/photo-1582967788606-a171c7a4e01b?w=800","https://images.unsplash.com/photo-1571752726703-5e7d1f6a986d?w=800"]',
 20, 6, 10, 1, NOW() - INTERVAL 4 DAY),

(2, '今天在实验室分析了一批海水样本，发现 microplastics 浓度比上个月有所上升。海洋污染问题依然严峻。', 'NORMAL',
 '["https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=800","https://images.unsplash.com/photo-1576086213369-97a306d36557?w=800"]',
 6, 1, 2, 1, NOW() - INTERVAL 2 DAY),

(2, '海马识别结果：确认为 Yellow Seahorse (Hippocampus kuda)，位于广东沿海海草床区域。', 'RECOGNITION',
 '["https://images.unsplash.com/photo-1596417830157-59b1e80a5e4f?w=800","https://images.unsplash.com/photo-1583212292454-1fe6229603b7?w=800"]',
 18, 5, 8, 1, NOW() - INTERVAL 6 HOUR),

-- observer 的帖子
(3, '周末去了红树林湿地观鸟，记录到了白鹭、苍鹭等多种水鸟。生态环境保护初见成效！', 'OBSERVATION',
 '["https://images.unsplash.com/photo-1444464666168-49d633b86797?w=800","https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800","https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800"]',
 10, 2, 4, 1, NOW() - INTERVAL 7 DAY),

(3, '潜水时遇到了一群水母，阳光透过水面照在它们身上，像会发光的灯笼一样美。', 'NORMAL',
 '["https://images.unsplash.com/photo-1545671913-b89ac1b4ac10?w=800","https://images.unsplash.com/photo-1437622368342-7a3d73a34c8f?w=800"]',
 25, 8, 12, 1, NOW() - INTERVAL 10 HOUR),

(3, '今天在码头附近发现了一只被困的海龟，已经联系了海洋救助站。希望大家出海时注意保护海洋生物。', 'NORMAL',
 '["https://images.unsplash.com/photo-1518467166778-b88f373ffec7?w=800"]',
 30, 10, 15, 2, NOW() - INTERVAL 2 DAY);


-- ── 评论 (community_comment) ──
INSERT INTO `community_comment` (`post_id`, `user_id`, `content`, `like_count`, `create_time`) VALUES
-- 帖子1的评论
(1, 2, '太美了！西沙的水质确实很好，绿海龟能在那里栖息说明生态保护做得不错。', 2, NOW() - INTERVAL 5 DAY + INTERVAL 1 HOUR),
(1, 3, '羡慕！我也一直想去西沙潜水，下次组队一起？', 1, NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR),
(1, 1, '欢迎欢迎！西沙的珊瑚礁真的很壮观，强烈推荐。', 0, NOW() - INTERVAL 5 DAY + INTERVAL 3 HOUR),

-- 帖子2的评论
(2, 3, '深海生物都长得很奇特，那个发光的是什么物种？', 1, NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR),
(2, 2, '看起来像是深海鮟鱇鱼，利用生物发光来捕食。', 3, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),

-- 帖子3的评论
(3, 2, '江豚种群恢复是个好趋势！长江十年禁渔计划看来有效果。', 2, NOW() - INTERVAL 1 DAY + INTERVAL 1 HOUR),
(3, 3, '之前在鄱阳湖也看到过几头，希望它们的数量能继续增加。', 1, NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR),
(3, 1, '是的，我们团队也在持续监测江豚的活动范围。', 0, NOW() - INTERVAL 1 DAY + INTERVAL 3 HOUR),
(3, 2, '期待你们的监测报告！数据共享对研究很有帮助。', 1, NOW() - INTERVAL 1 DAY + INTERVAL 4 HOUR),

-- 帖子4的评论
(4, 1, '张研究员辛苦了！珊瑚白化缓解是个好消息。', 1, NOW() - INTERVAL 4 DAY + INTERVAL 1 HOUR),
(4, 3, '报告能公开吗？想学习一下调查方法。', 0, NOW() - INTERVAL 4 DAY + INTERVAL 2 HOUR),
(4, 1, '报告会上传到平台的数据共享模块，敬请关注。', 2, NOW() - INTERVAL 4 DAY + INTERVAL 3 HOUR),
(4, 3, '太好了！感谢分享。', 0, NOW() - INTERVAL 4 DAY + INTERVAL 4 HOUR),
(4, 2, '也欢迎大家参与下个月的珊瑚礁调查志愿活动。', 1, NOW() - INTERVAL 4 DAY + INTERVAL 5 HOUR),
(4, 1, '报名！算我一个。', 0, NOW() - INTERVAL 4 DAY + INTERVAL 6 HOUR),

-- 帖子5的评论
(5, 1, 'Microplastics 问题确实不容忽视，期待你们的研究成果。', 1, NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR),

-- 帖子6的评论
(6, 3, '海马好可爱！它们在海草床里伪装得真好。', 2, NOW() - INTERVAL 6 HOUR + INTERVAL 10 MINUTE),
(6, 1, 'Yellow Seahorse 属于易危物种，发现栖息地很重要。', 1, NOW() - INTERVAL 6 HOUR + INTERVAL 20 MINUTE),
(6, 3, '广东沿海的海草床近年来恢复得不错，希望它们能好好繁衍。', 0, NOW() - INTERVAL 6 HOUR + INTERVAL 30 MINUTE),
(6, 1, '这个发现应该上报给当地的海洋保护区管理部门。', 1, NOW() - INTERVAL 6 HOUR + INTERVAL 40 MINUTE),
(6, 2, '已经提交了观测记录，管理部门会跟进的。', 0, NOW() - INTERVAL 6 HOUR + INTERVAL 50 MINUTE),

-- 帖子7的评论
(7, 1, '红树林湿地是候鸟的重要栖息地，保护红树林就是保护鸟类。', 1, NOW() - INTERVAL 7 DAY + INTERVAL 1 HOUR),
(7, 2, '白鹭和苍鹭都是环境指示物种，它们的出现说明湿地生态健康。', 2, NOW() - INTERVAL 7 DAY + INTERVAL 2 HOUR),

-- 帖子8的评论
(8, 1, '水母好梦幻！是什么品种？', 0, NOW() - INTERVAL 10 HOUR + INTERVAL 10 MINUTE),
(8, 2, '看起来像海月水母，无毒的，可以放心欣赏。', 1, NOW() - INTERVAL 10 HOUR + INTERVAL 15 MINUTE),
(8, 1, '哈哈好的，下次遇到可以近距离观察了。', 0, NOW() - INTERVAL 10 HOUR + INTERVAL 20 MINUTE),
(8, 3, '水母爆发可能跟海水温度升高有关，值得关注。', 2, NOW() - INTERVAL 10 HOUR + INTERVAL 25 MINUTE),
(8, 1, '有道理，全球变暖对海洋生态的影响越来越大了。', 1, NOW() - INTERVAL 10 HOUR + INTERVAL 30 MINUTE),
(8, 2, '建议记录一下当时的水温和盐度数据。', 0, NOW() - INTERVAL 10 HOUR + INTERVAL 35 MINUTE),
(8, 3, '好的，下次注意收集环境参数。', 0, NOW() - INTERVAL 10 HOUR + INTERVAL 40 MINUTE),
(8, 1, '这才是科学观测该有的态度！', 1, NOW() - INTERVAL 10 HOUR + INTERVAL 45 MINUTE),

-- 帖子9的评论
(9, 1, '为你点赞！救助海洋生物是每个人的责任。', 3, NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR),
(9, 2, '希望海龟能顺利康复然后回归大海。', 2, NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR),
(9, 1, '已经联系了救助站，他们说会尽力救治。', 1, NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR),
(9, 2, '大家出海时如果遇到被困的海洋生物，请及时报告。', 1, NOW() - INTERVAL 2 DAY + INTERVAL 4 HOUR),
(9, 3, '好的！已保存海洋救助站的联系方式。', 0, NOW() - INTERVAL 2 DAY + INTERVAL 5 HOUR),
(9, 1, '希望更多人能关注海洋保护。', 2, NOW() - INTERVAL 2 DAY + INTERVAL 6 HOUR),
(9, 2, '已转发到社交媒体，让更多人看到。', 1, NOW() - INTERVAL 2 DAY + INTERVAL 7 HOUR),
(9, 3, '海洋保护需要每个人的参与！', 0, NOW() - INTERVAL 2 DAY + INTERVAL 8 HOUR),
(9, 1, '下次出海我也带上救助工具箱。', 1, NOW() - INTERVAL 2 DAY + INTERVAL 9 HOUR),
(9, 2, '这才是正能量的社区氛围！', 0, NOW() - INTERVAL 2 DAY + INTERVAL 10 HOUR);


-- ── 点赞 (community_like) ──
INSERT INTO `community_like` (`user_id`, `target_id`, `target_type`, `create_time`) VALUES
-- 帖子点赞
(2, 1, 'POST', NOW() - INTERVAL 5 DAY),
(3, 1, 'POST', NOW() - INTERVAL 5 DAY),
(2, 3, 'POST', NOW() - INTERVAL 1 DAY),
(1, 4, 'POST', NOW() - INTERVAL 4 DAY),
(3, 4, 'POST', NOW() - INTERVAL 4 DAY),
(1, 6, 'POST', NOW() - INTERVAL 6 HOUR),
(3, 6, 'POST', NOW() - INTERVAL 6 HOUR),
(1, 7, 'POST', NOW() - INTERVAL 7 DAY),
(2, 7, 'POST', NOW() - INTERVAL 7 DAY),
(1, 8, 'POST', NOW() - INTERVAL 10 HOUR),
(2, 8, 'POST', NOW() - INTERVAL 10 HOUR),
(1, 9, 'POST', NOW() - INTERVAL 2 DAY),
(2, 9, 'POST', NOW() - INTERVAL 2 DAY),

-- 评论点赞
(1, 1, 'COMMENT', NOW() - INTERVAL 5 DAY),
(2, 4, 'COMMENT', NOW() - INTERVAL 3 DAY),
(1, 6, 'COMMENT', NOW() - INTERVAL 1 DAY),
(3, 10, 'COMMENT', NOW() - INTERVAL 4 DAY),
(1, 11, 'COMMENT', NOW() - INTERVAL 2 DAY),
(2, 17, 'COMMENT', NOW() - INTERVAL 6 HOUR),
(3, 18, 'COMMENT', NOW() - INTERVAL 6 HOUR),
(1, 22, 'COMMENT', NOW() - INTERVAL 7 DAY),
(2, 23, 'COMMENT', NOW() - INTERVAL 7 DAY),
(1, 30, 'COMMENT', NOW() - INTERVAL 2 DAY),
(2, 31, 'COMMENT', NOW() - INTERVAL 2 DAY),
(3, 33, 'COMMENT', NOW() - INTERVAL 2 DAY);


-- ── 收藏 (community_favorite) ──
INSERT INTO `community_favorite` (`user_id`, `target_id`, `target_type`, `create_time`) VALUES
(2, 1, 'POST', NOW() - INTERVAL 5 DAY),
(3, 1, 'POST', NOW() - INTERVAL 5 DAY),
(1, 3, 'POST', NOW() - INTERVAL 1 DAY),
(1, 4, 'POST', NOW() - INTERVAL 4 DAY),
(3, 4, 'POST', NOW() - INTERVAL 4 DAY),
(1, 6, 'POST', NOW() - INTERVAL 6 HOUR),
(3, 6, 'POST', NOW() - INTERVAL 6 HOUR),
(1, 8, 'POST', NOW() - INTERVAL 10 HOUR),
(2, 8, 'POST', NOW() - INTERVAL 10 HOUR),
(1, 9, 'POST', NOW() - INTERVAL 2 DAY),
(2, 9, 'POST', NOW() - INTERVAL 2 DAY);


-- =====================================================
-- 补充列（来自 new.sql，使用 IF NOT EXISTS 防止重复）
-- =====================================================
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS background_url VARCHAR(500) DEFAULT NULL COMMENT '背景图URL' AFTER avatar_url;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介' AFTER background_url;
ALTER TABLE sys_notification ADD COLUMN IF NOT EXISTS target_post_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标帖子ID' AFTER related_id;
ALTER TABLE sys_notification ADD COLUMN IF NOT EXISTS from_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作者ID' AFTER target_post_id;


-- =====================================================
-- 物种分布数据 (species_distribution) — 共21条
-- =====================================================
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (1, 1, 'NATIVE', '南海西沙群岛', '中国', '海南', 16.831900, 112.332800, 0.00, 40.00, '珊瑚礁', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (2, 2, 'NATIVE', '长江中下游', '中国', '湖北', 30.294000, 114.254000, 0.00, 10.00, '淡水河流', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (3, 3, 'NATIVE', '南海北部沿海', '中国', '广东', 21.508000, 115.368000, 1.00, 30.00, '海草床', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (4, 4, 'NATIVE', '甘斯巴海域', '南非', '西开普省', -34.618000, 19.349000, 0.00, 250.00, '远洋', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (5, 5, 'NATIVE', '董索海域', '菲律宾', '南吕宋', 12.966000, 123.246000, 0.00, 100.00, '远洋', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (6, 13, 'NATIVE', '珠江口中华白海豚保护区', '中国', '广东', 22.120000, 113.780000, 0.00, 20.00, '浅海湾', 1, null, 'RARE', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (7, 14, 'NATIVE', '南极半岛海域', '南极洲', null, -64.812000, -63.512000, 0.00, 500.00, '远洋', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (8, 15, 'NATIVE', '罗弗敦群岛海域', '挪威', '诺尔兰', 68.209400, 14.563000, 0.00, 300.00, '寒冷海域', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-18 16:06:10', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (9, 16, 'NATIVE', '科莫多国家公园', '印度尼西亚', '东努沙登加拉', -8.543000, 119.488000, 0.00, 120.00, '珊瑚礁', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (10, 17, 'NATIVE', '大堡礁', '澳大利亚', '昆士兰', -16.482000, 145.462000, 1.00, 15.00, '珊瑚礁', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (11, 18, 'NATIVE', '悉尼沿海', '澳大利亚', '新南威尔士', -33.856000, 151.262000, 0.00, 20.00, '岩石潮间带', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (12, 19, 'NATIVE', '鲨鱼湾', '澳大利亚', '西澳大利亚', -25.902000, 113.682000, 0.00, 10.00, '海草床', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (13, 20, 'NATIVE', '蒙特雷湾', '美国', '加利福尼亚', 36.802000, -121.945000, 0.00, 200.00, '远洋', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (14, 21, 'NATIVE', '加拉帕戈斯群岛', '厄瓜多尔', null, -0.953000, -90.966000, 0.00, 300.00, '远洋', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (15, 22, 'NATIVE', '濑户内海', '日本', '的冈', 34.573000, 133.768000, 0.00, 20.00, '内海', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (16, 23, 'NATIVE', '加勒比海珊瑚礁', '古巴', null, 21.521000, -82.286000, 0.00, 30.00, '珊瑚礁', 1, null, 'RARE', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (17, 24, 'NATIVE', '图兰奔海域', '印度尼西亚', '巴厘', -8.342000, 115.672000, 1.00, 40.00, '珊瑚礁', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (18, 25, 'NATIVE', '北马累环礁', '马尔代夫', null, 4.374000, 73.532000, 1.00, 25.00, '珊瑚礁', 1, null, 'COMMON', '2026-06-17 11:47:34', '2026-06-17 11:47:34', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (19, 27, 'NATIVE', null, null, null, 30.000000, 120.000000, null, null, null, 1, null, 'COMMON', '2026-06-17 14:36:41', '2026-06-17 14:36:41', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (20, 29, 'NATIVE', null, null, null, 30.000000, 120.000000, null, null, null, 1, null, 'COMMON', '2026-06-18 10:51:07', '2026-06-18 10:51:07', 0);
INSERT INTO oceanverse.species_distribution (id, species_id, distribution_type, region_name, country, province, latitude, longitude, depth_min, depth_max, habitat_type, is_primary, population_estimate, distribution_status, create_time, update_time, deleted) VALUES (21, 34, 'NATIVE', null, null, null, 30.000000, 110.000000, null, null, null, 1, null, 'COMMON', '2026-06-18 11:03:26', '2026-06-18 11:03:54', 21);
-- =====================================================
-- OceanVerse — 权限补充 & 建表问题修复 迁移脚本 v2
-- 用途: 补齐缺失权限码 + 修复建表问题 + 重新分配角色权限
-- 前置: 已执行 oceanverse.sql 完整初始化
-- 执行时间: 2026-06-24
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第〇部分：建表结构与实体类对齐
-- =====================================================

-- =====================================================
-- 第一部分：补充缺失权限码（共 11 条）
-- =====================================================

-- 【权限管理】P1修复: permission:list 在 PermissionController 中被引用但DB不存在
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(7,  0, '权限管理', 'system:permission', 'MENU', 7);

INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(70, 7, '权限列表', 'permission:list', 'BUTTON', 1);

-- 【观测管理】观测CRUD功能已实现但无对应权限码
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(8,  0, '观测管理', 'observation', 'MENU', 8);

INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(80, 8, '观测列表',   'observation:list',    'BUTTON', 1),
(81, 8, '观测新增',   'observation:create',  'BUTTON', 2),
(82, 8, '观测编辑',   'observation:update',  'BUTTON', 3),
(83, 8, '观测删除',   'observation:delete',  'BUTTON', 4);

-- 【社区管理补充】评论删除
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(43, 4, '评论删除', 'community:comment:delete', 'BUTTON', 4);

-- 【社区管理清理】community:post:list 是公开展示不需要权限码
DELETE FROM `sys_role_permission` WHERE `permission_id` = 40;
DELETE FROM `sys_permission` WHERE `id` = 40;

-- 【用户管理清理】移除无对应后端端点的权限码
-- user:info(11), user:create(12), user:delete(14) 在 AdminController 中无对应端点
DELETE FROM `sys_role_permission` WHERE `permission_id` IN (11, 12, 14);
DELETE FROM `sys_permission` WHERE `id` IN (11, 12, 14);

-- 【AI管理】知识库重建端点无保护
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(9,  0, 'AI管理', 'ai', 'MENU', 9);

INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(90, 9, '知识库重建', 'ai:knowledge:rebuild', 'BUTTON', 1);

-- 【数据可视化补充】导出功能
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `name`, `perm_code`, `type`, `sort`) VALUES
(51, 5, '数据导出', 'visual:export', 'BUTTON', 1);


-- =====================================================
-- 第二部分：角色权限重新分配
-- =====================================================

-- ── SUPER_ADMIN（role_id=1）：自动拥有全部权限 ──
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`;

-- ── ADMIN（role_id=2）：用户管理 + 内容管理 ──
-- 原有: 1,10, 2,20, 6,60,61（11,14 已由上方 DELETE 清理）
-- 补充: user:update(13), user:status(17) — 用户状态管理
--       species:list(30)                    — 内容查看
--       community(4), post:audit(41), delete(42), comment:delete(43) — 内容审核
--       permission:list(70), visual:export(51) — 辅助管理
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 13),  (2, 17),
(2, 30),
(2, 4),  (2, 41),  (2, 42),  (2, 43),
(2, 51),  (2, 70);

-- ── RESEARCHER（role_id=3）：物种管理 + 观测录入 ──
-- 原有: 3,30,31,32,33, 4（40 已由上方 DELETE 清理）
-- 补充: observation(8), list(80), create(81), update(82)
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 8),  (3, 80),  (3, 81),  (3, 82);

-- ── OBSERVER（role_id=4）：物种查看 + 观测录入 ──
-- 原有: 3,30
-- 补充: observation(8), list(80), create(81)
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4, 8),  (4, 80),  (4, 81);

-- ── VIEWER（role_id=5）：基础浏览 ──
-- 原有: 无
-- 补充: species(3), species:list(30)
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(5, 3),  (5, 30);


-- =====================================================
-- 第三部分：补充测试账号
-- =====================================================

-- ADMIN 管理员测试账号 (admin2 / admin123)
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES
(4, 'admin2', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', '管理员', 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(4, (SELECT `id` FROM `sys_role` WHERE `role_code` = 'ADMIN' AND `deleted` = 0));

-- VIEWER 访客测试账号 (viewer / admin123)
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES
(5, 'viewer', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', '王访客', 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(5, (SELECT `id` FROM `sys_role` WHERE `role_code` = 'VIEWER' AND `deleted` = 0));


SET FOREIGN_KEY_CHECKS = 1;
