<template>
  <div v-loading="loading" class="profile-page">
    <!-- 返回 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">用户主页</span>
      <div style="width: 36px"></div>
    </div>

    <!-- 用户信息卡片 -->
    <div v-if="profile" class="profile-card">
      <div class="profile-cover"></div>
      <div class="profile-body">
        <div class="profile-top">
          <div class="profile-avatar-wrap">
            <div class="profile-avatar">
              {{ profile.username?.charAt(0)?.toUpperCase() || 'U' }}
            </div>
          </div>
          <div class="profile-info">
            <h2 class="profile-name">{{ profile.username }}</h2>
            <p class="profile-id">ID: {{ userId }}</p>
          </div>
          <div v-if="!isSelf" class="profile-action">
            <button
              class="follow-btn"
              :class="{ following: isFollowing }"
              :disabled="followLoading"
              @click="handleFollow"
            >
              {{ isFollowing ? '已关注' : '+ 关注' }}
            </button>
          </div>
        </div>

        <!-- 数据统计 -->
        <div class="profile-stats">
          <div class="stat-item">
            <span class="stat-value">{{ profile.postCount }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.followerCount }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ profile.followCount }}</span>
            <span class="stat-label">关注</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户帖子 - 瀑布流 -->
    <div class="posts-section">
      <h3 class="section-title">TA 的动态</h3>
      <div v-loading="loadingPosts" class="waterfall">
        <div v-if="userPosts.length === 0 && !loadingPosts" class="empty-state">
          <p>暂无动态</p>
        </div>
        <div
          v-for="post in userPosts"
          :key="post.id"
          class="feed-card"
          @click="goToPost(post.id)"
        >
          <div v-if="parseImages(post.imageUrls).length" class="card-image-wrap">
            <img :src="parseImages(post.imageUrls)[0]" class="card-image" loading="lazy" />
            <span v-if="parseImages(post.imageUrls).length > 1" class="image-count">
              {{ parseImages(post.imageUrls).length }}张
            </span>
          </div>
          <div v-else class="card-image-wrap no-image">
            <span class="no-image-text">📝</span>
          </div>
          <div class="card-body">
            <p class="card-text">{{ post.content }}</p>
            <div class="card-footer">
              <span class="card-time">{{ formatTime(post.createTime) }}</span>
              <div class="card-stats">
                <span class="stat">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  {{ post.likeCount }}
                </span>
                <span class="stat">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                  {{ post.commentCount }}
                </span>
              </div>
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
import { useUserStore } from '@/stores/user'
import { getUserProfile, toggleFollow, getPostList } from '@/api/community'
import type { UserProfile, CommunityPost } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = Number(route.params.id)

const loading = ref(false)
const loadingPosts = ref(false)
const followLoading = ref(false)
const profile = ref<UserProfile | null>(null)
const userPosts = ref<CommunityPost[]>([])
const isFollowing = ref(false)
const isSelf = computed(() => userStore.userId === userId)

async function loadProfile() {
  loading.value = true
  try {
    const res: any = await getUserProfile(userId)
    profile.value = res.data || res
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadUserPosts() {
  loadingPosts.value = true
  try {
    const res: any = await getPostList({ userId, page: 1, size: 50 })
    userPosts.value = res.data?.records || res.data || []
  } catch (e) { console.error(e) }
  finally { loadingPosts.value = false }
}

async function handleFollow() {
  followLoading.value = true
  try {
    const res: any = await toggleFollow(userId)
    isFollowing.value = !isFollowing.value
    if (profile.value) {
      profile.value.followerCount += isFollowing.value ? 1 : -1
    }
    ElMessage.success(res.data || res.message || '操作成功')
  } catch (e) { console.error(e) }
  finally { followLoading.value = false }
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

function goToPost(id: number) {
  router.push(`/community/post/${id}`)
}

onMounted(() => {
  loadProfile()
  loadUserPosts()
})
</script>

<style scoped lang="scss">
.profile-page {
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

/* ══════ 个人信息卡片 ══════ */
.profile-card {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-75);
  overflow: hidden;
  margin-bottom: 20px;
}

.profile-cover {
  height: 100px;
  background: linear-gradient(135deg, #1a6b8a 0%, #2d8cb0 40%, #5bb5d5 100%);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 40px;
    background: linear-gradient(to top, rgba(255,255,255,0.1), transparent);
  }
}

.profile-body {
  padding: 0 24px 20px;
}

.profile-top {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-top: -32px;
  position: relative;
  z-index: 1;
}

.profile-avatar-wrap {
  flex-shrink: 0;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid var(--surface-card);
  box-shadow: var(--shadow-md);
}

.profile-info {
  flex: 1;
  min-width: 0;
  padding-bottom: 4px;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
  margin-bottom: 2px;
}

.profile-id {
  font-size: 12px;
  color: var(--neutral-400);
}

.profile-action {
  padding-bottom: 4px;
}

.follow-btn {
  padding: 8px 28px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  background: var(--gradient-ocean);
  color: #fff;
  box-shadow: 0 3px 12px rgba(26, 107, 138, 0.3);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 5px 18px rgba(26, 107, 138, 0.4);
  }

  &.following {
    background: var(--neutral-75);
    color: var(--neutral-600);
    box-shadow: none;

    &:hover {
      background: var(--neutral-100);
    }
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

/* ══════ 统计 ══════ */
.profile-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-top: 20px;
  padding: 16px 0;
  background: var(--neutral-25);
  border-radius: var(--radius-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
}

.stat-label {
  font-size: 12px;
  color: var(--neutral-400);
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: var(--neutral-100);
}

/* ══════ 帖子区域 ══════ */
.posts-section {
  margin-top: 4px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: 14px;
  padding-left: 2px;
}

/* ══════ 瀑布流 ══════ */
.waterfall {
  columns: 2;
  column-gap: 12px;
}

.feed-card {
  break-inside: avoid;
  margin-bottom: 12px;
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
  border: 1px solid var(--neutral-75);
  display: inline-block;
  width: 100%;

  &:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
  }
}

.card-image-wrap {
  position: relative;
  width: 100%;
  overflow: hidden;

  &.no-image {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 120px;
    background: linear-gradient(135deg, #e8f4f8, #f0f4f8);

    .no-image-text {
      font-size: 32px;
      opacity: 0.4;
    }
  }
}

.card-image {
  width: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.4s var(--ease-out);

  .feed-card:hover & {
    transform: scale(1.03);
  }
}

.image-count {
  position: absolute;
  bottom: 6px;
  right: 6px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(6px);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
}

.card-body {
  padding: 10px 12px 8px;
}

.card-text {
  font-size: 13px;
  line-height: 1.55;
  color: var(--neutral-700);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 6px;
  border-top: 1px solid var(--neutral-75);
}

.card-time {
  font-size: 11px;
  color: var(--neutral-400);
}

.card-stats {
  display: flex;
  gap: 10px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--neutral-400);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  columns: 1;

  p {
    font-size: 14px;
    color: var(--neutral-400);
  }
}
</style>
