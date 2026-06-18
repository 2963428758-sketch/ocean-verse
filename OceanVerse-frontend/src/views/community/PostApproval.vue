<template>
  <div class="approval-page">
    <div class="page-header">
      <h2>帖子审核</h2>
      <p class="header-desc">审核用户发布的社区动态</p>
    </div>

    <div v-loading="loading" class="approval-list">
      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">✅</div>
        <p>暂无待审核帖子</p>
      </div>

      <div v-for="post in posts" :key="post.id" class="approval-card">
        <div class="card-header">
          <div class="user-info">
            <div class="avatar">{{ post.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
            <div class="meta">
              <span class="username">{{ post.username || '用户' }}</span>
              <span class="time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <span class="type-tag" :class="post.postType">{{ typeLabel(post.postType) }}</span>
        </div>

        <div class="card-content">
          <p>{{ post.content }}</p>
        </div>

        <div v-if="parseImages(post.imageUrls).length" class="card-images">
          <img
            v-for="(img, idx) in parseImages(post.imageUrls).slice(0, 3)"
            :key="idx"
            :src="img"
            class="preview-img"
          />
          <span v-if="parseImages(post.imageUrls).length > 3" class="more-count">
            +{{ parseImages(post.imageUrls).length - 3 }}
          </span>
        </div>

        <div class="card-actions">
          <button class="action-btn reject" @click="handleReject(post)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            驳回
          </button>
          <button class="action-btn approve" @click="handleApprove(post)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
            通过
          </button>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <button class="page-btn" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
      <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; load()">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingPosts, approvePost, rejectPost } from '@/api/community'
import type { CommunityPost } from '@/types'

const loading = ref(false)
const posts = ref<CommunityPost[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res: any = await getPendingPosts({ page: page.value, size: pageSize })
    posts.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleApprove(post: CommunityPost) {
  try {
    await approvePost(post.id)
    ElMessage.success('已通过')
    posts.value = posts.value.filter(p => p.id !== post.id)
    total.value = Math.max(0, total.value - 1)
  } catch (e) {
    console.error(e)
  }
}

async function handleReject(post: CommunityPost) {
  try {
    await rejectPost(post.id)
    ElMessage.success('已驳回')
    posts.value = posts.value.filter(p => p.id !== post.id)
    total.value = Math.max(0, total.value - 1)
  } catch (e) {
    console.error(e)
  }
}

function parseImages(imageUrls?: string): string[] {
  if (!imageUrls) return []
  try {
    const arr = JSON.parse(imageUrls)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch {
    return imageUrls ? [imageUrls] : []
  }
}

function formatTime(t?: string) {
  if (!t) return ''
  const d = new Date(t), now = new Date(), m = Math.floor((now.getTime() - d.getTime()) / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}小时前`
  const dy = Math.floor(h / 24)
  if (dy < 30) return `${dy}天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function typeLabel(t: string) {
  return { NORMAL: '日常', OBSERVATION: '观测', RECOGNITION: '识别' }[t] || t
}

onMounted(load)
</script>

<style scoped lang="scss">
.approval-page {
  max-width: 720px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    font-size: 22px;
    font-weight: 700;
    color: var(--neutral-800);
    margin-bottom: 4px;
  }

  .header-desc {
    font-size: 13px;
    color: var(--neutral-400);
  }
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.approval-card {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-75);
  padding: 18px;
  transition: all 0.25s var(--ease-out);

  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);
}

.time {
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

.card-content {
  margin-bottom: 12px;

  p {
    font-size: 14px;
    line-height: 1.65;
    color: var(--neutral-700);
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.card-images {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  position: relative;
}

.preview-img {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  border: 1px solid var(--neutral-100);
}

.more-count {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: var(--radius-sm);
  background: var(--neutral-75);
  color: var(--neutral-500);
  font-size: 14px;
  font-weight: 600;
}

.card-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid var(--neutral-75);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &.reject {
    background: var(--neutral-75);
    color: var(--neutral-600);

    &:hover {
      background: #fde8e8;
      color: var(--danger);
    }
  }

  &.approve {
    background: var(--gradient-ocean);
    color: #fff;
    box-shadow: 0 2px 8px rgba(26, 107, 138, 0.25);

    &:hover {
      box-shadow: 0 4px 14px rgba(26, 107, 138, 0.35);
      transform: translateY(-1px);
    }
  }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
    opacity: 0.5;
  }

  p {
    font-size: 14px;
    color: var(--neutral-400);
  }
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
}

.page-btn {
  padding: 8px 20px;
  border: 1px solid var(--neutral-200);
  border-radius: 20px;
  font-size: 13px;
  color: var(--neutral-600);
  background: var(--surface-card);
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    border-color: var(--primary-main);
    color: var(--primary-main);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.page-info {
  font-size: 13px;
  color: var(--neutral-500);
}
</style>
