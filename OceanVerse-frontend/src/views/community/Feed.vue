<template>
  <div class="feed-page">
    <!-- 顶部操作栏 -->
    <div class="feed-header">
      <div class="feed-header-left">
        <h2 class="page-title">动态广场</h2>
        <p class="page-desc">社区用户的海洋探索分享</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 发布动态
      </el-button>
    </div>

    <!-- 筛选标签 -->
    <div class="feed-filters">
      <el-radio-group v-model="currentFilter" @change="handleFilterChange" size="default">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="NORMAL">日常</el-radio-button>
        <el-radio-button value="OBSERVATION">观测</el-radio-button>
        <el-radio-button value="RECOGNITION">识别</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 帖子列表 -->
    <div v-loading="loading" class="post-list">
      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无动态，快来发布第一条吧！" />
      </div>

      <div v-for="post in posts" :key="post.id" class="post-card">
        <!-- 用户信息 -->
        <div class="post-header">
          <div class="user-info">
            <el-avatar :size="40" class="user-avatar">
              {{ post.username?.charAt(0)?.toUpperCase() || 'U' }}
            </el-avatar>
            <div class="user-meta">
              <span class="username" @click.stop="goToUser(post.userId)">{{ post.username || '用户' }}</span>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <div class="post-header-right">
            <el-tag
              v-if="post.postType !== 'NORMAL'"
              :type="postTypeTag(post.postType)"
              size="small"
              effect="light"
            >
              {{ postTypeLabel(post.postType) }}
            </el-tag>
            <el-popconfirm
              v-if="post.userId === userStore.userId"
              title="确定删除这条帖子？"
              @confirm="handleDeletePost(post)"
            >
              <template #reference>
                <button class="post-delete-btn">删除</button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <!-- 内容 -->
        <div class="post-content" @click="goToDetail(post.id)">
          <p>{{ post.content }}</p>
        </div>

        <!-- 图片 -->
        <div v-if="post.parsedImages?.length" class="post-images">
          <el-image
            v-for="(img, idx) in post.parsedImages"
            :key="idx"
            :src="img"
            :preview-src-list="post.parsedImages"
            :initial-index="idx"
            fit="cover"
            class="post-image"
            :class="{ 'single-image': post.parsedImages.length === 1 }"
          />
        </div>

        <!-- 操作栏 -->
        <div class="post-actions">
          <button
            class="action-btn"
            :class="{ active: post.isLiked }"
            @click="handleLike(post)"
          >
            <el-icon><Star /></el-icon>
            <span>{{ post.likeCount || 0 }}</span>
          </button>
          <button class="action-btn" @click="goToDetail(post.id)">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ post.commentCount || 0 }}</span>
          </button>
          <button
            class="action-btn"
            :class="{ active: post.isFavorited }"
            @click="handleFavorite(post)"
          >
            <el-icon><StarFilled /></el-icon>
            <span>{{ post.favoriteCount || 0 }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadPosts"
      />
    </div>

    <!-- 发帖弹窗 -->
    <el-dialog
      v-model="showCreateDialog"
      title="发布动态"
      width="540px"
      :close-on-click-modal="false"
      @closed="resetCreateForm"
    >
      <el-form :model="createForm" label-position="top">
        <el-form-item label="动态内容" required>
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的海洋探索故事..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="动态类型">
          <el-select v-model="createForm.postType" placeholder="选择类型" style="width: 100%">
            <el-option label="日常分享" value="NORMAL" />
            <el-option label="观测记录" value="OBSERVATION" />
            <el-option label="物种识别" value="RECOGNITION" />
          </el-select>
        </el-form-item>
        <el-form-item label="添加图片">
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            list-type="picture-card"
            :limit="9"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <p class="upload-tip">最多 9 张，单张不超过 10MB</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Star, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getPostList,
  createPost,
  deletePost,
  toggleLike,
  toggleFavorite,
} from '@/api/community'
import type { CommunityPost } from '@/types'

const router = useRouter()
const userStore = useUserStore()

// ── 状态 ──
const loading = ref(false)
const creating = ref(false)
const posts = ref<(CommunityPost & { username?: string; parsedImages?: string[]; isLiked?: boolean; isFavorited?: boolean })[]>([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const currentFilter = ref('ALL')
const showCreateDialog = ref(false)
const uploadRef = ref()

// ── 发帖表单 ──
const createForm = reactive({
  content: '',
  postType: 'NORMAL',
  imageUrls: '' as string,
})
const uploadedUrls = ref<string[]>([])

// ── 上传配置 ──
const uploadUrl = '/api/upload/image'
const uploadHeaders = {
  get Authorization() {
    return `Bearer ${userStore.token}`
  }
}

// ── 加载帖子 ──
async function loadPosts() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize,
    }
    if (currentFilter.value !== 'ALL') {
      params.postType = currentFilter.value
    }
    const res: any = await getPostList(params)
    const records = res.data?.records || res.data || []
    posts.value = records.map((post: CommunityPost) => ({
      ...post,
      parsedImages: parseImages(post.imageUrls),
      isLiked: false,
      isFavorited: false,
    }))
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载帖子失败', e)
  } finally {
    loading.value = false
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

// ── 筛选 ──
function handleFilterChange() {
  currentPage.value = 1
  loadPosts()
}

// ── 删除帖子 ──
async function handleDeletePost(post: any) {
  try {
    await deletePost(post.id)
    posts.value = posts.value.filter(p => p.id !== post.id)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('帖子已删除')
  } catch (e) {
    console.error('删除失败', e)
  }
}

// ── 点赞 ──
async function handleLike(post: any) {
  try {
    await toggleLike('POST', post.id)
    post.isLiked = !post.isLiked
    post.likeCount += post.isLiked ? 1 : -1
  } catch (e) {
    console.error('点赞失败', e)
  }
}

// ── 收藏 ──
async function handleFavorite(post: any) {
  try {
    await toggleFavorite('POST', post.id)
    post.isFavorited = !post.isFavorited
    post.favoriteCount += post.isFavorited ? 1 : -1
  } catch (e) {
    console.error('收藏失败', e)
  }
}

// ── 发帖 ──
async function handleCreate() {
  if (!createForm.content.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }
  creating.value = true
  try {
    const data: any = {
      content: createForm.content,
      postType: createForm.postType,
    }
    if (uploadedUrls.value.length > 0) {
      data.imageUrls = JSON.stringify(uploadedUrls.value)
    }
    await createPost(data)
    ElMessage.success('发布成功')
    showCreateDialog.value = false
    resetCreateForm()
    currentPage.value = 1
    loadPosts()
  } catch (e) {
    console.error('发帖失败', e)
  } finally {
    creating.value = false
  }
}

function resetCreateForm() {
  createForm.content = ''
  createForm.postType = 'NORMAL'
  uploadedUrls.value = []
  uploadRef.value?.clearFiles()
}

// ── 图片上传 ──
function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

function handleUploadSuccess(response: any) {
  if (response.code === 200 && response.data?.url) {
    uploadedUrls.value.push(response.data.url)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('图片上传失败')
}

// ── 工具函数 ──
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

function postTypeLabel(type: string): string {
  const map: Record<string, string> = { NORMAL: '日常', OBSERVATION: '观测', RECOGNITION: '识别' }
  return map[type] || type
}

function postTypeTag(type: string): '' | 'success' | 'warning' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'info'> = {
    NORMAL: '',
    OBSERVATION: 'success',
    RECOGNITION: 'warning',
  }
  return map[type] || 'info'
}

function goToDetail(id: number) {
  router.push(`/community/post/${id}`)
}

function goToUser(id: number) {
  router.push(`/community/user/${id}`)
}

// ── 初始化 ──
onMounted(() => {
  loadPosts()
})
</script>

<style scoped lang="scss">
.feed-page {
  max-width: 720px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

/* ══════ 顶部 ══════ */
.feed-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--neutral-800);
  letter-spacing: -0.02em;
}

.page-desc {
  color: var(--neutral-400);
  font-size: 14px;
  margin-top: 4px;
}

/* ══════ 筛选 ══════ */
.feed-filters {
  margin-bottom: 20px;
}

/* ══════ 帖子卡片 ══════ */
.post-card {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100);
  padding: 20px;
  margin-bottom: 14px;
  transition: box-shadow 0.25s var(--ease-out), transform 0.25s var(--ease-out);
  animation: fadeSlideUp 0.5s var(--ease-out) both;

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-1px);
  }
}

.post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.post-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-delete-btn {
  background: none;
  border: 1px solid var(--neutral-200);
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;

  &:hover {
    color: var(--danger);
    border-color: var(--danger);
    background: rgba(196, 53, 53, 0.04);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  background: var(--gradient-ocean);
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);
  cursor: pointer;
  transition: color 0.15s;

  &:hover {
    color: var(--primary-main);
  }
}

.post-time {
  font-size: 12px;
  color: var(--neutral-400);
}

/* ══════ 内容 ══════ */
.post-content {
  margin-bottom: 12px;
  cursor: pointer;

  p {
    font-size: 15px;
    line-height: 1.6;
    color: var(--neutral-700);
    white-space: pre-wrap;
    word-break: break-word;
  }
}

/* ══════ 图片 ══════ */
.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 12px;
  border-radius: var(--radius-md);
  overflow: hidden;

  .single-image & {
    grid-template-columns: 1fr;
    max-width: 360px;
  }
}

.post-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.02);
  }

  .single-image & {
    aspect-ratio: 16 / 10;
  }
}

/* ══════ 操作栏 ══════ */
.post-actions {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--neutral-75);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: var(--neutral-400);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;

  &:hover {
    color: var(--primary-main);
    background: var(--primary-soft);
  }

  &.active {
    color: var(--accent-warm);
  }
}

/* ══════ 分页 ══════ */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* ══════ 空状态 ══════ */
.empty-state {
  padding: 60px 0;
}

/* ══════ 上传提示 ══════ */
.upload-tip {
  font-size: 12px;
  color: var(--neutral-400);
  margin-top: 8px;
}

/* ══════ 响应式 ══════ */
@media (max-width: 768px) {
  .feed-page {
    padding: 0 12px;
  }

  .post-images {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
