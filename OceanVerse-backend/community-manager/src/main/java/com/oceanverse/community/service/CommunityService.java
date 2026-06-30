package com.oceanverse.community.service;

import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;

public interface CommunityService {
    void createPost(PostCreateDTO dto, String token);
    void updatePost(Long id, PostCreateDTO dto, String token);
    Object listPosts(PostQueryDTO query, String token);
    Object getPostDetail(Long id, String token);
    void deletePost(Long id, String token);
    Object listPendingPosts(Integer page, Integer size);
    void approvePost(Long id);
    void rejectPost(Long id);
    void createComment(CommentCreateDTO dto, String token);
    Object listComments(Long postId, Integer page, Integer size);
    Object toggleLike(String targetType, Long targetId, String token);
    Object toggleFavorite(String targetType, Long targetId, String token);
    Object listFavorites(String targetType, Integer page, Integer size, String token);
    Object listLikedPosts(Integer page, Integer size, String token);
    Object getLeaderboard(String type);
    Object toggleFollow(Long followUserId, String token);
    Object getUserProfile(Long userId);
    Object listNotifications(Integer page, Integer size, String token);
    Object getUnreadCount(String token);
    void markNotificationRead(Long notificationId, String token);
    void markAllRead(String token);
    void deleteNotification(Long id, String token);
    void deleteAllReadNotifications(String token);
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file, String token);
    void deleteComment(Long commentId, String token);
    void updateProfile(String nickname, String token);
    void updateBio(String bio, String token);
    String uploadBackground(org.springframework.web.multipart.MultipartFile file, String token);
    Object getFollowingList(Long userId, String token);
    Object getFollowerList(Long userId, String token);
}
