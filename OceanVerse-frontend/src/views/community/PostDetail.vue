<template>
  <div v-loading="loading" class="detail-page">
    <!-- 返回 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">帖子详情</span>
      <div class="nav-right">
        <el-popconfirm
          v-if="post?.userId === userStore.userId"
          title="确定删除这条帖子？"
          @confirm="handleDeletePost"
        >
          <template #reference>
            <button class="nav-action delete">删除</button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <div v-if="post" class="detail-content">
      <!-- 作者信息 -->
      <div class="author-bar">
        <div class="author-info" @click="$router.push(`/community/user/${post.userId}`)">
          <div class="author-avatar">
            {{ post.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="author-meta">
            <span class="author-name">{{ post.username || '用户' }}</span>
            <span class="author-time">{{ formatTime(post.createTime) }}</span>
          </div>
        </div>
        <span v-if="post.postType !== 'NORMAL'" class="type-tag" :class="post.postType">
          {{ postTypeLabel(post.postType) }}
        </span>
      </div>

      <!-- 文字内容 -->
      <div class="post-text">
        <p>{{ post.content }}</p>
      </div>

      <!-- 图片 -->
      <div v-if="parsedImages.length" class="post-gallery">
        <img
          v-for="(img, idx) in parsedImages"
          :key="idx"
          :src="img"
          class="gallery-image"
          @click="previewImage(idx)"
        />
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <button class="action-item" :class="{ active: isLiked }" @click="handleLikePost">
          <svg width="20" height="20" viewBox="0 0 24 24" :fill="isLiked ? '#ff4757' : 'none'" :stroke="isLiked ? '#ff4757' : '#6e695f'" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          <span>{{ post.likeCount || 0 }}</span>
        </button>
        <button class="action-item comment-action">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6e695f" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <span>{{ post.commentCount || 0 }}</span>
        </button>
        <button class="action-item" :class="{ active: isFavorited }" @click="handleFavoritePost">
          <svg width="20" height="20" viewBox="0 0 24 24" :fill="isFavorited ? '#e07850' : 'none'" :stroke="isFavorited ? '#e07850' : '#6e695f'" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          <span>{{ post.favoriteCount || 0 }}</span>
        </button>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comment-section">
      <div class="comment-header">
        <span class="comment-count">评论 ({{ post?.commentCount || 0 }})</span>
      </div>

      <!-- 评论输入 -->
      <div class="comment-input-box">
        <div class="input-avatar">
          {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
        </div>
        <div class="input-wrap">
          <textarea
            v-model="commentContent"
            class="comment-textarea"
            placeholder="写评论..."
            maxlength="200"
            rows="1"
          ></textarea>
          <button
            class="comment-submit"
            :disabled="commenting || !commentContent.trim()"
            @click="handleComment"
          >
            {{ commenting ? '...' : '发送' }}
          </button>
        </div>
      </div>

      <!-- 评论列表 -->
      <div v-loading="loadingComments" class="comment-list">
        <div v-if="comments.length === 0 && !loadingComments" class="empty-comments">
          暂无评论，快来抢沙发吧 ~
        </div>
        <div v-for="comment in comments" :key="comment.id" :id="`comment-${comment.id}`" class="comment-item">
          <div class="comment-avatar" @click="$router.push(`/community/user/${comment.userId}`)">
            {{ comment.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="comment-name" @click="$router.push(`/community/user/${comment.userId}`)">{{ comment.username || '用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <p class="comment-text">{{ comment.content }}</p>
            <div class="comment-actions">
              <button
                class="comment-like"
                :class="{ active: comment.isLiked }"
                @click="handleLikeComment(comment)"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" :fill="comment.isLiked ? '#ff4757' : 'none'" :stroke="comment.isLiked ? '#ff4757' : '#948f86'" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                {{ comment.likeCount || 0 }}
              </button>
              <el-popconfirm
                v-if="comment.userId === userStore.userId"
                title="确定删除？"
                @confirm="handleDeleteComment(comment)"
              >
                <template #reference>
                  <button class="comment-delete">删除</button>
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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
    isLiked.value = !!(res.data || res).isLiked
    isFavorited.value = !!(res.data || res).isFavorited
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadComments() {
  loadingComments.value = true
  try {
    const res: any = await getCommentList(postId, { page: 1, size: 50 })
    const records = res.data?.records || res.data || []
    comments.value = records.map((c: CommunityComment) => ({ ...c, isLiked: false }))
  } catch (e) { console.error(e) }
  finally { loadingComments.value = false }
}

function scrollToComment() {
  const commentId = route.query.comment
  if (!commentId) return
  setTimeout(() => {
    const el = document.getElementById(`comment-${commentId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('highlight')
    }
  }, 300)
}

watch([loading, loadingComments], ([postLoading, commentsLoading]) => {
  if (!postLoading && !commentsLoading) {
    scrollToComment()
  }
})

onMounted(async () => {
  await Promise.all([loadPost(), loadComments()])
})

async function handleLikePost() {
  try {
    await toggleLike('POST', postId)
    isLiked.value = !isLiked.value
    if (post.value) post.value.likeCount += isLiked.value ? 1 : -1
  } catch (e) { console.error(e) }
}

async function handleFavoritePost() {
  try {
    await toggleFavorite('POST', postId)
    isFavorited.value = !isFavorited.value
    if (post.value) post.value.favoriteCount += isFavorited.value ? 1 : -1
  } catch (e) { console.error(e) }
}

async function handleLikeComment(comment: any) {
  try {
    await toggleLike('COMMENT', comment.id)
    comment.isLiked = !comment.isLiked
    comment.likeCount += comment.isLiked ? 1 : -1
  } catch (e) { console.error(e) }
}

async function handleDeleteComment(comment: any) {
  try {
    await deleteComment(comment.id)
    comments.value = comments.value.filter(c => c.id !== comment.id)
    if (post.value) post.value.commentCount = Math.max(0, post.value.commentCount - 1)
    ElMessage.success('已删除')
  } catch (e) { console.error(e) }
}

async function handleDeletePost() {
  try {
    await deletePost(postId)
    ElMessage.success('已删除')
    router.push('/community/feed')
  } catch (e) { console.error(e) }
}

async function handleComment() {
  if (!commentContent.value.trim()) return
  commenting.value = true
  try {
    await createComment({ postId, content: commentContent.value })
    ElMessage.success('评论成功')
    commentContent.value = ''
    loadComments()
    if (post.value) post.value.commentCount++
  } catch (e) { console.error(e) }
  finally { commenting.value = false }
}

function previewImage(idx: number) {
  // Simple preview - could use el-image-viewer
}

function postTypeLabel(type: string): string {
  return { NORMAL: '日常', OBSERVATION: '观测', RECOGNITION: '识别' }[type] || type
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

onMounted(() => {
  loadPost()
  loadComments()
})
</script>

<style scoped lang="scss">
.detail-page {
  max-width: 680px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

/* ══════ 导航栏 ══════ */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: var(--surface-card);
  color: var(--neutral-600);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--shadow-xs);

  &:hover {
    background: var(--neutral-75);
    transform: translateX(-2px);
  }
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
}

.nav-right {
  min-width: 36px;
  display: flex;
  justify-content: flex-end;
}

.nav-action {
  background: none;
  border: none;
  font-size: 13px;
  color: var(--neutral-400);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;

  &.delete:hover {
    color: var(--danger);
    background: rgba(196, 53, 53, 0.06);
  }
}

/* ══════ 内容区 ══════ */
.detail-content {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-75);
  overflow: hidden;
  margin-bottom: 14px;
}

/* ══════ 作者 ══════ */
.author-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px 0;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.05);
  }
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);

  &:hover {
    color: var(--primary-main);
  }
}

.author-time {
  font-size: 12px;
  color: var(--neutral-400);
}

.type-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;

  &.NORMAL { background: var(--primary-soft); color: var(--primary-main); }
  &.OBSERVATION { background: #e8f8ef; color: var(--success); }
  &.RECOGNITION { background: #fef4ee; color: var(--accent-warm); }
}

/* ══════ 文字 ══════ */
.post-text {
  padding: 14px 18px;

  p {
    font-size: 15px;
    line-height: 1.7;
    color: var(--neutral-700);
    white-space: pre-wrap;
    word-break: break-word;
  }
}

/* ══════ 图片 ══════ */
.post-gallery {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.gallery-image {
  width: 100%;
  display: block;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.92;
  }
}

/* ══════ 操作栏 ══════ */
.action-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 10px 18px 14px;
  border-top: 1px solid var(--neutral-75);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: var(--neutral-500);
  padding: 8px 18px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;

  &:hover {
    background: var(--neutral-75);
  }

  &.active {
    color: #ff4757;
  }

  &:nth-child(2) {
    border-left: 1px solid var(--neutral-75);
    border-right: 1px solid var(--neutral-75);
  }

  &:nth-child(3):hover {
    color: var(--accent-warm);
  }
}

/* ══════ 评论区 ══════ */
.comment-section {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-75);
  overflow: hidden;
}

.comment-header {
  padding: 14px 18px;
  border-bottom: 1px solid var(--neutral-75);
}

.comment-count {
  font-size: 15px;
  font-weight: 600;
  color: var(--neutral-800);
}

/* ══════ 评论输入 ══════ */
.comment-input-box {
  display: flex;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--neutral-75);
}

.input-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.input-wrap {
  flex: 1;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.comment-textarea {
  flex: 1;
  min-height: 36px;
  max-height: 100px;
  border: 1px solid var(--neutral-100);
  border-radius: 18px;
  padding: 8px 16px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--neutral-700);
  resize: none;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;

  &:focus {
    border-color: var(--primary-lighter);
  }

  &::placeholder {
    color: var(--neutral-300);
  }
}

.comment-submit {
  padding: 8px 18px;
  border: none;
  border-radius: 18px;
  background: var(--primary-main);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: var(--primary-light);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* ══════ 评论列表 ══════ */
.comment-list {
  padding: 4px 0;
}

.empty-comments {
  text-align: center;
  padding: 32px 0;
  color: var(--neutral-400);
  font-size: 13px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 18px;
  transition: background 0.15s;

  &:hover {
    background: var(--neutral-25);
  }

  & + .comment-item {
    border-top: 1px solid var(--neutral-75);
  }

  &.highlight {
    background: #e8f4f8;
    animation: highlightFade 2s ease-out;
  }
}

@keyframes highlightFade {
  0% { background: #b3e5fc; }
  100% { background: transparent; }
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--neutral-100);
  color: var(--neutral-500);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;

  &:hover {
    background: var(--primary-soft);
    color: var(--primary-main);
  }
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}

.comment-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--neutral-600);
  cursor: pointer;

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
  line-height: 1.6;
  color: var(--neutral-700);
  margin-bottom: 4px;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-like {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  transition: all 0.2s;

  &:hover {
    color: var(--primary-main);
    background: var(--primary-soft);
  }

  &.active {
    color: #ff4757;
  }
}

.comment-delete {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  transition: all 0.2s;

  &:hover {
    color: var(--danger);
    background: rgba(196, 53, 53, 0.06);
  }
}
</style>
