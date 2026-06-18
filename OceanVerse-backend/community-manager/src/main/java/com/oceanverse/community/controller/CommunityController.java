package com.oceanverse.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;
import com.oceanverse.pojo.entity.CommunityComment;
import com.oceanverse.pojo.entity.CommunityPost;
import com.oceanverse.pojo.entity.SysNotification;
import com.oceanverse.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社区互动接口 — 成员D
 * 包含：动态发布、评论、点赞、收藏、关注、通知
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class    CommunityController {

    private final CommunityService communityService;

    // ==================== 动态 ====================

    @PostMapping("/post")
    public Result<Void> createPost(@Valid @RequestBody PostCreateDTO dto,
                                    @RequestHeader("Authorization") String token) {
        communityService.createPost(dto, token);
        return Result.success();
    }

    @GetMapping("/post/list")
    public Result<Page<CommunityPost>> listPosts(PostQueryDTO query) {
        return Result.success(communityService.listPosts(query));
    }

    @GetMapping("/post/{id}")
    public Result<CommunityPost> getPost(@PathVariable Long id) {
        return Result.success(communityService.getPostDetail(id));
    }

    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@PathVariable Long id,
                                    @RequestHeader("Authorization") String token) {
        communityService.deletePost(id, token);
        return Result.success();
    }

    // ==================== 评论 ====================

    @PostMapping("/comment")
    public Result<Void> createComment(@Valid @RequestBody CommentCreateDTO dto,
                                       @RequestHeader("Authorization") String token) {
        communityService.createComment(dto, token);
        return Result.success();
    }

    @GetMapping("/comment/list/{postId}")
    public Result<Page<CommunityComment>> listComments(@PathVariable Long postId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(communityService.listComments(postId, page, size));
    }

    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        communityService.deleteComment(id, token);
        return Result.success();
    }

    // ==================== 点赞 ====================

    @PostMapping("/like/{targetType}/{targetId}")
    public Result<String> toggleLike(@PathVariable String targetType,
                                      @PathVariable Long targetId,
                                      @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleLike(targetType, targetId, token));
    }

    // ==================== 收藏 ====================

    @PostMapping("/favorite/{targetType}/{targetId}")
    public Result<String> toggleFavorite(@PathVariable String targetType,
                                          @PathVariable Long targetId,
                                          @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleFavorite(targetType, targetId, token));
    }

    @GetMapping("/favorite/list")
    public Result<Page<CommunityPost>> listFavorites(@RequestParam String targetType,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listFavorites(targetType, page, size, token));
    }

    @GetMapping("/like/list")
    public Result<Page<CommunityPost>> listLikedPosts(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listLikedPosts(page, size, token));
    }

    // ==================== 关注 ====================

    @PostMapping("/follow/{userId}")
    public Result<String> toggleFollow(@PathVariable Long userId,
                                        @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleFollow(userId, token));
    }

    @GetMapping("/user/{userId}")
    public Result<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        return Result.success(communityService.getUserProfile(userId));
    }

    // ==================== 通知 ====================

    @GetMapping("/notification/list")
    public Result<Page<SysNotification>> listNotifications(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listNotifications(page, size, token));
    }

    @GetMapping("/notification/unread")
    public Result<Long> getUnreadCount(@RequestHeader("Authorization") String token) {
        return Result.success(communityService.getUnreadCount(token));
    }

    @PutMapping("/notification/{id}/read")
    public Result<Void> markNotificationRead(@PathVariable Long id,
                                              @RequestHeader("Authorization") String token) {
        communityService.markNotificationRead(id, token);
        return Result.success();
    }

    @PutMapping("/notification/read-all")
    public Result<Void> markAllRead(@RequestHeader("Authorization") String token) {
        communityService.markAllRead(token);
        return Result.success();
    }

    // ==================== 头像 ====================

    @PutMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                        @RequestHeader("Authorization") String token) {
        return Result.success("头像更新成功", communityService.uploadAvatar(file, token));
    }

    // ==================== 排行榜 ====================

    @GetMapping("/leaderboard")
    public Result<List<Map<String, Object>>> getLeaderboard(@RequestParam(defaultValue = "points") String type) {
        return Result.success(communityService.getLeaderboard(type));
    }
}
