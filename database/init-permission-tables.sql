-- SUPER_ADMIN 绑定全部权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0), p.id
FROM sys_permission p;

-- 用户 ID=1 分配 SUPER_ADMIN（如果不是1，改成你的用户ID）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 1, id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0;