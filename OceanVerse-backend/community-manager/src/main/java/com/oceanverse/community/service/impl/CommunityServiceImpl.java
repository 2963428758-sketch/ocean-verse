package com.oceanverse.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.auth.context.UserContext;
import com.oceanverse.auth.mapper.UserMapper;
import com.oceanverse.common.constants.CommonConstants;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.common.utils.OssUtil;
import com.oceanverse.common.utils.RedisUtil;
import com.oceanverse.community.mapper.CommunityCommentMapper;
import com.oceanverse.community.mapper.CommunityFavoriteMapper;
import com.oceanverse.community.mapper.CommunityFollowMapper;
import com.oceanverse.community.mapper.CommunityLikeMapper;
import com.oceanverse.community.mapper.CommunityNotificationMapper;
import com.oceanverse.community.mapper.CommunityPostMapper;
import com.oceanverse.community.service.CommunityService;
import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;
import com.oceanverse.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityLikeMapper likeMapper;
    private final CommunityFavoriteMapper favoriteMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityNotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final OssUtil ossUtil;

    @Override
    @Transactional
    public void createPost(PostCreateDTO dto, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setContent(dto.getContent());
        post.setPostType(dto.getPostType() != null ? dto.getPostType() : "NORMAL");
        post.setRelatedSpeciesId(dto.getRelatedSpeciesId());
        post.setRelatedObservationId(dto.getRelatedObservationId());
        post.setImageUrls(dto.getImageUrls());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setStatus(3); // 待审核
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.insert(post);
        log.info("用户 {} 发布动态: {}", userId, post.getId());
    }

    @Override
    @Transactional
    public void updatePost(Long id, PostCreateDTO dto, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw BusinessException.notFound("动态");
        }
        // 只有帖子作者可以编辑
        if (!post.getUserId().equals(userId)) {
            throw BusinessException.forbidden();
        }
        // 只有审核未通过(status=4 或 status=0)的帖子可以重新编辑提交
        if (post.getStatus() != 4 && post.getStatus() != 0) {
            throw BusinessException.fail("只能编辑审核未通过的帖子");
        }
        post.setContent(dto.getContent());
        post.setPostType(dto.getPostType() != null ? dto.getPostType() : "NORMAL");
        post.setRelatedSpeciesId(dto.getRelatedSpeciesId());
        post.setRelatedObservationId(dto.getRelatedObservationId());
        post.setImageUrls(dto.getImageUrls());
        post.setStatus(3); // 重新提交审核
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
        log.info("用户 {} 重新提交动态: {}", userId, post.getId());
    }

    @Override
    @Transactional
    public void deletePost(Long id, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw BusinessException.forbidden();
        }
        if (!post.getUserId().equals(userId) && !UserContext.hasAnyRole("SUPER_ADMIN", "ADMIN")) {
            throw BusinessException.forbidden();
        }
        postMapper.deleteById(id);
        commentMapper.delete(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, id));
        likeMapper.delete(new LambdaQueryWrapper<CommunityLike>()
                .eq(CommunityLike::getTargetId, id)
                .eq(CommunityLike::getTargetType, "POST"));
        favoriteMapper.delete(new LambdaQueryWrapper<CommunityFavorite>()
                .eq(CommunityFavorite::getTargetId, id)
                .eq(CommunityFavorite::getTargetType, "POST"));
    }

    @Override
    public Object listPendingPosts(Integer page, Integer size) {
        Page<CommunityPost> pageParam = new Page<>(page, size);
        Page<CommunityPost> result = postMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getStatus, 3)
                        .orderByDesc(CommunityPost::getCreateTime));
        fillUsernames(result.getRecords());
        fillRealCommentCounts(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public void approvePost(Long id) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw BusinessException.notFound("动态");
        }
        post.setStatus(1);
        postMapper.updateById(post);
        // 通知发布者
        sendNotification(post.getUserId(), "SYSTEM", "帖子审核通过",
                "你的动态已通过审核，已发布到广场", post.getId(), post.getId(), null);
    }

    @Override
    @Transactional
    public void rejectPost(Long id) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw BusinessException.notFound("动态");
        }
        post.setStatus(4); // 审核未通过
        postMapper.updateById(post);
        // 通知发布者
        sendNotification(post.getUserId(), "SYSTEM", "帖子审核未通过",
                "你的动态未通过审核，可前往个人主页重新编辑提交", post.getId(), post.getId(), null);
    }

    @Override
    public Object listPosts(PostQueryDTO query, String token) {
        Long currentUserId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                currentUserId = JwtUtil.getUserId(token.replace("Bearer ", ""));
            } catch (Exception ignored) {}
        }

        Page<CommunityPost> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>();

        if (Boolean.TRUE.equals(query.getMyPending())) {
            // 查看自己的待审核帖子
            if (currentUserId == null) {
                throw BusinessException.fail("请先登录");
            }
            wrapper.eq(CommunityPost::getUserId, currentUserId)
                    .eq(CommunityPost::getStatus, 3);
        } else if (query.getUserId() != null) {
            // 指定用户帖子
            wrapper.eq(CommunityPost::getUserId, query.getUserId());
            // 如果是查看自己的帖子，显示所有状态
            if (currentUserId != null && query.getUserId().equals(currentUserId)) {
                wrapper.in(CommunityPost::getStatus, 0, 1, 3, 4);
            } else {
                wrapper.eq(CommunityPost::getStatus, 1);
            }
        } else {
            // 正常广场只显示已审核的帖子
            wrapper.eq(CommunityPost::getStatus, 1);
        }
        if (query.getPostType() != null) {
            wrapper.eq(CommunityPost::getPostType, query.getPostType());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(CommunityPost::getContent, query.getKeyword().trim());
        }
        if ("HOT".equals(query.getOrderBy())) {
            wrapper.orderByDesc(CommunityPost::getLikeCount);
        } else {
            wrapper.orderByDesc(CommunityPost::getCreateTime);
        }
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        fillUsernames(result.getRecords());
        fillLikeFavoriteStatus(result.getRecords(), currentUserId);
        fillRealCommentCounts(result.getRecords());
        return result;
    }

    @Override
    public Object getPostDetail(Long id, String token) {
        Long currentUserId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                currentUserId = JwtUtil.getUserId(token.replace("Bearer ", ""));
            } catch (Exception ignored) {}
        }

        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw BusinessException.notFound("动态");
        }
        fillPostUsername(post);
        post.setCommentCount(getRealCommentCount(id));
        if (currentUserId != null) {
            post.setIsLiked(hasLiked(currentUserId, id, "POST"));
            post.setIsFavorited(hasFavorited(currentUserId, id, "POST"));
        } else {
            post.setIsLiked(false);
            post.setIsFavorited(false);
        }
        return post;
    }

    @Override
    @Transactional
    public void createComment(CommentCreateDTO dto, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityComment comment = new CommunityComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, dto.getPostId())
                .setSql("comment_count = comment_count + 1"));

        CommunityPost post = postMapper.selectById(dto.getPostId());
        if (post != null && !post.getUserId().equals(userId)) {
            User actor = userMapper.selectById(userId);
            String actorName = actor != null ? actor.getUsername() : "有人";
            sendNotification(post.getUserId(), "COMMENT", "新评论",
                    actorName + " 评论了你的帖子", comment.getId(), dto.getPostId(), userId);
        }
        if (dto.getParentId() != null) {
            CommunityComment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)
                    && (post == null || !parentComment.getUserId().equals(post.getUserId()))) {
                User actor = userMapper.selectById(userId);
                String actorName = actor != null ? actor.getUsername() : "有人";
                sendNotification(parentComment.getUserId(), "COMMENT", "新回复",
                        actorName + " 回复了你的评论", comment.getId(), dto.getPostId(), userId);
            }
        }
    }

    @Override
    public Object listComments(Long postId, Integer page, Integer size) {
        Page<CommunityComment> result = commentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CommunityComment>()
                        .eq(CommunityComment::getPostId, postId)
                        .eq(CommunityComment::getStatus, 1)
                        .orderByAsc(CommunityComment::getCreateTime)
        );
        fillCommentUsernames(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public Object toggleLike(String targetType, Long targetId, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityLike existing = likeMapper.selectOne(
                new LambdaQueryWrapper<CommunityLike>()
                        .eq(CommunityLike::getUserId, userId)
                        .eq(CommunityLike::getTargetId, targetId)
                        .eq(CommunityLike::getTargetType, targetType)
        );
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            if ("POST".equals(targetType)) {
                postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                        .eq(CommunityPost::getId, targetId)
                        .setSql("like_count = GREATEST(like_count - 1, 0)"));
            }
            return "取消点赞";
        } else {
            CommunityLike like = new CommunityLike();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            like.setCreateTime(LocalDateTime.now());
            likeMapper.insert(like);
            if ("POST".equals(targetType)) {
                postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                        .eq(CommunityPost::getId, targetId)
                        .setSql("like_count = like_count + 1"));

                CommunityPost post = postMapper.selectById(targetId);
                if (post != null && !post.getUserId().equals(userId)) {
                    User actor = userMapper.selectById(userId);
                    String actorName = actor != null ? actor.getUsername() : "有人";
                    sendNotification(post.getUserId(), "LIKE", "点赞通知",
                            actorName + " 赞了你的帖子", null, targetId, userId);
                }
            }
            return "点赞成功";
        }
    }

    @Override
    @Transactional
    public Object toggleFavorite(String targetType, Long targetId, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<CommunityFavorite>()
                        .eq(CommunityFavorite::getUserId, userId)
                        .eq(CommunityFavorite::getTargetId, targetId)
                        .eq(CommunityFavorite::getTargetType, targetType)
        );
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            if ("POST".equals(targetType)) {
                postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                        .eq(CommunityPost::getId, targetId)
                        .setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
            }
            return "取消收藏";
        } else {
            CommunityFavorite fav = new CommunityFavorite();
            fav.setUserId(userId);
            fav.setTargetId(targetId);
            fav.setTargetType(targetType);
            fav.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(fav);
            if ("POST".equals(targetType)) {
                postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                        .eq(CommunityPost::getId, targetId)
                        .setSql("favorite_count = favorite_count + 1"));
            }
            return "收藏成功";
        }
    }

    @Override
    public Object listFavorites(String targetType, Integer page, Integer size, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        Page<CommunityFavorite> favoritePage = favoriteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CommunityFavorite>()
                        .eq(CommunityFavorite::getUserId, userId)
                        .eq(CommunityFavorite::getTargetType, targetType)
                        .orderByDesc(CommunityFavorite::getCreateTime)
        );
        List<Long> postIds = favoritePage.getRecords().stream()
                .map(CommunityFavorite::getTargetId).collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return favoritePage.convert(f -> null);
        }
        List<CommunityPost> posts = postMapper.selectBatchIds(postIds);
        fillUsernames(posts);
        Map<Long, CommunityPost> postMap = posts.stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p, (a, b) -> a));
        return favoritePage.convert(f -> postMap.get(f.getTargetId()));
    }

    @Override
    public Object listLikedPosts(Integer page, Integer size, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        Page<CommunityLike> likePage = likeMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CommunityLike>()
                        .eq(CommunityLike::getUserId, userId)
                        .eq(CommunityLike::getTargetType, "POST")
                        .orderByDesc(CommunityLike::getCreateTime)
        );
        List<Long> postIds = likePage.getRecords().stream()
                .map(CommunityLike::getTargetId).collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return likePage.convert(l -> null);
        }
        List<CommunityPost> posts = postMapper.selectBatchIds(postIds);
        fillUsernames(posts);
        Map<Long, CommunityPost> postMap = posts.stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p, (a, b) -> a));
        return likePage.convert(l -> postMap.get(l.getTargetId()));
    }

    @Override
    public Object getLeaderboard(String type) {
        // TODO: 从 Redis 读取排行榜数据
        return "排行榜功能待实现";
    }

    @Override
    @Transactional
    public Object toggleFollow(Long followUserId, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        if (userId.equals(followUserId)) {
            throw BusinessException.fail("不能关注自己");
        }
        CommunityFollow existing = followMapper.selectOne(
                new LambdaQueryWrapper<CommunityFollow>()
                        .eq(CommunityFollow::getUserId, userId)
                        .eq(CommunityFollow::getFollowUserId, followUserId)
        );
        if (existing != null) {
            followMapper.deleteById(existing.getId());
            return "已取消关注";
        } else {
            CommunityFollow follow = new CommunityFollow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);

            User actor = userMapper.selectById(userId);
            User target = userMapper.selectById(followUserId);
            if (target != null) {
                SysNotification notification = new SysNotification();
                notification.setUserId(followUserId);
                notification.setTitle("新粉丝");
                notification.setContent(actor != null ? actor.getUsername() + " 关注了你" : "有人关注了你");
                notification.setType("FOLLOW");
                notification.setIsRead(0);
                notification.setRelatedId(userId);
                notification.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(notification);

                publishNotification("FOLLOW", followUserId, "新粉丝",
                        notification.getContent(), notification.getId());
            }
            return "关注成功";
        }
    }

    @Override
    public Object getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        long postCount = postMapper.selectCount(
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getUserId, userId)
                        .eq(CommunityPost::getStatus, 1));
        long followerCount = followMapper.selectCount(
                new LambdaQueryWrapper<CommunityFollow>()
                        .eq(CommunityFollow::getFollowUserId, userId));
        long followCount = followMapper.selectCount(
                new LambdaQueryWrapper<CommunityFollow>()
                        .eq(CommunityFollow::getUserId, userId));

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("backgroundUrl", user.getBackgroundUrl());
        profile.put("bio", user.getBio());
        profile.put("postCount", postCount);
        profile.put("followerCount", followerCount);
        profile.put("followCount", followCount);
        return profile;
    }

    @Override
    public Object listNotifications(Integer page, Integer size, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        return notificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .orderByDesc(SysNotification::getCreateTime)
        );
    }

    @Override
    public Object getUnreadCount(String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
        return count;
    }

    @Override
    public void markNotificationRead(Long notificationId, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        SysNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw BusinessException.forbidden();
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    @Override
    public void markAllRead(String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        notificationMapper.update(null,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0)
                        .set(SysNotification::getIsRead, 1));
    }

    @Override
    public void deleteNotification(Long id, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        SysNotification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw BusinessException.forbidden();
        }
        notificationMapper.deleteById(id);
    }

    @Override
    public void deleteAllReadNotifications(String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        notificationMapper.delete(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 1));
    }

    @Override
    public String uploadAvatar(org.springframework.web.multipart.MultipartFile file, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        String url = ossUtil.upload(file);
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getAvatarUrl, url));
        return url;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.forbidden();
        }
        if (!comment.getUserId().equals(userId) && !UserContext.hasAnyRole("SUPER_ADMIN", "ADMIN")) {
            throw BusinessException.forbidden();
        }
        commentMapper.deleteById(commentId);
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, comment.getPostId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
    }

    @Override
    @Transactional
    public void updateProfile(String nickname, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateBio(String bio, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        user.setBio(bio);
        userMapper.updateById(user);
    }

    @Override
    public String uploadBackground(org.springframework.web.multipart.MultipartFile file, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        String url = ossUtil.upload(file);
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getBackgroundUrl, url));
        return url;
    }

    @Override
    public Object getFollowingList(Long userId, String token) {
        if (userId == null) {
            userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        }
        List<CommunityFollow> follows = followMapper.selectList(
                new LambdaQueryWrapper<CommunityFollow>()
                        .eq(CommunityFollow::getUserId, userId)
                        .orderByDesc(CommunityFollow::getCreateTime));
        if (follows.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Long> targetIds = follows.stream()
                .map(CommunityFollow::getFollowUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(targetIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return targetIds.stream().map(id -> {
            User u = userMap.get(id);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", id);
            item.put("username", u != null ? u.getUsername() : "用户");
            item.put("avatarUrl", u != null ? u.getAvatarUrl() : null);
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Object getFollowerList(Long userId, String token) {
        if (userId == null) {
            userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        }
        List<CommunityFollow> followers = followMapper.selectList(
                new LambdaQueryWrapper<CommunityFollow>()
                        .eq(CommunityFollow::getFollowUserId, userId)
                        .orderByDesc(CommunityFollow::getCreateTime));
        if (followers.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Long> userIds = followers.stream()
                .map(CommunityFollow::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return userIds.stream().map(id -> {
            User u = userMap.get(id);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", id);
            item.put("username", u != null ? u.getUsername() : "用户");
            item.put("avatarUrl", u != null ? u.getAvatarUrl() : null);
            return item;
        }).collect(Collectors.toList());
    }

    private void fillPostUsername(CommunityPost post) {
        User user = userMapper.selectById(post.getUserId());
        post.setUsername(user != null ? user.getUsername() : "用户");
    }

    private boolean hasLiked(Long userId, Long targetId, String targetType) {
        return likeMapper.selectCount(new LambdaQueryWrapper<CommunityLike>()
                .eq(CommunityLike::getUserId, userId)
                .eq(CommunityLike::getTargetId, targetId)
                .eq(CommunityLike::getTargetType, targetType)) > 0;
    }

    private boolean hasFavorited(Long userId, Long targetId, String targetType) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<CommunityFavorite>()
                .eq(CommunityFavorite::getUserId, userId)
                .eq(CommunityFavorite::getTargetId, targetId)
                .eq(CommunityFavorite::getTargetType, targetType)) > 0;
    }

    private void fillLikeFavoriteStatus(List<CommunityPost> posts, Long currentUserId) {
        if (currentUserId == null || posts == null || posts.isEmpty()) return;
        for (CommunityPost post : posts) {
            post.setIsLiked(hasLiked(currentUserId, post.getId(), "POST"));
            post.setIsFavorited(hasFavorited(currentUserId, post.getId(), "POST"));
        }
    }

    private int getRealCommentCount(Long postId) {
        return Math.toIntExact(commentMapper.selectCount(
                new LambdaQueryWrapper<CommunityComment>()
                        .eq(CommunityComment::getPostId, postId)
                        .eq(CommunityComment::getStatus, 1)));
    }

    private void fillRealCommentCounts(List<CommunityPost> posts) {
        if (posts == null || posts.isEmpty()) return;
        for (CommunityPost post : posts) {
            post.setCommentCount(getRealCommentCount(post.getId()));
        }
    }

    private void fillUsernames(List<CommunityPost> posts) {
        if (posts == null || posts.isEmpty()) return;
        Set<Long> userIds = posts.stream().map(CommunityPost::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        posts.forEach(p -> {
            User u = userMap.get(p.getUserId());
            p.setUsername(u != null ? u.getUsername() : "用户");
            p.setAvatarUrl(u != null ? u.getAvatarUrl() : null);
        });
    }

    private void fillCommentUsernames(List<CommunityComment> comments) {
        if (comments == null || comments.isEmpty()) return;
        Set<Long> userIds = comments.stream().map(CommunityComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        comments.forEach(c -> {
            User u = userMap.get(c.getUserId());
            c.setUsername(u != null ? u.getUsername() : "用户");
            c.setAvatarUrl(u != null ? u.getAvatarUrl() : null);
        });
    }

    private void sendNotification(Long targetUserId, String type, String title, String content, 
                                    Long relatedId, Long targetPostId, Long fromUserId) {
        SysNotification notification = new SysNotification();
        notification.setUserId(targetUserId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setRelatedId(relatedId);
        notification.setTargetPostId(targetPostId);
        notification.setFromUserId(fromUserId);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        publishNotification(type, targetUserId, title, content, notification.getId());
    }

    private void publishNotification(String type, Long userId, String title, String content, Long notificationId) {
        try {
            Map<String, String> fields = new HashMap<>();
            fields.put("type", type);
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notificationId);
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("createTime", LocalDateTime.now().toString());
            fields.put("payload", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
            redisUtil.xAdd(CommonConstants.STREAM_NOTIFICATION, fields, 10000);
        } catch (Exception e) {
            log.error("发布通知消息失败: userId={}, type={}", userId, type, e);
        }
    }
}
