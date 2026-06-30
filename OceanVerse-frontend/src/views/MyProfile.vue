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

          <!-- 设置按钮 -->
          <button class="settings-btn" @click="showSettings = true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          </button>
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
      <button :class="{ active: showTab === 'rejected' }" @click="showTab = 'rejected'; loadRejectedPosts()">未通过</button>
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

    <!-- 未通过的帖子 -->
    <div v-if="showTab === 'rejected'" class="posts-section">
      <div v-loading="loadingRejected" class="waterfall" :class="{ 'single-col': rejectedPosts.length === 0 }">
        <div v-if="rejectedPosts.length === 0 && !loadingRejected" class="empty-state">
          <p>暂无未通过的帖子</p>
        </div>
        <div v-for="post in rejectedPosts" :key="post?.id" class="feed-card">
          <div v-if="parseImages(post?.imageUrls).length" class="card-image-wrap" @click="$router.push(`/community/post/${post?.id}`)">
            <img :src="parseImages(post?.imageUrls)[0]" class="card-image" loading="lazy" />
            <span class="status-badge rejected">未通过</span>
          </div>
          <div v-else class="card-image-wrap no-image" @click="$router.push(`/community/post/${post?.id}`)">
            <span>📝</span>
            <span class="status-badge rejected">未通过</span>
          </div>
          <div class="card-body" @click="$router.push(`/community/post/${post?.id}`)">
            <p class="card-text">{{ post?.content }}</p>
            <div class="card-footer">
              <span class="card-time">{{ formatTime(post?.createTime) }}</span>
            </div>
          </div>
          <button class="edit-btn" @click.stop="openEditDialog(post)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            编辑重提
          </button>
        </div>
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

    <!-- 编辑帖子弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑帖子" width="600px" :close-on-click-modal="false">
      <el-form v-if="editingPost">
        <el-form-item label="内容">
          <el-input v-model="editContent" type="textarea" :rows="5" placeholder="请输入帖子内容" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="edit-images">
            <div v-for="(img, idx) in editImages" :key="idx" class="edit-image-item">
              <img :src="img" />
              <el-icon class="remove-btn" @click="editImages.splice(idx, 1)"><Close /></el-icon>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">重新提交</el-button>
      </template>
    </el-dialog>

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

    <!-- 设置抽屉 -->
    <el-drawer v-model="showSettings" title="账号设置" size="420px" direction="rtl" :close-on-click-modal="true">
      <div class="settings-body">
        <!-- 编辑资料 -->
        <h4 class="settings-title">编辑资料</h4>
        <el-form ref="settingsFormRef" :model="settingsForm" label-position="top" class="settings-form">
          <el-form-item label="真实姓名">
            <el-input v-model="settingsForm.realName" placeholder="请输入真实姓名" maxlength="50" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">
              {{ profileSaved ? '✓ 已保存' : '保存修改' }}
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <!-- 修改密码 -->
        <h4 class="settings-title">修改密码</h4>
        <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="settings-form">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" :class="['strength-input', strengthClass]" />
            <div v-if="pwdForm.newPassword" class="pwd-strength">
              <div class="strength-bar"><div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent }" /></div>
              <span class="strength-label" :class="strengthClass">{{ strengthLabel }}</span>
              <div v-show="showAllChecks" class="strength-checks">
                <span :class="pwChecks.minLength ? 'pass' : 'fail'">{{ pwChecks.minLength ? '✓' : '✗' }} 至少8位</span>
                <span :class="pwChecks.hasUpper ? 'pass' : 'fail'">{{ pwChecks.hasUpper ? '✓' : '✗' }} 大写字母</span>
                <span :class="pwChecks.hasLower ? 'pass' : 'fail'">{{ pwChecks.hasLower ? '✓' : '✗' }} 小写字母</span>
                <span :class="pwChecks.hasDigit ? 'pass' : 'fail'">{{ pwChecks.hasDigit ? '✓' : '✗' }} 数字</span>
                <span :class="pwChecks.hasSpecial ? 'pass' : 'fail'">{{ pwChecks.hasSpecial ? '✓' : '✗' }} 特殊字符</span>
                <span :class="pwChecks.enoughTypes ? 'pass' : 'fail'">{{ pwChecks.enoughTypes ? '✓' : '✗' }} 至少3种</span>
              </div>
              <button type="button" class="toggle-checks" @click="showAllChecks = !showAllChecks">{{ showAllChecks ? '收起 ▲' : '展开规则 ▼' }}</button>
            </div>
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            <div v-if="pwdForm.confirmPassword && pwdForm.confirmPassword === pwdForm.newPassword" class="match-hint success">✓ 两次密码一致</div>
            <div v-else-if="pwdForm.confirmPassword && pwdForm.confirmPassword !== pwdForm.newPassword" class="match-hint error">✗ 两次密码不一致</div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <!-- 危险操作 -->
        <div class="drawer-danger">
          <div class="drawer-danger-bar" />
          <div class="drawer-danger-body">
            <p><strong>注销账号</strong></p>
            <p class="danger-desc">注销后无法登录，数据保留30天后彻底删除。</p>
            <el-popconfirm title="确定要注销账号吗？" confirm-button-text="确认注销" cancel-button-text="取消" @confirm="handleDeleteAccount">
              <template #reference>
                <el-button type="danger" size="small" :loading="deletingAccount">注销账号</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getPostList, getUserProfile, updateProfile, updateBio,
  getFollowingList, getFollowerList, getLikedList, getFavoriteList,
  uploadAvatar, uploadBackground, updatePost
} from '@/api/community'
import { updatePassword, deleteAccount, updateProfile as updateUserProfile } from '@/api/user'
import type { CommunityPost } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

defineOptions({ name: 'MyProfile' })

const router = useRouter()
const userStore = useUserStore()

// ── Profile ──
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

const rejectedPosts = ref<CommunityPost[]>([])
const loadingRejected = ref(false)

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

// ── Settings drawer ──
const showSettings = ref(false)
const settingsFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const savingProfile = ref(false)
const profileSaved = ref(false)
const changingPwd = ref(false)
const deletingAccount = ref(false)
const showAllChecks = ref(false)

const settingsForm = reactive({ realName: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_r, v, cb) => cb(v !== pwdForm.newPassword ? new Error('两次密码不一致') : undefined), trigger: 'blur' }
  ]
}

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
    const allPosts = (res.data?.records || res.data || []).filter(Boolean)
    myPosts.value = allPosts.filter((p: any) => p.status !== 4 && p.status !== 0)
    totalPosts.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loadingPosts.value = false }
}

async function loadRejectedPosts() {
  loadingRejected.value = true
  try {
    const res: any = await getPostList({ userId: userStore.userId, page: 1, size: 100 })
    const allPosts = (res.data?.records || res.data || []).filter(Boolean)
    rejectedPosts.value = allPosts.filter((p: any) => p.status === 4 || p.status === 0)
  } catch (e) { console.error(e) }
  finally { loadingRejected.value = false }
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

// 编辑帖子
const showEditDialog = ref(false)
const editingPost = ref<CommunityPost | null>(null)
const editContent = ref('')
const editImages = ref<string[]>([])
const editLoading = ref(false)

function openEditDialog(post: CommunityPost) {
  editingPost.value = post
  editContent.value = post.content || ''
  editImages.value = parseImages(post.imageUrls)
  showEditDialog.value = true
}

async function handleEditSubmit() {
  if (!editingPost.value) return
  if (!editContent.value.trim()) {
    ElMessage.warning('请输入帖子内容')
    return
  }
  editLoading.value = true
  try {
    await updatePost(editingPost.value.id, {
      content: editContent.value.trim(),
      postType: editingPost.value.postType || 'NORMAL',
      imageUrls: JSON.stringify(editImages.value)
    })
    ElMessage.success('已重新提交审核')
    showEditDialog.value = false
    loadMyPosts()
    loadRejectedPosts()
  } catch (e) { console.error(e) }
  finally { editLoading.value = false }
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

// ══════ 密码强度（与 Profile 一致） ══════
const UPPER = /[A-Z]/
const LOWER = /[a-z]/
const DIGIT = /[0-9]/
const SPECIAL = /[!@#\$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/
const CONSECUTIVE = /(?:abcdef|bcdefg|cdefgh|defghi|efghij|fghijk|ghijkl|hijklm|ijklmn|jklmno|klmnop|lmnopq|mnopqr|nopqrs|opqrst|pqrstu|qrstuv|rstuvw|stuvwx|tuvwxy|uvwxyz|123456|234567|345678|456789|567890|654321|765432|876543|987654)/i
const REPEATED = /(.)\1{3,}/
const WEAK_DICT = new Set(['123456','password','123456789','12345678','12345','1234567890','1234567','qwerty','abc123','111111','123123','admin','password1','iloveyou','welcome','monkey','dragon','master','football','baseball','sunshine','princess','1234','123','000000','666666','888888','qwerty123','1q2w3e4r','passw0rd'])

const pwChecks = computed(() => {
  const pwd = pwdForm.newPassword
  if (!pwd) return { minLength: false, hasUpper: false, hasLower: false, hasDigit: false, hasSpecial: false, enoughTypes: false }
  const hasUpper = UPPER.test(pwd), hasLower = LOWER.test(pwd), hasDigit = DIGIT.test(pwd), hasSpecial = SPECIAL.test(pwd)
  const typeCount = [hasUpper, hasLower, hasDigit, hasSpecial].filter(Boolean).length
  return { minLength: pwd.length >= 8, hasUpper, hasLower, hasDigit, hasSpecial, enoughTypes: typeCount >= 3 }
})

const strengthLabel = computed(() => {
  const pwd = pwdForm.newPassword
  if (!pwd || pwd.length < 3) return ''
  const tc = [pwChecks.value.hasUpper, pwChecks.value.hasLower, pwChecks.value.hasDigit, pwChecks.value.hasSpecial].filter(Boolean).length
  if (!pwChecks.value.minLength || tc < 2) return '弱'
  if (pwChecks.value.minLength && tc >= 4 && pwd.length >= 12) return '强'
  if (pwChecks.value.enoughTypes) return '中'
  return '弱'
})

const strengthClass = computed(() => {
  const m: Record<string, string> = { '强': 'strong', '中': 'medium', '弱': 'weak' }
  return m[strengthLabel.value] || ''
})
const strengthPercent = computed(() => {
  const m: Record<string, string> = { '强': '100%', '中': '66%', '弱': '33%' }
  return m[strengthLabel.value] || '0%'
})

// ══════ Settings actions ══════
async function handleSaveProfile() {
  savingProfile.value = true
  try {
    await updateUserProfile({ realName: settingsForm.realName })
    userStore.setUserInfo({ realName: settingsForm.realName })
    profileSaved.value = true
    setTimeout(() => { profileSaved.value = false }, 2000)
    ElMessage.success('资料已更新')
  } catch { /* http interceptor handles */ }
  finally { savingProfile.value = false }
}

async function handleChangePassword() {
  await pwdFormRef.value?.validate()
  changingPwd.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''
    showAllChecks.value = false
  } catch { /* http interceptor handles */ }
  finally { changingPwd.value = false }
}

async function handleDeleteAccount() {
  deletingAccount.value = true
  try {
    await deleteAccount()
    ElMessage.success('账号已注销')
    userStore.logout()
    router.push('/login')
  } finally { deletingAccount.value = false }
}

onMounted(() => {
  loadProfile()
  loadMyPosts()
  settingsForm.realName = userStore.realName || ''
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
  &.rejected { background: rgba(231, 76, 60, 0.85); color: #fff; }
}

.edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: calc(100% - 24px);
  margin: 0 12px 12px;
  padding: 8px 0;
  border: 1px solid var(--primary-main);
  border-radius: 6px;
  background: transparent;
  color: var(--primary-main);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--primary-soft);
  }
}

/* 编辑图片 */
.edit-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edit-image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .remove-btn {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 20px;
    height: 20px;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 12px;
  }
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

.card-time {
  font-size: 11px;
  color: var(--neutral-400);
  flex-shrink: 0;
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

/* ══════ 设置按钮 ══════ */
.settings-btn {
  margin-left: auto;
  align-self: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--neutral-50);
  color: var(--neutral-400);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;

  &:hover {
    background: var(--primary-soft);
    color: var(--primary-main);
  }
}

/* ══════ 设置抽屉 ══════ */
.settings-body {
  padding: 0 8px;
}

.settings-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--neutral-700);
  margin: 0 0 16px;
}

.settings-form {
  :deep(.el-form-item) { margin-bottom: 16px; }
}

/* ══════ 密码强度（抽屉内） ══════ */
.strength-input {
  :deep(.el-input__wrapper) { transition: box-shadow 0.3s ease; }
  &.weak :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #f56c6c inset !important; }
  &.medium :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #e6a23c inset !important; }
  &.strong :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #67c23a inset !important; }
}

.pwd-strength { margin-top: 6px; }

.strength-bar {
  height: 4px;
  background: var(--neutral-100);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
  &.weak { background: #f56c6c; }
  &.medium { background: #e6a23c; }
  &.strong { background: #67c23a; }
}

.strength-label {
  font-size: 12px;
  font-weight: 600;
  &.weak { color: #f56c6c; }
  &.medium { color: #e6a23c; }
  &.strong { color: #67c23a; }
}

.strength-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 3px 10px;
  margin: 4px 0;
  span { font-size: 11px; }
  .pass { color: #67c23a; }
  .fail { color: var(--neutral-400); }
}

.toggle-checks {
  border: none;
  background: none;
  font-size: 11px;
  color: var(--primary-main);
  cursor: pointer;
  padding: 0;
  &:hover { text-decoration: underline; }
}

.match-hint {
  font-size: 12px;
  margin-top: 4px;
  &.success { color: var(--el-color-success); }
  &.error { color: var(--el-color-danger); }
}

/* ══════ 危险操作（抽屉内） ══════ */
.drawer-danger {
  display: flex;
  padding: 12px 0;
}

.drawer-danger-bar {
  width: 4px;
  border-radius: 2px;
  background: var(--el-color-danger);
  flex-shrink: 0;
  margin-right: 12px;
}

.drawer-danger-body {
  flex: 1;
  p { margin: 0 0 8px; font-size: 13px; }
  .danger-desc { color: var(--neutral-400); margin-bottom: 10px; }
}

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
