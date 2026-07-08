<template>
  <div class="chat-history-page">
    <!-- 导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">问答历史</span>
      <span class="nav-count" v-if="total > 0">共 {{ total }} 条记录</span>
    </div>

    <!-- 列表 -->
    <div v-loading="loading" class="history-list">
      <div v-if="records.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">💬</div>
        <p>暂无问答记录</p>
      </div>

      <div
        v-for="item in records"
        :key="item.id"
        class="history-card"
      >
        <div class="card-header">
          <span class="question-type-badge" :class="typeClass(item.questionType)">
            {{ typeLabel(item.questionType) }}
          </span>
          <span class="card-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <div class="question-section">
          <div class="section-label">问题</div>
          <p class="question-text">{{ item.questionText }}</p>
        </div>
        <div class="answer-section">
          <div class="section-label">回答</div>
          <div class="answer-text markdown-body" :class="{ expanded: expandedIds.has(item.id) }" v-html="renderMarkdown(expandedIds.has(item.id) ? item.answerText : truncateAnswer(item.answerText))"></div>
          <button
            v-if="item.answerText && item.answerText.length > 200"
            class="expand-btn"
            @click="toggleExpand(item.id)"
          >
            {{ expandedIds.has(item.id) ? '收起' : '展开全文' }}
          </button>
        </div>
        <div class="card-footer">
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            {{ item.processingTimeMs || 0 }}ms
          </span>
          <span class="meta-item" v-if="item.sourceReferences">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
            RAG
          </span>
          <span class="feedback-badge" v-if="item.feedback != null" :class="item.feedback === 1 ? 'good' : 'bad'">
            {{ item.feedback === 1 ? '满意' : '不满意' }}
          </span>
          <span class="card-code">{{ item.questionCode }}</span>
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
import { getChatHistory } from '@/api/ai'
import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true })

function renderMarkdown(text: string): string {
  if (!text) return ''
  return marked.parse(text) as string
}

const records = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const expandedIds = ref<Set<number>>(new Set())

function toggleExpand(id: number) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

const totalPages = computed(() => Math.ceil(total.value / pageSize))

async function fetchHistory() {
  loading.value = true
  try {
    const res: any = await getChatHistory({ page: page.value, size: pageSize })
    const data = res.data || res
    records.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('获取问答历史失败', e)
  } finally {
    loading.value = false
  }
}

function changePage(p: number) {
  page.value = p
  expandedIds.value = new Set()
  fetchHistory()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    SPECIES: '物种',
    ECOLOGY: '生态',
    CONSERVATION: '保护',
    GENERAL: '通用'
  }
  return map[type] || '通用'
}

function typeClass(type: string) {
  return (type || 'general').toLowerCase()
}

function truncateAnswer(text: string) {
  if (!text) return ''
  return text.length > 200 ? text.slice(0, 200) + '...' : text
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

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped lang="scss">
.chat-history-page {
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
  p { font-size: 15px; }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-card {
  padding: 20px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 12px;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  .question-type-badge {
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 10px;
    font-weight: 500;

    &.species { background: #e3f2fd; color: #1565c0; }
    &.ecology { background: #e8f5e9; color: #2e7d32; }
    &.conservation { background: #fce4ec; color: #c62828; }
    &.general { background: #f3e5f5; color: #6a1b9a; }
  }

  .card-time {
    font-size: 13px;
    color: var(--el-text-color-secondary, #909399);
  }
}

.question-section, .answer-section {
  margin-bottom: 12px;

  .section-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-secondary, #909399);
    margin-bottom: 4px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.question-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary, #303133);
  line-height: 1.5;
  margin: 0;
}

.answer-text {
  font-size: 14px;
  color: var(--el-text-color-regular, #606266);
  line-height: 1.6;
  background: var(--el-fill-color-lighter, #fafafa);
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;

  &.expanded {
    white-space: normal;
  }

  &.markdown-body {
    :deep(strong) { font-weight: 600; color: var(--el-text-color-primary, #303133); }
    :deep(em) { font-style: italic; }
    :deep(p) { margin: 0 0 6px; }
    :deep(p:last-child) { margin-bottom: 0; }
    :deep(ul), :deep(ol) { margin: 4px 0; padding-left: 20px; }
    :deep(code) { background: #f0f0f0; padding: 1px 4px; border-radius: 3px; font-size: 13px; }
    :deep(pre) { background: #f0f0f0; padding: 8px 12px; border-radius: 6px; overflow-x: auto; }
    :deep(pre code) { background: none; padding: 0; }
  }
}

.expand-btn {
  display: inline-block;
  margin-top: 6px;
  padding: 3px 12px;
  border: none;
  background: transparent;
  color: var(--el-color-primary, #409eff);
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover { opacity: 0.7; }
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-extra-light, #f2f3f5);
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);

  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .feedback-badge {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 10px;
    font-weight: 500;

    &.good { background: #e8f5e9; color: #2e7d32; }
    &.bad { background: #ffebee; color: #c62828; }
  }

  .card-code {
    margin-left: auto;
    font-size: 12px;
    color: var(--el-text-color-placeholder, #c0c4cc);
    font-family: monospace;
  }
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
