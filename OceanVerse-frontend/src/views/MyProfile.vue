<template>
  <div class="my-profile-page">
    <!-- 个人信息 -->
    <div class="profile-header">
      <div class="profile-left">
        <!-- 头像 -->
        <div class="avatar-wrap" @click="triggerAvatarUpload">
          <div class="profile-avatar">
            <img v-if="userStore.avatarUrl" :src="userStore.avatarUrl" class="avatar-img" />
            <span v-else>{{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </div>
          <div class="avatar-overlay">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarUpload" />
        </div>
      </div>

      <div class="profile-right">
        <!-- 昵称 -->
        <div class="name-row">
          <h1 v-if="!editingNickname" class="profile-name" @click="startEditNickname">
            {{ displayNickname }}
          </h1>
          <div v-else class="nickname-edit">
            <input ref="nicknameInput" v-model="nicknameValue" class="nickname-input" maxlength="20" @keyup.enter="saveNickname" @blur="saveNickname" />
          </div>
        </div>

        <!-- 小红书号 -->
        <p class="profile-id">用户ID：{{ userStore.userId }}</p>

        <!-- 签名 -->
        <div class="bio-row">
          <p v-if="!editingBio" class="profile-bio" @click="startEditBio">
            {{ bioValue || '点击添加个人签名...' }}
          </p>
          <div v-else class="bio-edit">
            <input ref="bioInput" v-model="bioValue" class="bio-input" maxlength="100" placeholder="写一句签名..." @keyup.enter="saveBio" @blur="saveBio" />
          </div>
        </div>

        <!-- 数据统计 -->
        <div class="profile-stats">
          <span class="stat-item">
            <span class="stat-value">{{ followCount }}</span>
            <span class="stat-label">关注</span>
          </span>
          <span class="stat-item">
            <span class="stat-value">{{ followerCount }}</span>
            <span class="stat-label">粉丝</span>
          </span>
          <span class="stat-item">
            <span class="stat-value">{{ postCount }}</span>
            <span class="stat-label">获赞与收藏</span>
          </span>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="profile-tabs">
      <button :class="{ active: showTab === 'posts' }" @click="showTab = 'posts'">笔记</button>
      <button :class="{ active: showTab === 'following' }" @click="showTab = 'following'; loadFollowing()">关注</button>
      <button :class="{ active: showTab === 'followers' }" @click="showTab = 'followers'; loadFollowers()">粉丝</button>
      <button :class="{ active: showTab === 'favorites' }" @click="showTab = 'favorites'; loadFavorites()">收藏</button>
      <button :class="{ active: showTab === 'liked' }" @click="showTab = 'liked'; loadLiked()">点赞</button>
    </div>

    <!-- 我的动态 -->
    <div v-if="showTab === 'posts'" class="posts-section">
      <div v-loading="loadingPosts" class="waterfall" :class="{ 'single-col': myPosts.length === 0 }">
        <div v-if="myPosts.length === 0 && !loadingPosts" class="empty-state">
          <p>暂无笔记</p>
        </div>
        <div v-for="post in myPosts" :key="post?.id" class="feed-card" @click="$router.push(`/community/post/${post?.id}`)">
          <div v-if="parseImages(post?.imageUrls).length" class="card-image-wrap">
            <img :src="parseImages(post?.imageUrls)[0]" class="card-image" loading="lazy" />
            <span v-if="(post as any)?.status === 3" class="status-badge pending">待审核</span>
          </div>
          <div v-else class="card-image-wrap no-image">
            <span>📝</span>
          </div>
          <div class="card-body">
            <p class="card-text">{{ post?.content }}</p>
            <div class="card-footer">
              <div class="card-user">
                <div class="card-avatar">
                  {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
                </div>
                <span class="card-username">{{ userStore.username || '用户' }}</span>
              </div>
              <span class="card-like">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#ff4757" stroke="#ff4757" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                {{ post?.likeCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="totalPosts > pageSize" class="pagination-wrap">
        <button class="page-btn" :disabled="postPage <= 1" @click="postPage--; loadMyPosts()">上一页</button>
        <span class="page-info">{{ postPage }} / {{ Math.ceil(totalPosts / pageSize) }}</span>
        <button class="page-btn" :disabled="postPage >= Math.ceil(totalPosts / pageSize)" @click="postPage++; loadMyPosts()">下一页</button>
      </div>
    </div>

    <!-- 关注列表 -->
    <div v-if="showTab === 'following'" class="user-list-section">
      <div v-loading="loadingFollow" class="user-list">
        <div v-if="followingList.length === 0 && !loadingFollow" class="empty-state">
          <p>暂未关注任何人</p>
        </div>
        <div v-for="user in followingList" :key="user?.userId" class="user-item" @click="$router.push(`/community/user/${user?.userId}`)">
          <div class="list-avatar">
            <img v-if="user?.avatarUrl" :src="user?.avatarUrl" />
            <span v-else>{{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </div>
          <span class="user-name">{{ user?.username }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </div>

    <!-- 粉丝列表 -->
    <div v-if="showTab === 'followers'" class="user-list-section">
      <div v-loading="loadingFollow" class="user-list">
        <div v-if="followerList.length === 0 && !loadingFollow" class="empty-state">
          <p>暂无粉丝</p>
        </div>
        <div v-for="user in followerList" :key="user?.userId" class="user-item" @click="$router.push(`/community/user/${user?.userId}`)">
          <div class="list-avatar">
            <img v-if="user?.avatarUrl" :src="user?.avatarUrl" />
            <span v-else>{{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </div>
          <span class="user-name">{{ user?.username }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </div>

    <!-- 收藏列表 -->
    <div v-if="showTab === 'favorites'" class="posts-section">
      <div v-loading="loadingFavorites" class="waterfall" :class="{ 'single-col': favoritePosts.length === 0 }">
        <div v-if="favoritePosts.length === 0 && !loadingFavorites" class="empty-state">
          <p>暂无收藏</p>
        </div>
        <div v-for="post in favoritePosts" :key="post?.id" class="feed-card" @click="$router.push(`/community/post/${post?.id}`)">
          <div v-if="parseImages(post?.imageUrls).length" class="card-image-wrap">
            <img :src="parseImages(post?.imageUrls)[0]" class="card-image" loading="lazy" />
          </div>
          <div v-else class="card-image-wrap no-image">
            <span>📝</span>
          </div>
          <div class="card-body">
            <p class="card-text">{{ post?.content }}</p>
            <div class="card-footer">
              <div class="card-user">
                <div class="card-avatar">
                  {{ post?.username?.charAt(0)?.toUpperCase() || 'U' }}
                </div>
                <span class="card-username">{{ post?.username || '用户' }}</span>
              </div>
              <span class="card-like">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#ff4757" stroke="#ff4757" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                {{ post?.likeCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="totalFavorites > pageSize" class="pagination-wrap">
        <button class="page-btn" :disabled="favoritePage <= 1" @click="favoritePage--; loadFavorites()">上一页</button>
        <span class="page-info">{{ favoritePage }} / {{ Math.ceil(totalFavorites / pageSize) }}</span>
        <button class="page-btn" :disabled="favoritePage >= Math.ceil(totalFavorites / pageSize)" @click="favoritePage++; loadFavorites()">下一页</button>
      </div>
    </div>

    <!-- 点赞列表 -->
    <div v-if="showTab === 'liked'" class="posts-section">
      <div v-loading="loadingLiked" class="waterfall" :class="{ 'single-col': likedPosts.length === 0 }">
        <div v-if="likedPosts.length === 0 && !loadingLiked" class="empty-state">
          <p>暂无点赞</p>
        </div>
        <div v-for="post in likedPosts" :key="post?.id" class="feed-card" @click="$router.push(`/community/post/${post?.id}`)">
          <div v-if="parseImages(post?.imageUrls).length" class="card-image-wrap">
            <img :src="parseImages(post?.imageUrls)[0]" class="card-image" loading="lazy" />
          </div>
          <div v-else class="card-image-wrap no-image">
            <span>📝</span>
          </div>
          <div class="card-body">
            <p class="card-text">{{ post?.content }}</p>
            <div class="card-footer">
              <div class="card-user">
                <div class="card-avatar">
                  {{ post?.username?.charAt(0)?.toUpperCase() || 'U' }}
                </div>
                <span class="card-username">{{ post?.username || '用户' }}</span>
              </div>
              <span class="card-like">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#ff4757" stroke="#ff4757" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                {{ post?.likeCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="totalLiked > pageSize" class="pagination-wrap">
        <button class="page-btn" :disabled="likedPage <= 1" @click="likedPage--; loadLiked()">上一页</button>
        <span class="page-info">{{ likedPage }} / {{ Math.ceil(totalLiked / pageSize) }}</span>
        <button class="page-btn" :disabled="likedPage >= Math.ceil(totalLiked / pageSize)" @click="likedPage++; loadLiked()">下一页</button>
      </div>
    </div>

    <!-- 背景图裁切弹窗 -->
    <el-dialog v-model="showCropper" title="裁切背景图" width="800px" :close-on-click-modal="false">
      <div class="cropper-container">
        <img ref="cropperImage" :src="cropperImageUrl" />
      </div>
      <template #footer>
        <el-button @click="showCropper = false">取消</el-button>
        <el-button type="primary" @click="cropAndUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getPostList, getUserProfile, updateProfile, updateBio,
  getFollowingList, getFollowerList, getLikedList, getFavoriteList,
  uploadAvatar, uploadBackground
} from '@/api/community'
import type { CommunityPost } from '@/types'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

defineOptions({ name: 'MyProfile' })

const userStore = useUserStore()

const showTab = ref('posts')
const editingNickname = ref(false)
const editingBio = ref(false)
const nicknameValue = ref('')
const bioValue = ref('')
const nicknameInput = ref<HTMLInputElement>()
const bioInput = ref<HTMLInputElement>()
const originalBio = ref('')
const avatarInput = ref<HTMLInputElement>()

const backgroundUrl = ref('')
const postCount = ref(0)
const followerCount = ref(0)
const followCount = ref(0)

const myPosts = ref<CommunityPost[]>([])
const loadingPosts = ref(false)
const postPage = ref(1)
const pageSize = 20
const totalPosts = ref(0)

const loadingFollow = ref(false)
const followingList = ref<{ userId: number; username: string; avatarUrl?: string }[]>([])
const followerList = ref<{ userId: number; username: string; avatarUrl?: string }[]>([])

const loadingLiked = ref(false)
const likedPosts = ref<CommunityPost[]>([])
const likedPage = ref(1)
const totalLiked = ref(0)

const loadingFavorites = ref(false)
const favoritePosts = ref<CommunityPost[]>([])
const favoritePage = ref(1)
const totalFavorites = ref(0)

const showCropper = ref(false)
const cropperImageUrl = ref('')
const cropperImage = ref<HTMLImageElement>()
let cropper: Cropper | null = null

const displayNickname = computed(() => userStore.username || '用户')

async function loadProfile() {
  if (!userStore.userId) return
  try {
    const res: any = await getUserProfile(userStore.userId)
    const data = res.data || res
    postCount.value = data.postCount || 0
    followerCount.value = data.followerCount || 0
    followCount.value = data.followCount || 0
    backgroundUrl.value = data.backgroundUrl || ''
    bioValue.value = data.bio || ''
  } catch (e) { console.error(e) }
}

async function loadMyPosts() {
  loadingPosts.value = true
  try {
    const res: any = await getPostList({ userId: userStore.userId, page: postPage.value, size: pageSize })
    myPosts.value = (res.data?.records || res.data || []).filter(Boolean)
    totalPosts.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loadingPosts.value = false }
}

async function loadFollowing() {
  loadingFollow.value = true
  try {
    const res: any = await getFollowingList()
    followingList.value = (res.data || []).filter(Boolean)
  } catch (e) { console.error(e) }
  finally { loadingFollow.value = false }
}

async function loadFollowers() {
  loadingFollow.value = true
  try {
    const res: any = await getFollowerList()
    followerList.value = (res.data || []).filter(Boolean)
  } catch (e) { console.error(e) }
  finally { loadingFollow.value = false }
}

async function loadLiked() {
  loadingLiked.value = true
  try {
    const res: any = await getLikedList({ page: likedPage.value, size: pageSize })
    likedPosts.value = (res.data?.records || res.data || []).filter(Boolean)
    totalLiked.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loadingLiked.value = false }
}

async function loadFavorites() {
  loadingFavorites.value = true
  try {
    const res: any = await getFavoriteList({ targetType: 'POST', page: favoritePage.value, size: pageSize })
    favoritePosts.value = (res.data?.records || res.data || []).filter(Boolean)
    totalFavorites.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loadingFavorites.value = false }
}

function triggerAvatarUpload() { avatarInput.value?.click() }
async function handleAvatarUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/') || file.size > 5 * 1024 * 1024) {
    ElMessage.error('请上传 5MB 以内的图片'); return
  }
  try {
    const res: any = await uploadAvatar(file)
    userStore.avatarUrl = res.data || res
    ElMessage.success('头像已更新')
  } catch (e) { console.error(e) }
  if (avatarInput.value) avatarInput.value.value = ''
}

function triggerBgUpload() { /* bgInput.value?.click() */ }
async function handleBgUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) {
    ElMessage.error('请上传 10MB 以内的图片'); return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    cropperImageUrl.value = event.target?.result as string
    showCropper.value = true
    nextTick(() => {
      if (cropperImage.value) {
        cropper = new Cropper(cropperImage.value, {
          aspectRatio: 3 / 1,
          viewMode: 1,
          dragMode: 'move',
          autoCropArea: 1,
          restore: false,
          guides: false,
          center: false,
          highlight: false,
          cropBoxMovable: true,
          cropBoxResizable: true,
          toggleDragModeOnDblclick: false,
        })
      }
    })
  }
  reader.readAsDataURL(file)
}

async function cropAndUpload() {
  if (!cropper) return
  const canvas = cropper.getCroppedCanvas({ width: 1200, height: 400, fillColor: '#fff', imageSmoothingEnabled: true, imageSmoothingQuality: 'high' })
  canvas.toBlob(async (blob) => {
    if (!blob) return
    const file = new File([blob], 'background.jpg', { type: 'image/jpeg' })
    try {
      const res: any = await uploadBackground(file)
      backgroundUrl.value = res.data || res
      ElMessage.success('背景图已更新')
    } catch (e) { console.error(e) }
    showCropper.value = false
    cropper?.destroy()
    cropper = null
  }, 'image/jpeg', 0.9)
}

function startEditNickname() {
  nicknameValue.value = userStore.username || ''
  editingNickname.value = true
  nextTick(() => nicknameInput.value?.focus())
}
async function saveNickname() {
  editingNickname.value = false
  const val = nicknameValue.value.trim()
  if (!val || val === userStore.username) return
  try {
    await updateProfile({ nickname: val })
    userStore.username = val
    ElMessage.success('昵称已更新')
  } catch (e) { console.error(e) }
}

function startEditBio() {
  originalBio.value = bioValue.value || ''
  editingBio.value = true
  nextTick(() => bioInput.value?.focus())
}
async function saveBio() {
  editingBio.value = false
  const val = bioValue.value.trim()
  if (val === originalBio.value) return
  try {
    await updateBio({ bio: val })
    ElMessage.success('签名已更新')
  } catch (e) { console.error(e) }
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

onMounted(() => {
  loadProfile()
  loadMyPosts()
})
</script>

<style scoped lang="scss">
.my-profile-page {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

/* ══════ 个人信息头部 ══════ */
.profile-header {
  display: flex;
  gap: 40px;
  padding: 40px 0 32px;
  border-bottom: 1px solid var(--neutral-100);
  margin-bottom: 0;
}

.profile-left {
  flex-shrink: 0;
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
  width: 100px;
  height: 100px;

  &:hover .avatar-overlay {
    opacity: 1;
  }
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 36px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.profile-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.name-row {
  margin-bottom: 6px;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--neutral-800);
  margin: 0;
  cursor: pointer;

  &:hover {
    color: var(--primary-main);
  }
}

.nickname-edit {
  display: inline-flex;
}

.nickname-input {
  font-size: 22px;
  font-weight: 700;
  color: var(--neutral-800);
  border: none;
  border-bottom: 2px solid var(--primary-main);
  outline: none;
  background: none;
  padding: 2px 0;
  font-family: inherit;
  width: 200px;
}

.profile-id {
  font-size: 13px;
  color: var(--neutral-400);
  margin: 0 0 6px;
}

.bio-row {
  margin-bottom: 12px;
}

.profile-bio {
  font-size: 14px;
  color: var(--neutral-600);
  margin: 0;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  transition: background 0.15s;

  &:hover {
    background: var(--neutral-75);
  }
}

.bio-input {
  font-size: 14px;
  color: var(--neutral-700);
  border: none;
  border-bottom: 1.5px solid var(--primary-lighter);
  outline: none;
  background: none;
  padding: 2px 6px;
  font-family: inherit;
  width: 260px;
}

.profile-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
  cursor: default;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--neutral-800);
}

.stat-label {
  font-size: 13px;
  color: var(--neutral-500);
}

/* ══════ Tab 切换 ══════ */
.profile-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--neutral-100);
  margin-bottom: 16px;

  button {
    padding: 14px 24px;
    border: none;
    border-radius: 0;
    background: none;
    font-size: 15px;
    font-weight: 500;
    color: var(--neutral-500);
    cursor: pointer;
    position: relative;
    transition: color 0.2s;

    &::after {
      content: '';
      position: absolute;
      bottom: -1px;
      left: 50%;
      transform: translateX(-50%) scaleX(0);
      width: 24px;
      height: 2px;
      background: var(--neutral-800);
      border-radius: 1px;
      transition: transform 0.2s;
    }

    &:hover {
      color: var(--neutral-700);
    }

    &.active {
      color: var(--neutral-800);
      font-weight: 600;

      &::after {
        transform: translateX(-50%) scaleX(1);
      }
    }
  }
}

/* ══════ 瀑布流 ══════ */
.posts-section { margin-top: 0; }

.waterfall {
  columns: 4;
  column-gap: 12px;

  &.single-col {
    columns: 1;
  }
}

@media (max-width: 1200px) {
  .waterfall { columns: 3; }
}
@media (max-width: 900px) {
  .waterfall { columns: 2; }
}
@media (max-width: 600px) {
  .waterfall { columns: 2; column-gap: 8px; }
}

.feed-card {
  break-inside: avoid;
  margin-bottom: 12px;
  background: var(--surface-card);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
  border: none;
  display: inline-block;
  width: 100%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
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
    background: linear-gradient(135deg, #f0f7fa 0%, #e8f4f8 100%);
    span { font-size: 32px; opacity: 0.4; }
  }
}

.card-image {
  width: 100%;
  display: block;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  backdrop-filter: blur(8px);
  &.pending { background: rgba(204, 138, 48, 0.85); color: #fff; }
}

.card-body { padding: 10px 12px 8px; }

.card-text {
  font-size: 13px;
  line-height: 1.6;
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
}

.card-user {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.card-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--gradient-ocean);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-username {
  font-size: 12px;
  font-weight: 500;
  color: var(--neutral-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.card-like {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--neutral-400);
  flex-shrink: 0;
}

/* ══════ 用户列表 ══════ */
.user-list-section { margin-top: 0; }

.user-list {
  background: var(--surface-card);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  cursor: pointer;
  transition: background 0.15s;

  & + .user-item { border-top: 1px solid var(--neutral-75); }
  &:hover { background: var(--neutral-25); }
}

.list-avatar {
  width: 42px;
  height: 42px;
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

/* ══════ 空状态 ══════ */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  columns: 1;

  p {
    font-size: 14px;
    color: var(--neutral-400);
  }
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

/* ══════ 裁切弹窗 ══════ */
.cropper-container {
  max-height: 400px;
  overflow: hidden;

  img {
    max-width: 100%;
    max-height: 400px;
  }
}

/* ══════ 响应式 ══════ */
@media (max-width: 600px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    gap: 20px;
    padding: 24px 0;
    text-align: center;
  }

  .profile-stats {
    justify-content: center;
  }

  .profile-tabs button {
    padding: 12px 16px;
    font-size: 14px;
  }
}
</style>
