-- =====================================================
-- OceanVerse 物种扩展测试数据
-- 新增13个海洋物种 (SP006-SP018) 及对应图片媒体记录
-- 图片存放在 OceanVerse-frontend/public/test-images/
-- =====================================================

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
-- 物种媒体数据（对应新增物种 SP006-SP018）
-- 使用 species_code 子查询动态匹配 species_id，避免ID间隙问题
-- =====================================================

-- 先清理之前可能插入的错误媒体记录
DELETE FROM `species_media` WHERE `file_name` IN (
  'humpback_dolphin_1.png', 'humpback_dolphin_2.png',
  'blue_whale_1.png', 'blue_whale_2.png',
  'orca_1.png', 'orca_2.png',
  'manta_ray_1.png', 'manta_ray_2.png',
  'clownfish_1.png', 'clownfish_2.png',
  'blue_ringed_octopus_1.png', 'blue_ringed_octopus_2.png',
  'dugong_1.png', 'dugong_2.png',
  'ocean_sunfish_1.png', 'ocean_sunfish_2.png',
  'hammerhead_shark_1.png', 'hammerhead_shark_2.png',
  'moon_jellyfish_1.png', 'moon_jellyfish_2.png',
  'hawksbill_turtle_1.png', 'hawksbill_turtle_2.png',
  'lionfish_1.png', 'lionfish_2.png',
  'parrotfish_1.png', 'parrotfish_2.png'
);

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
