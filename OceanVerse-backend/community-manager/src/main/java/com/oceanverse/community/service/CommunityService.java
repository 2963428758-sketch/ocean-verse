package com.oceanverse.community.service;

import com.oceanverse.pojo.dto.CommentCreateDTO;
import com.oceanverse.pojo.dto.PostCreateDTO;
import com.oceanverse.pojo.dto.PostQueryDTO;

public interface CommunityService {
    void createPost(PostCreateDTO dto, String token);
    Object listPosts(PostQueryDTO query);
    Object getPostDetail(Long id);
    void deletePost(Long id, String token);
    void createComment(CommentCreateDTO dto, String token);
    Object listComments(Long postId, Integer page, Integer size);
    Object toggleLike(String targetType, Long targetId, String token);
    Object toggleFavorite(String targetType, Long targetId, String token);
    Object listFavorites(String targetType, Integer page, Integer size, String token);
    Object getLeaderboard(String type);
}
