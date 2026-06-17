<template>
  <div v-loading="loading" class="user-profile-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">用户主页</span>
      </template>
    </el-page-header>

    <div v-if="profile" class="profile-card">
      <div class="profile-header">
        <el-avatar :size="72" class="profile-avatar">
          {{ profile.username?.charAt(0)?.toUpperCase() || 'U' }}
        </el-avatar>
        <div class="profile-info">
          <h2 class="profile-name">{{ profile.username }}</h2>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ profile.postCount }}</span>
              <span class="stat-label">帖子</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ profile.followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ profile.followCount }}</span>
              <span class="stat-label">关注</span>
            </div>
          </div>
        </div>
        <div v-if="!isSelf" class="profile-action">
          <el-button
            :type="isFollowing ? 'default' : 'primary'"
            :loading="followLoading"
            @click="handleFollow"
          >
            {{ isFollowing ? '已关注' : '+ 关注' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 用户帖子列表 -->
    <div class="user-posts">
      <h3 class="section-title">TA 的动态</h3>
      <div v-loading="loadingPosts" class="post-list">
        <div v-if="userPosts.length === 0 && !loadingPosts" class="empty-state">
          <el-empty description="暂无动态" />
        </div>
        <div v-for="post in userPosts" :key="post.id" class="post-card" @click="goToPost(post.id)">
          <div class="post-content">
            <p>{{ post.content }}</p>
          </div>
          <div v-if="parseImages(post.imageUrls).length" class="post-images">
            <el-image
              v-for="(img, idx) in parseImages(post.imageUrls).slice(0, 3)"
              :key="idx"
              :src="img"
              fit="cover"
              class="post-image"
            />
          </div>
          <div class="post-footer">
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
            <div class="post-stats">
              <span><el-icon><Star /></el-icon> {{ post.likeCount }}</span>
              <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
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
import { Star, ChatDotRound } from '@element-plus/icons-vue'
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
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadUserPosts() {
  loadingPosts.value = true
  try {
    const res: any = await getPostList({ userId, page: 1, size: 50 })
    userPosts.value = res.data?.records || res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loadingPosts.value = false
  }
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
  } catch (e) {
    console.error(e)
  } finally {
    followLoading.value = false
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

/* ══════ 用户信息卡片 ══════ */
.profile-card {
  background: var(--surface-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);
  padding: 16px;
  margin-bottom: 12px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  background: var(--gradient-ocean);
  color: #fff;
  font-weight: 700;
  font-size: 22px;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: 6px;
}

.profile-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--neutral-800);
}

.stat-label {
  font-size: 12px;
  color: var(--neutral-400);
}

.profile-action {
  flex-shrink: 0;
}

/* ══════ 帖子列表 ══════ */
.user-posts {
  margin-top: 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);
  margin-bottom: 8px;
}

.post-card {
  background: var(--surface-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);
  padding: 12px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-1px);
  }
}

.post-content {
  margin-bottom: 6px;

  p {
    font-size: 13px;
    line-height: 1.5;
    color: var(--neutral-700);
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.post-images {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}

.post-image {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-xs);
  flex-shrink: 0;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.post-time {
  font-size: 11px;
  color: var(--neutral-400);
}

.post-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--neutral-400);

  span {
    display: flex;
    align-items: center;
    gap: 3px;
  }
}

.empty-state {
  padding: 20px 0;
}
</style>
