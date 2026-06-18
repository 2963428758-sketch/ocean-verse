<template>
  <div class="favorites-page">
    <!-- 导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">我的收藏</span>
      <div style="width: 36px"></div>
    </div>

    <!-- 瀑布流 -->
    <div v-loading="loading" class="waterfall">
      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">⭐</div>
        <p>暂无收藏</p>
      </div>

      <div
        v-for="post in posts"
        :key="post.id"
        class="feed-card"
        @click="$router.push(`/community/post/${post.id}`)"
      >
        <div v-if="parseImages(post.imageUrls).length" class="card-image-wrap">
          <img :src="parseImages(post.imageUrls)[0]" class="card-image" loading="lazy" />
          <span v-if="parseImages(post.imageUrls).length > 1" class="image-count">
            {{ parseImages(post.imageUrls).length }}张
          </span>
          <span v-if="post.postType !== 'NORMAL'" class="type-badge" :class="post.postType">
            {{ typeLabel(post.postType) }}
          </span>
        </div>
        <div v-else class="card-image-wrap no-image">
          <span class="no-image-text">📝</span>
        </div>
        <div class="card-body">
          <p class="card-text">{{ post.content }}</p>
          <div class="card-footer">
            <div class="card-user">
              <div class="card-avatar">{{ post.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
              <span class="card-username">{{ post.username || '用户' }}</span>
            </div>
            <span class="card-time">{{ formatTime(post.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <button class="page-btn" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
      <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; load()">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFavoriteList } from '@/api/community'
import type { CommunityPost } from '@/types'

const loading = ref(false)
const posts = ref<CommunityPost[]>([])
const page = ref(1)
const pageSize = 12
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res: any = await getFavoriteList({ targetType: 'POST', page: page.value, size: pageSize })
    posts.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function parseImages(imageUrls?: string): string[] {
  if (!imageUrls) return []
  try { const a = JSON.parse(imageUrls); return Array.isArray(a) ? a.filter(Boolean) : [] }
  catch { return imageUrls ? [imageUrls] : [] }
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

function typeLabel(t: string) { return { NORMAL: '日常', OBSERVATION: '观测', RECOGNITION: '识别' }[t] || t }

onMounted(load)
</script>

<style scoped lang="scss">
.favorites-page {
  max-width: 960px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
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
  &:hover { background: var(--neutral-75); transform: translateX(-2px); }
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--neutral-800);
}

/* ══════ 瀑布流 ══════ */
.waterfall {
  columns: 2;
  column-gap: 14px;
}

.feed-card {
  break-inside: avoid;
  margin-bottom: 14px;
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
  border: 1px solid var(--neutral-75);
  display: inline-block;
  width: 100%;
  &:hover { box-shadow: var(--shadow-lg); transform: translateY(-3px); }
}

.card-image-wrap {
  position: relative;
  width: 100%;
  overflow: hidden;
  &.no-image {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 140px;
    background: linear-gradient(135deg, #e8f4f8, #f0f4f8);
    .no-image-text { font-size: 36px; opacity: 0.4; }
  }
}

.card-image {
  width: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.4s var(--ease-out);
  .feed-card:hover & { transform: scale(1.03); }
}

.image-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 12px;
}

.type-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  &.NORMAL { background: rgba(26,107,138,0.85); color: #fff; }
  &.OBSERVATION { background: rgba(45,138,94,0.85); color: #fff; }
  &.RECOGNITION { background: rgba(224,120,80,0.85); color: #fff; }
}

.card-body { padding: 12px 14px 10px; }

.card-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--neutral-700);
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid var(--neutral-75);
}

.card-user { display: flex; align-items: center; gap: 6px; min-width: 0; }

.card-avatar {
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--gradient-ocean); color: #fff;
  font-size: 10px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.card-username {
  font-size: 12px; font-weight: 500; color: var(--neutral-600);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.card-time { font-size: 11px; color: var(--neutral-400); flex-shrink: 0; }

/* ══════ 分页 ══════ */
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
  &:hover:not(:disabled) { border-color: var(--primary-main); color: var(--primary-main); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

.page-info { font-size: 13px; color: var(--neutral-500); }

.empty-state {
  columns: 1;
  text-align: center;
  padding: 80px 20px;
  .empty-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.4; }
  p { font-size: 14px; color: var(--neutral-400); }
}
</style>
