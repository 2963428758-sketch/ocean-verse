<template>
  <div class="recognition-history-page">
    <!-- 导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">识别历史</span>
      <span class="nav-count" v-if="total > 0">共 {{ total }} 条记录</span>
    </div>

    <!-- 列表 -->
    <div v-loading="loading" class="history-list">
      <div v-if="records.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📷</div>
        <p>暂无识别记录</p>
        <button class="go-recognize-btn" @click="$router.push('/ai/recognize')">去识别</button>
      </div>

      <div
        v-for="item in records"
        :key="item.id"
        class="history-card"
        @click="goToDetail(item)"
      >
        <div class="card-thumb">
          <img :src="item.imageUrl" :alt="item.predictedSpeciesName || '识别图片'" loading="lazy" />
        </div>
        <div class="card-body">
          <div class="card-top">
            <span class="species-name">{{ item.predictedSpeciesName || '未知物种' }}</span>
            <span class="status-badge" :class="item.verificationStatus === 'VERIFIED' ? 'verified' : 'pending'">
              {{ item.verificationStatus === 'VERIFIED' ? '已验证' : '待确认' }}
            </span>
          </div>
          <div class="card-meta">
            <span class="meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              {{ formatTime(item.createTime) }}
            </span>
            <span class="meta-item">
              置信度
              <span class="confidence-value" :class="confidenceClass(item.confidenceScore)">
                {{ formatConfidence(item.confidenceScore) }}
              </span>
            </span>
            <span class="meta-item">
              耗时 {{ item.processingTimeMs || 0 }}ms
            </span>
          </div>
          <div class="card-code">编号：{{ item.recognitionCode }}</div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-bar">
      <button :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecognitionHistory } from '@/api/ai'

const router = useRouter()
const records = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize))

async function fetchHistory() {
  loading.value = true
  try {
    const res: any = await getRecognitionHistory({ page: page.value, size: pageSize })
    const data = res.data || res
    records.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取识别历史失败', e)
  } finally {
    loading.value = false
  }
}

function changePage(p: number) {
  page.value = p
  fetchHistory()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goToDetail(item: any) {
  router.push(`/ai/recognize?id=${item.id}`)
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function formatConfidence(score: any) {
  if (score == null) return '--'
  return `${Math.round(Number(score) * 100)}%`
}

function confidenceClass(score: any) {
  if (score == null) return ''
  const s = Number(score)
  if (s >= 0.8) return 'high'
  if (s >= 0.5) return 'medium'
  return 'low'
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped lang="scss">
.recognition-history-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 16px;
}

.nav-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .back-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    background: var(--el-fill-color-light, #f5f7fa);
    border-radius: 8px;
    cursor: pointer;
    color: var(--el-text-color-primary, #303133);
    transition: background 0.2s;

    &:hover { background: var(--el-fill-color, #e9ecf0); }
  }

  .nav-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
  }

  .nav-count {
    margin-left: auto;
    font-size: 13px;
    color: var(--el-text-color-secondary, #909399);
  }
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--el-text-color-secondary, #909399);

  .empty-icon { font-size: 48px; margin-bottom: 12px; }
  p { font-size: 15px; margin-bottom: 16px; }

  .go-recognize-btn {
    padding: 8px 24px;
    background: var(--el-color-primary, #409eff);
    color: #fff;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    transition: opacity 0.2s;

    &:hover { opacity: 0.85; }
  }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5, #a0cfff);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }
}

.card-thumb {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-light, #f5f7fa);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 8px;

  .species-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .status-badge {
    flex-shrink: 0;
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 10px;
    font-weight: 500;

    &.verified {
      background: #e8f5e9;
      color: #2e7d32;
    }
    &.pending {
      background: #fff3e0;
      color: #e65100;
    }
  }
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);

  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .confidence-value {
    font-weight: 600;
    &.high { color: #2e7d32; }
    &.medium { color: #f57c00; }
    &.low { color: #c62828; }
  }
}

.card-code {
  font-size: 12px;
  color: var(--el-text-color-placeholder, #c0c4cc);
  font-family: monospace;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 12px 0;

  button {
    padding: 6px 16px;
    border: 1px solid var(--el-border-color, #dcdfe6);
    border-radius: 6px;
    background: var(--el-bg-color, #fff);
    color: var(--el-text-color-primary, #303133);
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      border-color: var(--el-color-primary, #409eff);
      color: var(--el-color-primary, #409eff);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  .page-info {
    font-size: 13px;
    color: var(--el-text-color-secondary, #909399);
  }
}
</style>
