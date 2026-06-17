-- ============================================================
-- 观测记录模块测试数据
-- 依赖：先执行 migration-observation-deleted.sql
-- ============================================================

-- ==================== 观测地点 ====================
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

-- ==================== 观测记录 ====================
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
