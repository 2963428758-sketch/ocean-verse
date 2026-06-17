-- =====================================================
-- OceanVerse 物种分布数据初始化
-- 为所有18个物种添加初始经纬度分布记录
-- 坐标均为该物种在自然界中的典型分布区域
-- =====================================================

-- 先清理可能已存在的记录，避免重复
DELETE FROM `species_distribution` WHERE `species_id` IN (
  SELECT id FROM `species` WHERE `deleted` = 0
);

-- SP001 绿海龟 — 南海西沙群岛海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP001' AND deleted = 0 LIMIT 1), 'NATIVE', '南海西沙群岛', '中国', '海南', 16.831900, 112.332800, 0.00, 40.00, '珊瑚礁', 1, 'COMMON', NOW(), NOW());

-- SP002 江豚 — 长江中下游
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP002' AND deleted = 0 LIMIT 1), 'NATIVE', '长江中下游', '中国', '湖北', 30.294000, 114.254000, 0.00, 10.00, '淡水河流', 1, 'COMMON', NOW(), NOW());

-- SP003 海马 — 广东沿海
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP003' AND deleted = 0 LIMIT 1), 'NATIVE', '南海北部沿海', '中国', '广东', 21.508000, 115.368000, 1.00, 30.00, '海草床', 1, 'COMMON', NOW(), NOW());

-- SP004 大白鲨 — 南非甘斯巴海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP004' AND deleted = 0 LIMIT 1), 'NATIVE', '甘斯巴海域', '南非', '西开普省', -34.618000, 19.349000, 0.00, 250.00, '远洋', 1, 'COMMON', NOW(), NOW());

-- SP005 鲸鲨 — 菲律宾董索海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP005' AND deleted = 0 LIMIT 1), 'NATIVE', '董索海域', '菲律宾', '南吕宋', 12.966000, 123.246000, 0.00, 100.00, '远洋', 1, 'COMMON', NOW(), NOW());

-- SP006 中华白海豚 — 珠江口
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP006' AND deleted = 0 LIMIT 1), 'NATIVE', '珠江口中华白海豚保护区', '中国', '广东', 22.120000, 113.780000, 0.00, 20.00, '浅海湾', 1, 'RARE', NOW(), NOW());

-- SP007 蓝鲸 — 南极海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP007' AND deleted = 0 LIMIT 1), 'NATIVE', '南极半岛海域', '南极洲', NULL, -64.812000, -63.512000, 0.00, 500.00, '远洋', 1, 'COMMON', NOW(), NOW());

-- SP008 虎鲸 — 挪威海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP008' AND deleted = 0 LIMIT 1), 'NATIVE', '罗弗敦群岛海域', '挪威', '诺尔兰', 68.209400, 14.563000, 0.00, 300.00, '寒冷海域', 1, 'COMMON', NOW(), NOW());

-- SP009 蝠鲼 — 印尼科莫多海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP009' AND deleted = 0 LIMIT 1), 'NATIVE', '科莫多国家公园', '印度尼西亚', '东努沙登加拉', -8.543000, 119.488000, 0.00, 120.00, '珊瑚礁', 1, 'COMMON', NOW(), NOW());

-- SP010 小丑鱼 — 大堡礁
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP010' AND deleted = 0 LIMIT 1), 'NATIVE', '大堡礁', '澳大利亚', '昆士兰', -16.482000, 145.462000, 1.00, 15.00, '珊瑚礁', 1, 'COMMON', NOW(), NOW());

-- SP011 蓝环章鱼 — 澳洲东部沿海
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP011' AND deleted = 0 LIMIT 1), 'NATIVE', '悉尼沿海', '澳大利亚', '新南威尔士', -33.856000, 151.262000, 0.00, 20.00, '岩石潮间带', 1, 'COMMON', NOW(), NOW());

-- SP012 儒艮 — 澳洲鲨鱼湾
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP012' AND deleted = 0 LIMIT 1), 'NATIVE', '鲨鱼湾', '澳大利亚', '西澳大利亚', -25.902000, 113.682000, 0.00, 10.00, '海草床', 1, 'COMMON', NOW(), NOW());

-- SP013 翻车鱼 — 加利福尼亚沿海
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP013' AND deleted = 0 LIMIT 1), 'NATIVE', '蒙特雷湾', '美国', '加利福尼亚', 36.802000, -121.945000, 0.00, 200.00, '远洋', 1, 'COMMON', NOW(), NOW());

-- SP014 锤头鲨 — 加拉帕戈斯群岛
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP014' AND deleted = 0 LIMIT 1), 'NATIVE', '加拉帕戈斯群岛', '厄瓜多尔', NULL, -0.953000, -90.966000, 0.00, 300.00, '远洋', 1, 'COMMON', NOW(), NOW());

-- SP015 海月水母 — 日本濑户内海
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP015' AND deleted = 0 LIMIT 1), 'NATIVE', '濑户内海', '日本', '的冈', 34.573000, 133.768000, 0.00, 20.00, '内海', 1, 'COMMON', NOW(), NOW());

-- SP016 玳瑁 — 加勒比海
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP016' AND deleted = 0 LIMIT 1), 'NATIVE', '加勒比海珊瑚礁', '古巴', NULL, 21.521000, -82.286000, 0.00, 30.00, '珊瑚礁', 1, 'RARE', NOW(), NOW());

-- SP017 狮子鱼 — 印尼巴厘岛海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP017' AND deleted = 0 LIMIT 1), 'NATIVE', '图兰奔海域', '印度尼西亚', '巴厘', -8.342000, 115.672000, 1.00, 40.00, '珊瑚礁', 1, 'COMMON', NOW(), NOW());

-- SP018 鹦嘴鱼 — 马尔代夫海域
INSERT INTO `species_distribution` (`species_id`, `distribution_type`, `region_name`, `country`, `province`, `latitude`, `longitude`, `depth_min`, `depth_max`, `habitat_type`, `is_primary`, `distribution_status`, `create_time`, `update_time`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP018' AND deleted = 0 LIMIT 1), 'NATIVE', '北马累环礁', '马尔代夫', NULL, 4.374000, 73.532000, 1.00, 25.00, '珊瑚礁', 1, 'COMMON', NOW(), NOW());
