package com.oceanverse.community.controller;

import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.annotation.RequireRole;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;
import com.oceanverse.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<Object> listPosts(PostQueryDTO query,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        return Result.success(communityService.listPosts(query, token));
    }

    @GetMapping("/post/{id}")
    public Result<Object> getPost(@PathVariable Long id,
                                  @RequestHeader(value = "Authorization", required = false) String token) {
        return Result.success(communityService.getPostDetail(id, token));
    }

    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@PathVariable Long id,
                                    @RequestHeader("Authorization") String token) {
        communityService.deletePost(id, token);
        return Result.success();
    }

    // ==================== 帖子审核 ====================

    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("community:post:audit")
    @GetMapping("/post/pending")
    public Result<Object> listPendingPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(communityService.listPendingPosts(page, size));
    }

    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("community:post:audit")
    @PutMapping("/post/{id}/approve")
    public Result<Void> approvePost(@PathVariable Long id) {
        communityService.approvePost(id);
        return Result.success();
    }

    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("community:post:audit")
    @PutMapping("/post/{id}/reject")
    public Result<Void> rejectPost(@PathVariable Long id) {
        communityService.rejectPost(id);
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
    public Result<Object> listComments(@PathVariable Long postId,
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
    public Result<Object> toggleLike(@PathVariable String targetType,
                                      @PathVariable Long targetId,
                                      @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleLike(targetType, targetId, token));
    }

    // ==================== 收藏 ====================

    @PostMapping("/favorite/{targetType}/{targetId}")
    public Result<Object> toggleFavorite(@PathVariable String targetType,
                                          @PathVariable Long targetId,
                                          @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleFavorite(targetType, targetId, token));
    }

    @GetMapping("/favorite/list")
    public Result<Object> listFavorites(@RequestParam String targetType,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listFavorites(targetType, page, size, token));
    }

    @GetMapping("/like/list")
    public Result<Object> listLikedPosts(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listLikedPosts(page, size, token));
    }

    // ==================== 关注 ====================

    @PostMapping("/follow/{userId}")
    public Result<Object> toggleFollow(@PathVariable Long userId,
                                        @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleFollow(userId, token));
    }

    @GetMapping("/user/{userId}")
    public Result<Object> getUserProfile(@PathVariable Long userId) {
        return Result.success(communityService.getUserProfile(userId));
    }

    // ==================== 通知 ====================

    @GetMapping("/notification/list")
    public Result<Object> listNotifications(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listNotifications(page, size, token));
    }

    @GetMapping("/notification/unread")
    public Result<Object> getUnreadCount(@RequestHeader("Authorization") String token) {
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

    @DeleteMapping("/notification/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id,
                                            @RequestHeader("Authorization") String token) {
        communityService.deleteNotification(id, token);
        return Result.success();
    }

    @DeleteMapping("/notification/read")
    public Result<Void> deleteAllReadNotifications(@RequestHeader("Authorization") String token) {
        communityService.deleteAllReadNotifications(token);
        return Result.success();
    }

    // ==================== 头像 ====================

    @PutMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                        @RequestHeader("Authorization") String token) {
        return Result.success("头像更新成功", communityService.uploadAvatar(file, token));
    }

    // ==================== 个人资料 ====================

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestParam String nickname,
                                       @RequestHeader("Authorization") String token) {
        communityService.updateProfile(nickname, token);
        return Result.success();
    }

    @PutMapping("/bio")
    public Result<Void> updateBio(@RequestParam String bio,
                                   @RequestHeader("Authorization") String token) {
        communityService.updateBio(bio, token);
        return Result.success();
    }

    @PutMapping("/background")
    public Result<String> uploadBackground(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                            @RequestHeader("Authorization") String token) {
        return Result.success("背景更新成功", communityService.uploadBackground(file, token));
    }

    @GetMapping("/following")
    public Result<Object> getFollowingList(@RequestHeader("Authorization") String token) {
        return Result.success(communityService.getFollowingList(token));
    }

    @GetMapping("/follower")
    public Result<Object> getFollowerList(@RequestHeader("Authorization") String token) {
        return Result.success(communityService.getFollowerList(token));
    }

    // ==================== 排行榜 ====================

    @GetMapping("/leaderboard")
    public Result<Object> getLeaderboard(@RequestParam(defaultValue = "points") String type) {
        return Result.success(communityService.getLeaderboard(type));
    }
}
