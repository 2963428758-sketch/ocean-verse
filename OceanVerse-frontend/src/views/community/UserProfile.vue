<template>
  <div v-loading="loading" class="user-profile-page">
    <!-- 返回导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">用户主页</span>
      <div style="width: 36px"></div>
    </div>

    <!-- 个人信息卡片 -->
    <div v-if="profile" class="profile-card">
      <!-- 背景图 -->
      <div class="profile-cover" :style="coverStyle">
        <div class="cover-gradient"></div>
      </div>

      <div class="profile-body">
        <div class="profile-top">
          <!-- 头像 -->
          <div class="avatar-wrap">
            <div class="profile-avatar">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" class="avatar-img" />
              <span v-else>{{ profile.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
            </div>
          </div>

          <div class="profile-info">
            <h2 class="profile-name">{{ profile.username }}</h2>
            <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
            <p class="profile-id">ID: {{ userId }}</p>
          </div>

          <div v-if="!isSelf" class="profile-action">
            <button
              class="follow-btn"
              :class="{ following: isFollowing }"
              :disabled="followLoading"
              @click="handleFollow"
            >
              <span v-if="followLoading" class="btn-loading"></span>
              <span v-else>{{ isFollowing ? '已关注' : '+ 关注' }}</span>
            </button>
          </div>
        </div>

        <!-- 数据统计 -->
        <div class="profile-stats">
          <div class="stat-item" @click="showTab = 'posts'">
            <span class="stat-value">{{ profile.postCount || 0 }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="showTab = 'followers'">
            <span class="stat-value">{{ profile.followerCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="showTab = 'following'">
            <span class="stat-value">{{ profile.followCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="profile-tabs">
          <button :class="{ active: showTab === 'posts' }" @click="showTab = 'posts'">TA 的动态</button>
          <button :class="{ active: showTab === 'followers' }" @click="showTab = 'followers'; loadFollowers()">粉丝</button>
          <button :class="{ active: showTab === 'following' }" @click="showTab = 'following'; loadFollowing()">关注</button>
        </div>
      </div>
    </div>

    <!-- TA 的动态 -->
    <div v-if="showTab === 'posts'" class="posts-section">
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
            <span class="no-image-icon">📝</span>
          </div>
          <div class="card-body">
            <p class="card-text">{{ post.content }}</p>
            <div class="card-footer">
              <span class="card-time">{{ formatTime(post.createTime) }}</span>
              <div class="card-stats">
                <span class="stat">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  {{ post.likeCount || 0 }}
                </span>
                <span class="stat">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                  {{ post.commentCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="totalPosts > pageSize" class="pagination-wrap">
        <button class="page-btn" :disabled="postPage <= 1" @click="postPage--; loadUserPosts()">上一页</button>
        <span class="page-info">{{ postPage }} / {{ Math.ceil(totalPosts / pageSize) }}</span>
        <button class="page-btn" :disabled="postPage >= Math.ceil(totalPosts / pageSize)" @click="postPage++; loadUserPosts()">下一页</button>
      </div>
    </div>

    <!-- 关注列表 -->
    <div v-if="showTab === 'following'" class="user-list-section">
      <div v-loading="loadingFollow" class="user-list">
        <div v-if="followingList.length === 0 && !loadingFollow" class="empty-state">
          <p>暂未关注任何人</p>
        </div>
        <div v-for="user in followingList" :key="user.userId" class="user-item" @click="$router.push(`/community/user/${user.userId}`)">
          <div class="list-avatar">
            <img v-if="user.avatarUrl" :src="user.avatarUrl" />
            <span v-else>{{ user.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </div>
          <span class="user-name">{{ user.username }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </div>

    <!-- 粉丝列表 -->
    <div v-if="showTab === 'followers'" class="user-list-section">
      <div v-loading="loadingFollow" class="user-list">
        <div v-if="followerList.length === 0 && !loadingFollow" class="empty-state">
          <p>暂无粉丝</p>
        </div>
        <div v-for="user in followerList" :key="user.userId" class="user-item" @click="$router.push(`/community/user/${user.userId}`)">
          <div class="list-avatar">
            <img v-if="user.avatarUrl" :src="user.avatarUrl" />
            <span v-else>{{ user.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </div>
          <span class="user-name">{{ user.username }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
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
import { getUserProfile, toggleFollow, getPostList, getFollowingList, getFollowerList } from '@/api/community'
import type { CommunityPost } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userId = Number(route.params.id)

const loading = ref(false)
const loadingPosts = ref(false)
const followLoading = ref(false)
const profile = ref<any>(null)
const userPosts = ref<CommunityPost[]>([])
const isFollowing = ref(false)
const isSelf = computed(() => userStore.userId === userId)

const showTab = ref('posts')
const postPage = ref(1)
const pageSize = 12
const totalPosts = ref(0)

const loadingFollow = ref(false)
const followingList = ref<{ userId: number; username: string; avatarUrl?: string }[]>([])
const followerList = ref<{ userId: number; username: string; avatarUrl?: string }[]>([])

const coverStyle = computed(() => {
  if (profile.value?.backgroundUrl) {
    return { backgroundImage: `url(${profile.value.backgroundUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
})

async function loadProfile() {
  loading.value = true
  try {
    const res: any = await getUserProfile(userId)
    profile.value = res.data || res
    isFollowing.value = profile.value?.isFollowing || false
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadUserPosts() {
  loadingPosts.value = true
  try {
    const res: any = await getPostList({ userId, page: postPage.value, size: pageSize })
    userPosts.value = res.data?.records || res.data || []
    totalPosts.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loadingPosts.value = false }
}

async function loadFollowing() {
  loadingFollow.value = true
  try {
    const res: any = await getFollowingList(userId)
    followingList.value = res.data || []
  } catch (e) { console.error(e) }
  finally { loadingFollow.value = false }
}

async function loadFollowers() {
  loadingFollow.value = true
  try {
    const res: any = await getFollowerList(userId)
    followerList.value = res.data || []
  } catch (e) { console.error(e) }
  finally { loadingFollow.value = false }
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
.user-profile-page {
  max-width: 960px;
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
  height: 140px;
  background: linear-gradient(135deg, #1a6b8a 0%, #2d8cb0 40%, #5bb5d5 100%);
  position: relative;
}

.cover-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(to top, rgba(0,0,0,0.15), transparent);
}

.profile-body {
  padding: 0 24px 16px;
}

.profile-top {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-top: -40px;
  position: relative;
  z-index: 1;
}

/* ══════ 头像 ══════ */
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.profile-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid var(--surface-card);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-info {
  flex: 1;
  min-width: 0;
  padding-bottom: 4px;
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--neutral-800);
  margin: 0 0 4px;
}

.profile-bio {
  font-size: 13px;
  color: var(--neutral-500);
  margin: 0 0 4px;
  line-height: 1.5;
}

.profile-id {
  font-size: 12px;
  color: var(--neutral-400);
  margin: 0;
}

.profile-action {
  padding-bottom: 4px;
}

/* ══════ 关注按钮 ══════ */
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
  min-width: 90px;
  display: flex;
  align-items: center;
  justify-content: center;

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

.btn-loading {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ══════ 统计 ══════ */
.profile-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 16px;
  padding: 14px 0;
  background: var(--neutral-25);
  border-radius: var(--radius-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex: 1;
  cursor: pointer;
  transition: color 0.2s;

  &:hover .stat-value { color: var(--primary-main); }
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
  transition: color 0.2s;
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

/* ══════ Tab 切换 ══════ */
.profile-tabs {
  display: flex;
  gap: 4px;
  margin-top: 14px;
  border-top: 1px solid var(--neutral-75);
  padding-top: 12px;

  button {
    flex: 1;
    padding: 8px 0;
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    font-size: 13px;
    font-weight: 500;
    color: var(--neutral-500);
    cursor: pointer;
    transition: all 0.2s;

    &:hover { color: var(--primary-main); background: var(--primary-soft); }
    &.active { color: var(--primary-main); background: var(--primary-soft); font-weight: 600; }
  }
}

/* ══════ 瀑布流 ══════ */
.posts-section { margin-top: 4px; }

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
  &:hover { box-shadow: var(--shadow-lg); transform: translateY(-2px); }
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

    .no-image-icon {
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

/* ══════ 用户列表 ══════ */
.user-list-section { margin-top: 4px; }

.user-list {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-75);
  overflow: hidden;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background 0.15s;

  & + .user-item { border-top: 1px solid var(--neutral-75); }
  &:hover { background: var(--neutral-25); }
}

.list-avatar {
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
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.user-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--neutral-700);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  p { font-size: 14px; color: var(--neutral-400); }
}

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
</style>
