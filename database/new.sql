ALTER TABLE sys_user
    ADD COLUMN background_url VARCHAR(500) DEFAULT NULL COMMENT '背景图URL' AFTER avatar_url,
    ADD COLUMN bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介' AFTER background_url;
ALTER TABLE sys_notification
    ADD COLUMN target_post_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标帖子ID' AFTER related_id,
    ADD COLUMN from_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作者ID' AFTER target_post_id;