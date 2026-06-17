<template>
  <div class="liked-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">点赞记录</span>
      </template>
    </el-page-header>

    <div v-loading="loading" class="post-list">
      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无点赞记录" />
      </div>

      <div v-for="post in posts" :key="post.id" class="post-card" @click="$router.push(`/community/post/${post.id}`)">
        <div class="post-header">
          <div class="user-info">
            <el-avatar :size="36" class="user-avatar">
              {{ post.username?.charAt(0)?.toUpperCase() || 'U' }}
            </el-avatar>
            <div class="user-meta">
              <span class="username">{{ post.username || '用户' }}</span>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <el-tag v-if="post.postType !== 'NORMAL'" :type="typeTag(post.postType)" size="small" effect="light">
            {{ typeLabel(post.postType) }}
          </el-tag>
        </div>
        <p class="post-content">{{ post.content }}</p>
        <div v-if="parseImages(post.imageUrls).length" class="post-images">
          <el-image
            v-for="(img, idx) in parseImages(post.imageUrls).slice(0, 3)"
            :key="idx" :src="img" fit="cover" class="post-image"
          />
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="load"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLikedList } from '@/api/community'
import type { CommunityPost } from '@/types'

const loading = ref(false)
const posts = ref<CommunityPost[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res: any = await getLikedList({ page: page.value, size: pageSize })
    posts.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
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
  if (m < 60) return `${m} 分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前`
  const dy = Math.floor(h / 24)
  if (dy < 30) return `${dy} 天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function typeLabel(t: string) { return { NORMAL: '日常', OBSERVATION: '观测', RECOGNITION: '识别' }[t] || t }
function typeTag(t: string): '' | 'success' | 'warning' | 'info' { return { NORMAL: '', OBSERVATION: 'success', RECOGNITION: 'warning' }[t] as any || 'info' }

onMounted(load)
</script>

<style scoped lang="scss">
.liked-page { max-width: 720px; margin: 0 auto; animation: fadeIn 0.4s ease; }
.page-title { font-size: 15px; font-weight: 600; }
:deep(.el-page-header) { margin-bottom: 12px; }

.post-card {
  background: var(--surface-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);
  padding: 12px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  &:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
}
.post-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.user-info { display: flex; align-items: center; gap: 6px; }
.user-avatar { background: var(--gradient-ocean); color: #fff; font-weight: 600; font-size: 12px; }
.user-meta { display: flex; flex-direction: column; gap: 1px; }
.username { font-size: 12px; font-weight: 600; color: var(--neutral-700); }
.post-time { font-size: 11px; color: var(--neutral-400); }
.post-content { font-size: 13px; line-height: 1.5; color: var(--neutral-700); margin-bottom: 6px; white-space: pre-wrap; }
.post-images { display: flex; gap: 4px; margin-bottom: 2px; }
.post-image { width: 72px; height: 72px; border-radius: var(--radius-xs); object-fit: cover; }
.pagination-wrap { display: flex; justify-content: center; padding: 12px 0; }
.empty-state { padding: 30px 0; }
</style>
