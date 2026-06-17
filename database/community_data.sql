-- =====================================================
-- 社区模块测试数据 (含真实图片)
-- 前置: sys_user 表中 id=1(admin), 2(researcher), 3(observer)
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