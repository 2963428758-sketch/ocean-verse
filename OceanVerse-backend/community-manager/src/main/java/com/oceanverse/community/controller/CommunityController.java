package com.oceanverse.community.controller;

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
 * 包含：动态发布、评论、点赞、收藏
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
    public Result<Object> listPosts(PostQueryDTO query) {
        return Result.success(communityService.listPosts(query));
    }

    @GetMapping("/post/{id}")
    public Result<Object> getPost(@PathVariable Long id) {
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
    public Result<Object> listComments(@PathVariable Long postId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(communityService.listComments(postId, page, size));
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

    // ==================== 排行榜 ====================

    @GetMapping("/leaderboard")
    public Result<Object> getLeaderboard(@RequestParam(defaultValue = "points") String type) {
        return Result.success(communityService.getLeaderboard(type));
    }
}
