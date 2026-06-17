-- =====================================================
-- 物种图片测试数据
-- 使用本地 Vite 开发服务器提供的静态图片
-- 图片存放在 OceanVerse-frontend/public/test-images/
-- =====================================================

-- 新增物种：大白鲨
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP004', 'Carcharodon carcharias', 'Great White Shark', '大白鲨', '2', 'VU', 'Animalia', 'Chordata', 'Chondrichthyes', 'Lamniformes', 'Lamnidae', 'Carcharodon', 'C. carcharias',
'大白鲨是世界上最大的掠食性鱼类之一，广泛分布于全球温带和热带海域。它们是海洋食物链的顶级捕食者，对维持海洋生态平衡起着至关重要的作用。',
'大白鲨体长可达6米，体重可达2吨。身体呈流线型，背部深灰色，腹部白色。拥有300颗锋利的锯齿状牙齿，咬合力极强。',
'大白鲨是独居动物，主要在近海和远洋活动。以海豹、海狮、鱼类和其他鲨鱼为食。具有极强的嗅觉和电感知能力。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- 新增物种：鲸鲨
INSERT INTO `species` (`species_code`, `scientific_name`, `common_name`, `chinese_name`, `protection_level`, `iucn_status`, `kingdom`, `phylum`, `class_name`, `order_name`, `family`, `genus`, `species`, `description`, `morphology`, `ecology`, `is_endemic`, `is_invasive`, `data_quality`, `source`)
VALUES ('SP005', 'Rhincodon typus', 'Whale Shark', '鲸鲨', '2', 'EN', 'Animalia', 'Chordata', 'Chondrichthyes', 'Orectolobiformes', 'Rhincodontidae', 'Rhincodon', 'R. typus',
'鲸鲨是世界上最大的鱼类，体长可达18米。尽管体型庞大，它们却是温和的滤食性动物，以浮游生物和小型鱼类为食。广泛分布于热带和暖温带海域。',
'鲸鲨身体呈深灰色，布满白色斑点和条纹，每只鲸鲨的斑点图案都是独一无二的。头部宽大扁平，嘴巴可达1.5米宽。',
'鲸鲨通常独居或成小群活动，常在表层水域缓慢游动滤食。具有长距离迁徙习性，会跟随浮游生物的季节性分布移动。', 0, 0, 'VERIFIED', 'IUCN Red List');

-- =====================================================
-- 物种媒体数据（对应已有物种和新增物种）
-- 使用 species_code 子查询动态匹配 species_id
-- =====================================================

-- 绿海龟 (SP001) 图片
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP001' AND deleted = 0 LIMIT 1), 'IMAGE', 'green_sea_turtle_1.png', '/test-images/green_sea_turtle_1.png', 1302117, '绿海龟在珊瑚礁海域游泳', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP001' AND deleted = 0 LIMIT 1), 'IMAGE', 'green_sea_turtle_2.png', '/test-images/green_sea_turtle_2.png', 1144183, '绿海龟浮出水面', 0, 1);

-- 江豚 (SP002) 图片
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP002' AND deleted = 0 LIMIT 1), 'IMAGE', 'finless_porpoise_1.png', '/test-images/finless_porpoise_1.png', 995356, '长江江豚在水中游动', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP002' AND deleted = 0 LIMIT 1), 'IMAGE', 'finless_porpoise_2.png', '/test-images/finless_porpoise_2.png', 986628, '两只江豚结伴而行', 0, 1);

-- 海马 (SP003) 图片
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP003' AND deleted = 0 LIMIT 1), 'IMAGE', 'yellow_seahorse_1.png', '/test-images/yellow_seahorse_1.png', 1088956, '海马附着在海草上', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP003' AND deleted = 0 LIMIT 1), 'IMAGE', 'yellow_seahorse_2.png', '/test-images/yellow_seahorse_2.png', 1216051, '一对海马共同游动', 0, 1);

-- 大白鲨 (SP004) 图片
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP004' AND deleted = 0 LIMIT 1), 'IMAGE', 'great_white_shark_1.png', '/test-images/great_white_shark_1.png', 1008527, '大白鲨在深蓝海水中游弋', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP004' AND deleted = 0 LIMIT 1), 'IMAGE', 'great_white_shark_2.png', '/test-images/great_white_shark_2.png', 1053568, '大白鲨展示白色腹部和牙齿', 0, 1);

-- 鲸鲨 (SP005) 图片
INSERT INTO `species_media` (`species_id`, `media_type`, `file_name`, `file_url`, `file_size`, `media_title`, `is_primary`, `status`)
VALUES
((SELECT id FROM species WHERE species_code = 'SP005' AND deleted = 0 LIMIT 1), 'IMAGE', 'whale_shark_1.png', '/test-images/whale_shark_1.png', 1127237, '鲸鲨展示独特的斑点花纹', 1, 1),
((SELECT id FROM species WHERE species_code = 'SP005' AND deleted = 0 LIMIT 1), 'IMAGE', 'whale_shark_2.png', '/test-images/whale_shark_2.png', 1186589, '鲸鲨与潜水员同框展示巨大体型', 0, 1);
