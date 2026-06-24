<template>
  <div class="my-profile-page">
    <!-- 个人信息卡片 -->
    <div class="profile-card">
      <!-- 背景图 -->
      <div class="profile-cover" :style="bgStyle" @click="triggerBgUpload">
        <div class="cover-overlay"></div>
        <div class="cover-edit">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
          更换背景
        </div>
        <input ref="bgInput" type="file" accept="image/*" style="display:none" @change="handleBgUpload" />
      </div>

      <div class="profile-body">
        <div class="profile-top">
          <!-- 头像 -->
          <div class="avatar-wrap">
            <div class="profile-avatar" @click="triggerAvatarUpload">
              <img v-if="userStore.avatarUrl" :src="userStore.avatarUrl" class="avatar-img" />
              <span v-else>{{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
            </div>
            <label class="avatar-edit" @click.stop="triggerAvatarUpload">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
            </label>
            <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarUpload" />
          </div>

          <div class="profile-info">
            <!-- 昵称 -->
            <div class="name-row">
              <h2 v-if="!editingNickname" class="profile-name" @click="startEditNickname">
                {{ displayNickname }}
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2" stroke-linecap="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              </h2>
              <div v-else class="nickname-edit">
                <input ref="nicknameInput" v-model="nicknameValue" class="nickname-input" maxlength="20" @keyup.enter="saveNickname" @blur="saveNickname" />
              </div>
            </div>

            <!-- 签名 -->
            <div class="bio-row">
              <p v-if="!editingBio" class="profile-bio" @click="startEditBio">
                {{ bioValue || '点击添加个人签名...' }}
              </p>
              <div v-else class="bio-edit">
                <input ref="bioInput" v-model="bioValue" class="bio-input" maxlength="100" placeholder="写一句签名..." @keyup.enter="saveBio" @blur="saveBio" />
              </div>
            </div>

            <p class="profile-id">ID: {{ userStore.userId }}</p>
          </div>
        </div>

        <!-- 数据统计 -->
        <div class="profile-stats">
          <div class="stat-item" @click="showTab = 'posts'">
            <span class="stat-value">{{ postCount }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="showTab = 'followers'">
            <span class="stat-value">{{ followerCount }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="showTab = 'following'">
            <span class="stat-value">{{ followCount }}</span>
            <span class="stat-label">关注</span>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="profile-tabs">
          <button :class="{ active: showTab === 'posts' }" @click="showTab = 'posts'">我的动态</button>
          <button :class="{ active: showTab === 'following' }" @click="showTab = 'following'; loadFollowing()">关注</button>
          <button :class="{ active: showTab === 'followers' }" @click="showTab = 'followers'; loadFollowers()">粉丝</button>
          <button :class="{ active: showTab === 'liked' }" @click="showTab = 'liked'; loadLiked()">点赞</button>
          <button :class="{ active: showTab === 'favorites' }" @click="showTab = 'favorites'; loadFavorites()">收藏</button>
        </div>
      </div>
    </div>

    <!-- 我的动态 -->
    <div v-if="showTab === 'posts'" class="posts-section">
      <div v-loading="loadingPosts" class="waterfall">
        <div v-if="myPosts.length === 0 && !loadingPosts" class="empty-state">
          <p>暂无动态</p>
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
              <span class="card-time">{{ formatTime(post?.createTime) }}</span>
              <div class="card-stats">
                <span><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg> {{ post?.likeCount }}</span>
                <span><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> {{ post?.commentCount }}</span>
              </div>
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
        <div v-for="user in followerList" :key="user?.userId" class="user-item" @click="$router.push(`/community/user/${user?.userId}`)">
          <div class="list-avatar">
            <img v-if="user?.avatarUrl" :src="user?.avatarUrl" />
            <span v-else>{{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </div>
          <span class="user-name">{{ user?.username }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </div>

    <!-- 点赞列表 -->
    <div v-if="showTab === 'liked'" class="posts-section">
      <div v-loading="loadingLiked" class="waterfall">
        <div v-if="likedPosts.length === 0 && !loadingLiked" class="empty-state">
          <p>暂无点赞记录</p>
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
              <span class="card-time">{{ formatTime(post?.createTime) }}</span>
              <div class="card-stats">
                <span><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg> {{ post?.likeCount }}</span>
                <span><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> {{ post?.commentCount }}</span>
              </div>
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

    <!-- 收藏列表 -->
    <div v-if="showTab === 'favorites'" class="posts-section">
      <div v-loading="loadingFavorites" class="waterfall">
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
              <span class="card-time">{{ formatTime(post?.createTime) }}</span>
              <div class="card-stats">
                <span><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg> {{ post?.likeCount }}</span>
                <span><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#948f86" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> {{ post?.commentCount }}</span>
              </div>
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
const avatarInput = ref<HTMLInputElement>()
const bgInput = ref<HTMLInputElement>()

const backgroundUrl = ref('')
const postCount = ref(0)
const followerCount = ref(0)
const followCount = ref(0)

const myPosts = ref<CommunityPost[]>([])
const loadingPosts = ref(false)
const postPage = ref(1)
const pageSize = 12
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
const bgStyle = computed(() => {
  if (backgroundUrl.value) {
    return { backgroundImage: `url(${backgroundUrl.value})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
})

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

// ── 头像 ──
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

// ── 背景图 ──
function triggerBgUpload() { bgInput.value?.click() }
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
  if (bgInput.value) bgInput.value.value = ''
}

async function cropAndUpload() {
  if (!cropper) return
  const canvas = cropper.getCroppedCanvas({
    width: 1200,
    height: 400,
    fillColor: '#fff',
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high',
  })

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

// ── 昵称 ──
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

// ── 签名 ──
function startEditBio() {
  editingBio.value = true
  nextTick(() => bioInput.value?.focus())
}
async function saveBio() {
  editingBio.value = false
  try {
    await updateBio({ bio: bioValue.value.trim() })
    ElMessage.success('签名已更新')
  } catch (e) { console.error(e) }
}

// ── 工具 ──
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
  max-width: 960px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
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
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover .cover-overlay { opacity: 1; }
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.3);
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-edit {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;

  .profile-cover:hover & { opacity: 1; }
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
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.2s;

  &:hover { transform: scale(1.03); }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary-main);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: background 0.2s;

  &:hover { background: var(--primary-light); }
}

.profile-info {
  flex: 1;
  min-width: 0;
  padding-bottom: 4px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--neutral-800);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;

  &:hover { color: var(--primary-main); }
}

.nickname-edit {
  display: flex;
  align-items: center;
}

.nickname-input {
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
  border: none;
  border-bottom: 2px solid var(--primary-main);
  outline: none;
  background: none;
  padding: 2px 0;
  font-family: inherit;
  width: 180px;
}

/* ══════ 签名 ══════ */
.bio-row {
  margin-top: 4px;
}

.profile-bio {
  font-size: 13px;
  color: var(--neutral-500);
  margin: 0;
  cursor: pointer;
  padding: 3px 8px;
  border-radius: var(--radius-xs);
  transition: all 0.15s;
  display: inline-block;

  &:hover {
    background: var(--neutral-75);
    color: var(--neutral-600);
  }
}

.bio-input {
  font-size: 13px;
  color: var(--neutral-700);
  border: none;
  border-bottom: 1.5px solid var(--primary-lighter);
  outline: none;
  background: none;
  padding: 3px 8px;
  font-family: inherit;
  width: 260px;
}

.profile-id {
  font-size: 12px;
  color: var(--neutral-400);
  margin-top: 4px;
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
    span { font-size: 32px; opacity: 0.4; }
  }
}

.card-image {
  width: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.4s var(--ease-out);
  .feed-card:hover & { transform: scale(1.03); }
}

.status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  backdrop-filter: blur(8px);
  &.pending { background: rgba(204,138,48,0.85); color: #fff; }
}

.card-body { padding: 10px 12px 8px; }

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

.card-time { font-size: 11px; color: var(--neutral-400); }

.card-stats {
  display: flex;
  gap: 10px;
  span { display: flex; align-items: center; gap: 3px; font-size: 12px; color: var(--neutral-400); }
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

/* ══════ 裁切弹窗 ══════ */
.cropper-container {
  max-height: 400px;
  overflow: hidden;

  img {
    max-width: 100%;
    max-height: 400px;
  }
}
</style>
