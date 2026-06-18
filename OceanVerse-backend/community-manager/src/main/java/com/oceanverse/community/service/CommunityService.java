package com.oceanverse.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;
import com.oceanverse.pojo.entity.CommunityComment;
import com.oceanverse.pojo.entity.CommunityPost;
import com.oceanverse.pojo.entity.SysNotification;

import java.util.List;
import java.util.Map;

public interface CommunityService {
    void createPost(PostCreateDTO dto, String token);
    Page<CommunityPost> listPosts(PostQueryDTO query);
    CommunityPost getPostDetail(Long id);
    void deletePost(Long id, String token);
    void createComment(CommentCreateDTO dto, String token);
    Page<CommunityComment> listComments(Long postId, Integer page, Integer size);
    String toggleLike(String targetType, Long targetId, String token);
    String toggleFavorite(String targetType, Long targetId, String token);
    Page<CommunityPost> listFavorites(String targetType, Integer page, Integer size, String token);
    Page<CommunityPost> listLikedPosts(Integer page, Integer size, String token);
    List<Map<String, Object>> getLeaderboard(String type);
    String toggleFollow(Long followUserId, String token);
    Map<String, Object> getUserProfile(Long userId);
    Page<SysNotification> listNotifications(Integer page, Integer size, String token);
    Long getUnreadCount(String token);
    void markNotificationRead(Long notificationId, String token);
    void markAllRead(String token);
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file, String token);
    void deleteComment(Long commentId, String token);
}
