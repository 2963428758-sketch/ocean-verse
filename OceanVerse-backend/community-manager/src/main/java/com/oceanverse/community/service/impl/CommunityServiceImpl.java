package com.oceanverse.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.utils.JwtUtil;
import com.oceanverse.community.mapper.*;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityLikeMapper likeMapper;
    private final CommunityFavoriteMapper favoriteMapper;

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
        post.setStatus(1);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.insert(post);
        log.info("用户 {} 发布动态: {}", userId, post.getId());
    }

    @Override
    public Object listPosts(PostQueryDTO query) {
        Page<CommunityPost> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, 1);

        if (query.getUserId() != null) {
            wrapper.eq(CommunityPost::getUserId, query.getUserId());
        }
        if (query.getPostType() != null) {
            wrapper.eq(CommunityPost::getPostType, query.getPostType());
        }
        if ("HOT".equals(query.getOrderBy())) {
            wrapper.orderByDesc(CommunityPost::getLikeCount);
        } else {
            wrapper.orderByDesc(CommunityPost::getCreateTime);
        }
        return postMapper.selectPage(page, wrapper);
    }

    @Override
    public Object getPostDetail(Long id) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw BusinessException.notFound("动态");
        }
        return post;
    }

    @Override
    public void deletePost(Long id, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        CommunityPost post = postMapper.selectById(id);
        if (post == null || !post.getUserId().equals(userId)) {
            throw BusinessException.forbidden();
        }
        postMapper.deleteById(id);
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

        // 更新帖子评论数
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, dto.getPostId())
                .setSql("comment_count = comment_count + 1"));
    }

    @Override
    public Object listComments(Long postId, Integer page, Integer size) {
        return commentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CommunityComment>()
                        .eq(CommunityComment::getPostId, postId)
                        .eq(CommunityComment::getStatus, 1)
                        .orderByAsc(CommunityComment::getCreateTime)
        );
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
            return "取消点赞";
        } else {
            CommunityLike like = new CommunityLike();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            like.setCreateTime(LocalDateTime.now());
            likeMapper.insert(like);
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
            return "取消收藏";
        } else {
            CommunityFavorite fav = new CommunityFavorite();
            fav.setUserId(userId);
            fav.setTargetId(targetId);
            fav.setTargetType(targetType);
            fav.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(fav);
            return "收藏成功";
        }
    }

    @Override
    public Object listFavorites(String targetType, Integer page, Integer size, String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        return favoriteMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CommunityFavorite>()
                        .eq(CommunityFavorite::getUserId, userId)
                        .eq(CommunityFavorite::getTargetType, targetType)
                        .orderByDesc(CommunityFavorite::getCreateTime)
        );
    }

    @Override
    public Object getLeaderboard(String type) {
        // TODO: 从 Redis 读取排行榜数据
        return "排行榜功能待实现";
    }
}
