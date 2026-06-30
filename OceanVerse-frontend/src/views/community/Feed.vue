<template>
  <div class="feed-page">
    <!-- 顶部发布栏 -->
    <div class="feed-topbar">
      <div class="search-input-box">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索帖子..."
          @keyup.enter="currentPage = 1; loadPosts()"
        />
        <button v-if="searchKeyword" class="search-clear" @click="searchKeyword = ''; currentPage = 1; loadPosts()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <button class="publish-btn" @click="showCreateDialog = true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
    </div>

    <!-- 筛选标签 + 排序 -->
    <div class="feed-toolbar">
      <div class="feed-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: currentFilter === tab.value }"
          @click="currentFilter = tab.value; currentPage = 1; loadPosts()"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="sort-buttons">
        <button
          class="sort-btn"
          :class="{ active: currentSort === 'newest' }"
          @click="currentSort = 'newest'; currentPage = 1; loadPosts()"
        >
          最新
        </button>
        <button
          class="sort-btn"
          :class="{ active: currentSort === 'likes' }"
          @click="currentSort = 'likes'; currentPage = 1; loadPosts()"
        >
          最热
        </button>
      </div>
    </div>

    <!-- 瀑布流帖子列表 -->
    <div v-loading="loading" class="waterfall" :class="{ 'single-col': posts.length === 0 }">
      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🌊</div>
        <p>暂无动态，快来发布第一条吧！</p>
      </div>

      <div
        v-for="post in posts"
        :key="post.id"
        class="feed-card"
        @click="goToDetail(post.id)"
      >
        <!-- 图片区 -->
        <div v-if="post.parsedImages?.length" class="card-image-wrap">
          <img
            :src="post.parsedImages[0]"
            class="card-image"
            loading="lazy"
          />
          <span v-if="post.parsedImages.length > 1" class="image-count">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="white"><rect x="3" y="3" width="18" height="18" rx="3" stroke="white" stroke-width="2" fill="none"/><rect x="7" y="7" width="14" height="14" rx="3" fill="white" opacity="0.5"/></svg>
            {{ post.parsedImages.length }}
          </span>
          <span v-if="post.postType !== 'NORMAL'" class="type-badge" :class="post.postType">
            {{ postTypeLabel(post.postType) }}
          </span>
          <span v-if="(post as any).status === 3" class="type-badge pending">待审核</span>
        </div>
        <div v-else class="card-image-wrap no-image">
          <span class="no-image-icon">📝</span>
        </div>

        <!-- 内容区 -->
        <div class="card-body">
          <p class="card-text">{{ post.content }}</p>

          <!-- 底部：用户 + 点赞 -->
          <div class="card-footer">
            <div class="card-user" @click.stop="goToUser(post.userId)">
              <div class="card-avatar">
                <img v-if="post.avatarUrl" :src="post.avatarUrl" class="avatar-img" />
                <span v-else>{{ post.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
              </div>
              <span class="card-username">{{ post.username || '用户' }}</span>
            </div>
            <div class="card-actions" @click.stop>
              <button
                class="like-btn"
                :class="{ liked: post.isLiked }"
                @click="handleLike(post)"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" :fill="post.isLiked ? '#ff4757' : 'none'" :stroke="post.isLiked ? '#ff4757' : '#999'" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                <span v-if="post.likeCount">{{ post.likeCount }}</span>
              </button>
              <el-popconfirm
                v-if="post.userId === userStore.userId || userStore.role === 'SUPER_ADMIN' || userStore.role === 'ADMIN'"
                title="确定删除？"
                @confirm="handleDeletePost(post)"
              >
                <template #reference>
                  <button class="delete-btn" @click.stop>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  </button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <button
        class="page-btn"
        :disabled="currentPage <= 1"
        @click="currentPage--; loadPosts()"
      >上一页</button>
      <span class="page-info">{{ currentPage }} / {{ Math.ceil(total / pageSize) }}</span>
      <button
        class="page-btn"
        :disabled="currentPage >= Math.ceil(total / pageSize)"
        @click="currentPage++; loadPosts()"
      >下一页</button>
    </div>

    <!-- 发帖弹窗 -->
    <el-dialog
      v-model="showCreateDialog"
      title="发布动态"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetCreateForm"
      class="create-dialog"
    >
      <div class="dialog-body">
        <textarea
          v-model="createForm.content"
          class="create-textarea"
          placeholder="分享你的海洋探索故事..."
          maxlength="500"
        ></textarea>
        <div class="create-meta">
          <select v-model="createForm.postType" class="create-select">
            <option value="NORMAL">日常分享</option>
            <option value="OBSERVATION">观测记录</option>
            <option value="RECOGNITION">物种识别</option>
          </select>
        </div>
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
          class="create-upload"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
        </el-upload>
        <p class="upload-hint">最多 9 张，单张不超过 10MB</p>
      </div>
      <template #footer>
        <button class="dialog-btn cancel" @click="showCreateDialog = false">取消</button>
        <button class="dialog-btn confirm" :disabled="creating || !createForm.content.trim()" @click="handleCreate">
          {{ creating ? '发布中...' : '发布' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const loading = ref(false)
const creating = ref(false)
const posts = ref<(CommunityPost & { username?: string; parsedImages?: string[]; isLiked?: boolean; isFavorited?: boolean })[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const currentFilter = ref('ALL')
const currentSort = ref('newest')
const searchKeyword = ref('')
const showCreateDialog = ref(false)
const uploadRef = ref()

const tabs = [
  { label: '推荐', value: 'ALL' },
  { label: '日常', value: 'NORMAL' },
  { label: '观测', value: 'OBSERVATION' },
  { label: '识别', value: 'RECOGNITION' },
  { label: '我的待审核', value: 'MY_PENDING' },
]

const createForm = reactive({
  content: '',
  postType: 'NORMAL',
})
const uploadedUrls = ref<string[]>([])

const uploadUrl = '/api/upload/image'
const uploadHeaders = {
  get Authorization() {
    return `Bearer ${userStore.token}`
  }
}

async function loadPosts() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize }
    if (currentFilter.value === 'MY_PENDING') {
      params.myPending = true
    } else if (currentFilter.value !== 'ALL') {
      params.postType = currentFilter.value
    }
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (currentSort.value === 'likes') {
      params.orderBy = 'HOT'
    }
    const res: any = await getPostList(params)
    const records = res.data?.records || res.data || []
    posts.value = records.map((post: CommunityPost) => ({
      ...post,
      parsedImages: parseImages(post.imageUrls),
      isLiked: !!(post as any).isLiked,
      isFavorited: !!(post as any).isFavorited,
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

async function handleDeletePost(post: any) {
  try {
    await deletePost(post.id)
    posts.value = posts.value.filter(p => p.id !== post.id)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已删除')
  } catch (e) {
    console.error('删除失败', e)
  }
}

async function handleLike(post: any) {
  try {
    await toggleLike('POST', post.id)
    post.isLiked = !post.isLiked
    post.likeCount += post.isLiked ? 1 : -1
  } catch (e) {
    console.error('点赞失败', e)
  }
}

async function handleCreate() {
  if (!createForm.content.trim()) return
  creating.value = true
  try {
    const data: any = { content: createForm.content, postType: createForm.postType }
    if (uploadedUrls.value.length > 0) data.imageUrls = JSON.stringify(uploadedUrls.value)
    await createPost(data)
    ElMessage.success('发布成功，等待审核通过后将显示在广场')
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

function beforeUpload(file: File) {
  const ok = file.type.startsWith('image/') && file.size / 1024 / 1024 < 10
  if (!ok) ElMessage.error('仅支持 10MB 以内的图片')
  return ok
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

function postTypeLabel(type: string): string {
  return { NORMAL: '日常', OBSERVATION: '观测', RECOGNITION: '识别' }[type] || type
}

function goToDetail(id: number) {
  router.push(`/community/post/${id}`)
}

function goToUser(id: number) {
  router.push(`/community/user/${id}`)
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped lang="scss">
.feed-page {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

/* ══════ 顶部发布栏 ══════ */
.feed-topbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.search-input-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-card);
  border: 1px solid var(--neutral-100);
  border-radius: 20px;
  padding: 0 16px;
  transition: all 0.2s;

  &:focus-within {
    border-color: var(--primary-lighter);
    box-shadow: 0 0 0 2px rgba(26, 107, 138, 0.08);
  }
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: none;
  font-size: 14px;
  color: var(--neutral-700);
  padding: 10px 0;
  font-family: inherit;

  &::placeholder {
    color: var(--neutral-400);
  }
}

.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: background 0.15s;

  &:hover {
    background: var(--neutral-75);
  }
}

.publish-btn {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-ocean);
  color: #fff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s;
  flex-shrink: 0;

  &:hover {
    transform: scale(1.05);
  }
}

/* ══════ 工具栏（筛选 + 排序）══════ */
.feed-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--neutral-100);
  margin-bottom: 16px;
}

.tab-item {
  padding: 10px 20px;
  border: none;
  border-radius: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--neutral-500);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

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

.feed-tabs {
  display: flex;
  gap: 4px;
}

.sort-buttons {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.sort-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
  color: var(--neutral-500);
  background: var(--neutral-50);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: var(--neutral-700);
    background: var(--neutral-100);
  }

  &.active {
    color: #fff;
    background: var(--neutral-800);
  }
}

/* ══════ 瀑布流 ══════ */
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
@media (max-width: 90px) {
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

/* ══════ 图片区 ══════ */
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
}

.image-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 3px;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
}

.type-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  backdrop-filter: blur(8px);

  &.NORMAL { background: rgba(26, 107, 138, 0.85); color: #fff; }
  &.OBSERVATION { background: rgba(45, 138, 94, 0.85); color: #fff; }
  &.RECOGNITION { background: rgba(224, 120, 80, 0.85); color: #fff; }
  &.pending { background: rgba(204, 138, 48, 0.85); color: #fff; }
}

/* ══════ 内容区 ══════ */
.card-body {
  padding: 10px 12px 8px;
}

.card-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--neutral-700);
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

/* ══════ 底部 ══════ */
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
  cursor: pointer;
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
  overflow: hidden;

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.card-username {
  font-size: 12px;
  font-weight: 500;
  color: var(--neutral-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;

  .card-user:hover & {
    color: var(--primary-main);
  }
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--neutral-400);
  padding: 2px 4px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    color: #ff4757;
    background: rgba(255, 71, 87, 0.06);
  }

  &.liked {
    color: #ff4757;
  }
}

.delete-btn {
  display: flex;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;
  opacity: 0;
  transition: all 0.2s;

  .feed-card:hover & {
    opacity: 1;
  }

  &:hover {
    color: var(--danger);
    background: rgba(196, 53, 53, 0.06);
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

/* ══════ 空状态 ══════ */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  columns: 1;

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

/* ══════ 发帖弹窗 ══════ */
.create-textarea {
  width: 100%;
  min-height: 120px;
  border: 1px solid var(--neutral-100);
  border-radius: var(--radius-md);
  padding: 14px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--neutral-700);
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;

  &:focus {
    border-color: var(--primary-lighter);
  }

  &::placeholder {
    color: var(--neutral-300);
  }
}

.create-meta {
  margin: 12px 0;
}

.create-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--neutral-100);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--neutral-600);
  background: var(--surface-card);
  outline: none;
  cursor: pointer;

  &:focus {
    border-color: var(--primary-lighter);
  }
}

.create-upload {
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 12px;
  color: var(--neutral-400);
  margin-top: 4px;
}

.dialog-body {
  padding: 0 4px;
}

.dialog-btn {
  padding: 9px 28px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;

  &.cancel {
    background: var(--neutral-75);
    color: var(--neutral-600);

    &:hover {
      background: var(--neutral-100);
    }
  }

  &.confirm {
    background: var(--gradient-ocean);
    color: #fff;
    box-shadow: 0 2px 8px rgba(26, 107, 138, 0.25);

    &:hover:not(:disabled) {
      box-shadow: 0 4px 14px rgba(26, 107, 138, 0.35);
      transform: translateY(-1px);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

/* ══════ 响应式 ══════ */
@media (max-width: 600px) {
  .feed-page {
    padding: 0 4px;
  }

  .feed-topbar {
    gap: 8px;
  }

  .search-input-box {
    padding: 0 12px;
  }

  .search-input {
    font-size: 13px;
    padding: 8px 0;
  }

  .publish-btn {
    width: 38px;
    height: 38px;
  }

  .feed-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .feed-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    width: 100%;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .tab-item {
    padding: 8px 14px;
    font-size: 13px;
    white-space: nowrap;
  }

  .sort-buttons {
    align-self: flex-end;
  }

  .card-body {
    padding: 8px 10px 6px;
  }

  .card-text {
    font-size: 12px;
  }
}
</style>
