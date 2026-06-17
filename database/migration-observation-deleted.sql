-- ============================================================
-- 观测记录模块数据库迁移
-- 修复 @TableLogic 约定：deleted 列改为 BIGINT，唯一索引改为联合唯一
-- ============================================================

-- 1. observation 表
ALTER TABLE `observation` DROP INDEX `uk_observation_code`;
ALTER TABLE `observation` MODIFY COLUMN `deleted` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `observation` ADD UNIQUE KEY `uk_observation_code` (`observation_code`, `deleted`);

-- 2. ecosystem 表
ALTER TABLE `ecosystem` DROP INDEX `uk_ecosystem_code`;
ALTER TABLE `ecosystem` MODIFY COLUMN `deleted` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `ecosystem` ADD UNIQUE KEY `uk_ecosystem_code` (`ecosystem_code`, `deleted`);

-- 3. observation_location 表
ALTER TABLE `observation_location` DROP INDEX `uk_location_code`;
ALTER TABLE `observation_location` MODIFY COLUMN `deleted` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `observation_location` ADD UNIQUE KEY `uk_location_code` (`location_code`, `deleted`);
