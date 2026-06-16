package com.oceanverse.community.controller;

import com.oceanverse.common.annotation.RequirePermission;
import com.oceanverse.common.result.Result;
import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;
import com.oceanverse.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 社区互动接口 — 成员D
 * <p>
 * 公开接口：listPosts、getPost、listComments、getLeaderboard
 * 登录即可用：createPost、createComment、toggleLike、toggleFavorite、listFavorites
 * 管理接口（需要权限）：deletePost
 * <p>
 * Swagger Tag: Community（社区互动）
 */
@Tag(name = "Community", description = "社区互动接口 — 动态、评论、点赞、收藏")
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // ==================== 动态 ====================

    @Operation(summary = "发布动态", description = "发布社区动态帖子")
    @PostMapping("/post")
    public Result<Void> createPost(@Valid @RequestBody PostCreateDTO dto,
                                    @RequestHeader("Authorization") String token) {
        communityService.createPost(dto, token);
        return Result.success();
    }

    @Operation(summary = "动态列表", description = "分页查询社区动态（公开接口）")
    @GetMapping("/post/list")
    public Result<Object> listPosts(PostQueryDTO query) {
        return Result.success(communityService.listPosts(query));
    }

    @Operation(summary = "动态详情", description = "获取单条动态详情（公开接口）")
    @GetMapping("/post/{id}")
    public Result<Object> getPost(
            @Parameter(description = "动态ID") @PathVariable Long id) {
        return Result.success(communityService.getPostDetail(id));
    }

    @Operation(summary = "删除动态", description = "删除动态帖子（需要 community:delete 权限或作者本人）")
    @DeleteMapping("/post/{id}")
    @RequirePermission("community:delete")
    public Result<Void> deletePost(
            @Parameter(description = "动态ID") @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        communityService.deletePost(id, token);
        return Result.success();
    }

    // ==================== 评论 ====================

    @Operation(summary = "发布评论", description = "对动态发布评论")
    @PostMapping("/comment")
    public Result<Void> createComment(@Valid @RequestBody CommentCreateDTO dto,
                                       @RequestHeader("Authorization") String token) {
        communityService.createComment(dto, token);
        return Result.success();
    }

    @Operation(summary = "评论列表", description = "获取动态下的评论列表（公开接口）")
    @GetMapping("/comment/list/{postId}")
    public Result<Object> listComments(
            @Parameter(description = "动态ID") @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(communityService.listComments(postId, page, size));
    }

    // ==================== 点赞 ====================

    @Operation(summary = "点赞/取消点赞", description = "对动态或评论进行点赞/取消点赞")
    @PostMapping("/like/{targetType}/{targetId}")
    public Result<Object> toggleLike(
            @Parameter(description = "目标类型: post/comment") @PathVariable String targetType,
            @Parameter(description = "目标ID") @PathVariable Long targetId,
            @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleLike(targetType, targetId, token));
    }

    // ==================== 收藏 ====================

    @Operation(summary = "收藏/取消收藏", description = "对动态进行收藏/取消收藏")
    @PostMapping("/favorite/{targetType}/{targetId}")
    public Result<Object> toggleFavorite(
            @Parameter(description = "目标类型: post") @PathVariable String targetType,
            @Parameter(description = "目标ID") @PathVariable Long targetId,
            @RequestHeader("Authorization") String token) {
        return Result.success(communityService.toggleFavorite(targetType, targetId, token));
    }

    @Operation(summary = "我的收藏列表", description = "查询当前用户的收藏列表")
    @GetMapping("/favorite/list")
    public Result<Object> listFavorites(@RequestParam String targetType,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestHeader("Authorization") String token) {
        return Result.success(communityService.listFavorites(targetType, page, size, token));
    }

    // ==================== 排行榜 ====================

    @Operation(summary = "排行榜", description = "获取积分/贡献排行榜（公开接口）")
    @GetMapping("/leaderboard")
    public Result<Object> getLeaderboard(
            @Parameter(description = "排行类型: points") @RequestParam(defaultValue = "points") String type) {
        return Result.success(communityService.getLeaderboard(type));
    }
}
