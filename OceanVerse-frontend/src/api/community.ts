import http from '@/utils/http'

// 发布动态
export const createPost = (data: any) => http.post('/community/post', data)

// 动态列表
export const getPostList = (params: any) => http.get('/community/post/list', { params })

// 动态详情
export const getPostDetail = (id: number) => http.get(`/community/post/${id}`)

// 删除动态
export const deletePost = (id: number) => http.delete(`/community/post/${id}`)

// 发表评论
export const createComment = (data: any) => http.post('/community/comment', data)

// 评论列表
export const getCommentList = (postId: number, params: any) =>
  http.get(`/community/comment/list/${postId}`, { params })

// 切换点赞
export const toggleLike = (targetType: string, targetId: number) =>
  http.post(`/community/like/${targetType}/${targetId}`)

// 切换收藏
export const toggleFavorite = (targetType: string, targetId: number) =>
  http.post(`/community/favorite/${targetType}/${targetId}`)

// 收藏列表
export const getFavoriteList = (params: any) => http.get('/community/favorite/list', { params })

// 排行榜
export const getLeaderboard = (type: string) =>
  http.get('/community/leaderboard', { params: { type } })
