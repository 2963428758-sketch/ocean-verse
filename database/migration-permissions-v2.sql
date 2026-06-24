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
