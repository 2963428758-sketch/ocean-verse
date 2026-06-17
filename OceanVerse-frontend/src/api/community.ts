import http from '@/utils/http'

// ==================== 动态 ====================

export const createPost = (data: any) => http.post('/community/post', data)

export const getPostList = (params: any) => http.get('/community/post/list', { params })

export const getPostDetail = (id: number) => http.get(`/community/post/${id}`)

export const deletePost = (id: number) => http.delete(`/community/post/${id}`)

// ==================== 评论 ====================

export const createComment = (data: any) => http.post('/community/comment', data)

export const getCommentList = (postId: number, params?: any) =>
  http.get(`/community/comment/list/${postId}`, { params })

export const deleteComment = (id: number) => http.delete(`/community/comment/${id}`)

// ==================== 点赞 ====================

export const toggleLike = (targetType: string, targetId: number) =>
  http.post(`/community/like/${targetType}/${targetId}`)

// ==================== 收藏 ====================

export const toggleFavorite = (targetType: string, targetId: number) =>
  http.post(`/community/favorite/${targetType}/${targetId}`)

export const getFavoriteList = (params?: any) => http.get('/community/favorite/list', { params })

export const getLikedList = (params?: any) => http.get('/community/like/list', { params })

// ==================== 关注 ====================

export const toggleFollow = (userId: number) => http.post(`/community/follow/${userId}`)

// ==================== 用户主页 ====================

export const getUserProfile = (userId: number) => http.get(`/community/user/${userId}`)

// ==================== 通知 ====================

export const getNotificationList = (params?: any) =>
  http.get('/community/notification/list', { params })

export const getUnreadCount = () => http.get('/community/notification/unread')

export const markNotificationRead = (id: number) => http.put(`/community/notification/${id}/read`)

export const markAllRead = () => http.put('/community/notification/read-all')

// ==================== 头像 ====================

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.put('/community/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 排行榜 ====================

export const getLeaderboard = (type: string) =>
  http.get('/community/leaderboard', { params: { type } })
