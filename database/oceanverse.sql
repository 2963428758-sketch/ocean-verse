-- =====================================================
-- OceanVerse — 智慧海洋探索平台 数据库脚本
-- =====================================================

CREATE DATABASE IF NOT EXISTS `oceanverse` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `oceanverse`;

SET FOREIGN_KEY_CHECKS = 0;

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
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常, 2-锁定',
    `data_scope` TINYINT NOT NULL DEFAULT 1 COMMENT '数据权限: 1-仅本人, 2-全部',
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
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_species_code` (`species_code`),
    UNIQUE KEY `uk_scientific_name` (`scientific_name`),
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
    `deleted` TINYINT NOT NULL DEFAULT 0,
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
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_species_id` (`species_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种媒体资源表';

-- =====================================================
-- 3. 生态系统与观测模块
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
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ecosystem_code` (`ecosystem_code`)
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
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_location_code` (`location_code`),
    KEY `idx_lat_lng` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观测地点表';

CREATE TABLE `observation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `observation_code` VARCHAR(50) NOT NULL,
    `observation_type` VARCHAR(30) NOT NULL COMMENT 'DIVE/SURVEY/SIGHTING/TRACKING',
    `observation_date` DATE NOT NULL,
    `observation_time` TIME DEFAULT NULL,
    `duration_minutes` INT UNSIGNED DEFAULT NULL,
    `location_id` BIGINT UNSIGNED NOT NULL,
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
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_observation_code` (`observation_code`),
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
-- 5. 社区互动模块 (成员D) — 新增
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
    `related_id` BIGINT UNSIGNED DEFAULT NULL,
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
-- 初始数据
-- =====================================================

-- 角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `status`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有所有权限', 1),
(2, 'ADMIN', '管理员', '系统管理员', 1),
(3, 'RESEARCHER', '研究员', '研究人员', 1),
(4, 'OBSERVER', '观测员', '野外观测员', 1),
(5, 'VIEWER', '访客', '普通访客', 1);

-- 管理员账号 admin / admin123
INSERT INTO `sys_user` (`id`, `username`, `password`, `email`, `real_name`, `status`) VALUES
(1, 'admin', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', 'admin@oceanverse.com', '系统管理员', 1),
(2, 'researcher', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', 'researcher@oceanverse.com', '张研究员', 1),
(3, 'observer', '$2a$10$gDsgAg9lLg5MsxAsMQnRvuoOJX/OQPtW4f29nuPndev0ul7tisdrW', 'observer@oceanverse.com', '李观测员', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 3), (3, 4);

-- 生态系统初始数据
INSERT INTO `ecosystem` (`ecosystem_code`, `ecosystem_name`, `ecosystem_type`, `description`, `area_estimate`, `depth_min`, `depth_max`, `temperature_range`, `threat_factors`, `conservation_status`) VALUES
('ECO001', '珊瑚礁生态系统', 'CORAL', '热带浅海珊瑚礁生态系统，生物多样性极其丰富', 5000.00, 0.50, 30.00, '22-30℃', '海水升温、酸化、过度捕捞', 'VULNERABLE'),
('ECO002', '红树林生态系统', 'COASTAL', '海岸潮间带红树林湿地生态系统', 200.00, 0.00, 5.00, '18-28℃', '围垦开发、水污染', 'ENDANGERED'),
('ECO003', '深海生态系统', 'DEEP_SEA', '深海海洋生态系统', 128000.00, 200.00, 6000.00, '1-4℃', '深海采矿、气候变化', 'STABLE');

-- 测试物种数据
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `iucn_status`, `family`, `genus`, `description`, `data_quality`) VALUES
('SP001', 'Chelonia mydas', 'Green Sea Turtle', '绿海龟', 'EN', 'Cheloniidae', 'Chelonia', '绿海龟是海龟科中体型较大的一种，广泛分布于热带和亚热带海域。', 'VERIFIED'),
('SP002', 'Neophocaena phocaenoides', 'Finless Porpoise', '江豚', 'VU', 'Phocoenidae', 'Neophocaena', '长江江豚是中国特有的淡水鲸类动物，被称为"水中大熊猫"。', 'VERIFIED'),
('SP003', 'Hippocampus kuda', 'Yellow Seahorse', '海马', 'VU', 'Syngnathidae', 'Hippocampus', '海马是一种小型海洋鱼类，以其独特的马头形状而闻名。', 'VERIFIED');

-- =====================================================
-- 权限种子数据
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
-- 超级管理员分配全部权限
-- =====================================================
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
