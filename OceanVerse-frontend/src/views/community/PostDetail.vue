<template>
  <div v-loading="loading" class="post-detail-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">帖子详情</span>
      </template>
    </el-page-header>

    <div v-if="post" class="post-detail-card">
      <div class="post-header">
        <div class="user-info">
          <el-avatar :size="34" class="user-avatar">
            {{ post.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <div class="user-meta">
            <span class="username" @click="$router.push(`/community/user/${post.userId}`)">
              {{ post.username || '用户' }}
            </span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
          </div>
        </div>
        <el-popconfirm
          v-if="post.userId === userStore.userId"
          title="确定删除这条帖子？"
          @confirm="handleDeletePost"
        >
          <template #reference>
            <button class="post-delete-btn">删除</button>
          </template>
        </el-popconfirm>
      </div>

      <div class="post-content">
        <p>{{ post.content }}</p>
      </div>

      <div v-if="parsedImages.length" class="post-images">
        <el-image
          v-for="(img, idx) in parsedImages"
          :key="idx"
          :src="img"
          :preview-src-list="parsedImages"
          :initial-index="idx"
          fit="cover"
          class="post-image"
        />
      </div>

      <div class="post-actions">
        <button class="action-btn" :class="{ active: isLiked }" @click="handleLikePost">
          <el-icon><Star /></el-icon>
          <span>{{ post.likeCount || 0 }} 赞</span>
        </button>
        <span class="action-stat">{{ post.commentCount || 0 }} 评论</span>
        <button class="action-btn" :class="{ active: isFavorited }" @click="handleFavoritePost">
          <el-icon><StarFilled /></el-icon>
          <span>{{ post.favoriteCount || 0 }} 收藏</span>
        </button>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comment-section">
      <h3 class="section-title">评论 ({{ post?.commentCount || 0 }})</h3>

      <div class="comment-input">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="2"
          placeholder="写下你的评论..."
          maxlength="200"
          show-word-limit
        />
        <el-button type="primary" size="small" :loading="commenting" @click="handleComment">
          发表评论
        </el-button>
      </div>

      <div v-loading="loadingComments" class="comment-list">
        <div v-if="comments.length === 0 && !loadingComments" class="empty-comments">
          暂无评论，快来抢沙发吧！
        </div>
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <el-avatar :size="32" class="comment-avatar">
            {{ comment.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="comment-user" @click="$router.push(`/community/user/${comment.userId}`)">{{ comment.username || '用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <p class="comment-text">{{ comment.content }}</p>
            <div class="comment-footer">
              <button
                class="comment-like-btn"
                :class="{ active: comment.isLiked }"
                @click="handleLikeComment(comment)"
              >
                <el-icon><Star /></el-icon>
                <span>{{ comment.likeCount || 0 }}</span>
              </button>
              <el-popconfirm
                v-if="comment.userId === userStore.userId"
                title="确定删除这条评论？"
                @confirm="handleDeleteComment(comment)"
              >
                <template #reference>
                  <button class="comment-delete-btn">删除</button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import {
  getPostDetail, getCommentList, createComment, deleteComment, deletePost,
  toggleLike, toggleFavorite
} from '@/api/community'
import type { CommunityPost, CommunityComment } from '@/types'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const postId = Number(route.params.id)

const loading = ref(false)
const loadingComments = ref(false)
const commenting = ref(false)
const post = ref<CommunityPost | null>(null)
const comments = ref<(CommunityComment & { username?: string; isLiked?: boolean })[]>([])
const commentContent = ref('')
const isLiked = ref(false)
const isFavorited = ref(false)

const parsedImages = computed(() => {
  if (!post.value?.imageUrls) return []
  try {
    const arr = JSON.parse(post.value.imageUrls)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch {
    return post.value.imageUrls ? [post.value.imageUrls] : []
  }
})

async function loadPost() {
  loading.value = true
  try {
    const res: any = await getPostDetail(postId)
    post.value = res.data || res
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  loadingComments.value = true
  try {
    const res: any = await getCommentList(postId, { page: 1, size: 50 })
    const records = res.data?.records || res.data || []
    comments.value = records.map((c: CommunityComment) => ({
      ...c,
      isLiked: false,
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loadingComments.value = false
  }
}

async function handleLikePost() {
  try {
    await toggleLike('POST', postId)
    isLiked.value = !isLiked.value
    if (post.value) post.value.likeCount += isLiked.value ? 1 : -1
  } catch (e) {
    console.error(e)
  }
}

async function handleFavoritePost() {
  try {
    await toggleFavorite('POST', postId)
    isFavorited.value = !isFavorited.value
    if (post.value) post.value.favoriteCount += isFavorited.value ? 1 : -1
  } catch (e) {
    console.error(e)
  }
}

async function handleLikeComment(comment: any) {
  try {
    await toggleLike('COMMENT', comment.id)
    comment.isLiked = !comment.isLiked
    comment.likeCount += comment.isLiked ? 1 : -1
  } catch (e) {
    console.error(e)
  }
}

async function handleDeleteComment(comment: any) {
  try {
    await deleteComment(comment.id)
    comments.value = comments.value.filter(c => c.id !== comment.id)
    if (post.value) post.value.commentCount = Math.max(0, post.value.commentCount - 1)
    ElMessage.success('评论已删除')
  } catch (e) {
    console.error(e)
  }
}

async function handleDeletePost() {
  try {
    await deletePost(postId)
    ElMessage.success('帖子已删除')
    router.push('/community/feed')
  } catch (e) {
    console.error(e)
  }
}

async function handleComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await createComment({ postId, content: commentContent.value })
    ElMessage.success('评论成功')
    commentContent.value = ''
    loadComments()
    if (post.value) post.value.commentCount++
  } catch (e) {
    console.error(e)
  } finally {
    commenting.value = false
  }
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

onMounted(() => {
  loadPost()
  loadComments()
})
</script>

<style scoped lang="scss">
.post-detail-page {
  max-width: 720px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

.page-title {
  font-size: 15px;
  font-weight: 600;
}

:deep(.el-page-header) {
  margin-bottom: 12px;
}

/* ══════ 帖子卡片 ══════ */
.post-detail-card {
  background: var(--surface-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);
  padding: 14px;
  margin-bottom: 8px;
}

.post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: var(--gradient-ocean);
  color: #fff;
  font-weight: 600;
  font-size: 13px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.username {
  font-size: 13px;
  font-weight: 600;
  color: var(--neutral-700);
  cursor: pointer;
  transition: color 0.15s;

  &:hover {
    color: var(--primary-main);
  }
}

.post-time {
  font-size: 11px;
  color: var(--neutral-400);
}

.post-content {
  margin-bottom: 8px;

  p {
    font-size: 14px;
    line-height: 1.55;
    color: var(--neutral-700);
    white-space: pre-wrap;
  }
}

.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 8px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.post-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--radius-xs);
}

.post-delete-btn {
  background: none;
  border: 1px solid var(--neutral-200);
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  transition: all 0.2s;

  &:hover {
    color: var(--danger);
    border-color: var(--danger);
    background: rgba(196, 53, 53, 0.04);
  }
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid var(--neutral-75);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--neutral-400);
  padding: 3px 6px;
  border-radius: var(--radius-xs);
  transition: all 0.2s;

  &:hover {
    color: var(--primary-main);
    background: var(--primary-soft);
  }

  &.active {
    color: var(--accent-warm);
  }
}

.action-stat {
  font-size: 13px;
  color: var(--neutral-400);
}

/* ══════ 评论区 ══════ */
.comment-section {
  background: var(--surface-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);
  padding: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);
  margin-bottom: 10px;
}

.comment-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;

  .el-button {
    align-self: flex-end;
  }
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-comments {
  text-align: center;
  padding: 20px 0;
  color: var(--neutral-400);
  font-size: 13px;
}

.comment-item {
  display: flex;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--neutral-75);

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.comment-avatar {
  background: var(--neutral-100);
  color: var(--neutral-500);
  font-size: 12px;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.comment-user {
  font-size: 12px;
  font-weight: 600;
  color: var(--neutral-600);
  cursor: pointer;
  transition: color 0.15s;

  &:hover {
    color: var(--primary-main);
  }
}

.comment-time {
  font-size: 11px;
  color: var(--neutral-400);
}

.comment-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--neutral-700);
  margin-bottom: 4px;
}

.comment-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 1px 6px;
  border-radius: var(--radius-xs);
  transition: all 0.2s;

  &:hover {
    color: var(--primary-main);
    background: var(--primary-soft);
  }

  &.active {
    color: var(--accent-warm);
  }
}

.comment-footer {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 1px 6px;
  border-radius: var(--radius-xs);
  transition: all 0.2s;

  &:hover {
    color: var(--danger);
    background: rgba(196, 53, 53, 0.06);
  }
}
</style>
