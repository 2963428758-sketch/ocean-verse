<template>
  <div class="ai-eval-page">
    <div class="nav-bar">
      <h2 class="page-title">AI 效果评估</h2>
      <button class="refresh-btn" @click="fetchStats" :disabled="loading">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :class="{ spinning: loading }">
          <path d="M23 4v6h-6M1 20v-6h6"/>
          <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
        </svg>
        刷新
      </button>
    </div>

    <div v-loading="loading" class="stats-grid">
      <!-- 问答统计卡片 -->
      <div class="stat-card">
        <div class="card-header">
          <span class="card-icon" style="background: #e3f2fd; color: #1565c0;">💬</span>
          <span class="card-label">问答统计</span>
        </div>
        <div class="stat-row">
          <span>总提问数</span>
          <strong>{{ stats.qaTotal }}</strong>
        </div>
        <div class="stat-row">
          <span>满意</span>
          <strong class="text-green">{{ stats.qaSatisfied }}</strong>
        </div>
        <div class="stat-row">
          <span>不满意</span>
          <strong class="text-red">{{ stats.qaUnsatisfied }}</strong>
        </div>
        <div class="stat-row highlight">
          <span>满意率</span>
          <strong>{{ stats.qaSatisfactionRate }}%</strong>
        </div>
        <div class="stat-row">
          <span>平均响应时间</span>
          <strong>{{ stats.qaAvgProcessingTimeMs }}ms</strong>
        </div>
      </div>

      <!-- 识别统计卡片 -->
      <div class="stat-card">
        <div class="card-header">
          <span class="card-icon" style="background: #e8f5e9; color: #2e7d32;">📷</span>
          <span class="card-label">识别统计</span>
        </div>
        <div class="stat-row">
          <span>总识别数</span>
          <strong>{{ stats.recognitionTotal }}</strong>
        </div>
        <div class="stat-row">
          <span>已验证</span>
          <strong class="text-green">{{ stats.recognitionVerified }}</strong>
        </div>
        <div class="stat-row highlight">
          <span>准确率</span>
          <strong>{{ stats.recognitionAccuracyRate }}%</strong>
        </div>
        <div class="stat-row">
          <span>平均耗时</span>
          <strong>{{ stats.recognitionAvgProcessingTimeMs }}ms</strong>
        </div>
        <div class="stat-row highlight">
          <span>高置信度占比</span>
          <strong>{{ stats.recognitionHighConfidenceRate }}%</strong>
        </div>
      </div>

      <!-- 问题类型分布 -->
      <div class="stat-card wide">
        <div class="card-header">
          <span class="card-icon" style="background: #f3e5f5; color: #6a1b9a;">📊</span>
          <span class="card-label">问题类型分布</span>
        </div>
        <div class="type-distribution">
          <div v-for="(count, type) in stats.qaTypeDistribution" :key="type" class="type-row">
            <span class="type-label">{{ typeLabel(String(type)) }}</span>
            <div class="type-bar-bg">
              <div class="type-bar-fill" :style="{ width: typePercent(count) + '%', background: typeColor(String(type)) }"></div>
            </div>
            <span class="type-count">{{ count }}</span>
          </div>
          <div v-if="!stats.qaTypeDistribution || Object.keys(stats.qaTypeDistribution).length === 0" class="empty-hint">暂无数据</div>
        </div>
      </div>

      <!-- 热门问题 Top 10 -->
      <div class="stat-card wide">
        <div class="card-header">
          <span class="card-icon" style="background: #fff3e0; color: #e65100;">🔥</span>
          <span class="card-label">热门问题 Top 10</span>
        </div>
        <div class="top-questions">
          <div v-for="(item, idx) in stats.qaTopQuestions" :key="idx" class="top-q-row">
            <span class="rank">{{ idx + 1 }}</span>
            <span class="q-text">{{ item.question }}</span>
            <span class="q-count">{{ item.count }} 次</span>
          </div>
          <div v-if="!stats.qaTopQuestions || stats.qaTopQuestions.length === 0" class="empty-hint">暂无数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAiEvalStats } from '@/api/ai'

const loading = ref(false)
const stats = ref<Record<string, any>>({})

async function fetchStats() {
  loading.value = true
  try {
    const res: any = await getAiEvalStats()
    stats.value = res.data || res || {}
  } catch (e) {
    console.error('获取 AI 评估统计失败', e)
  } finally {
    loading.value = false
  }
}

function typeLabel(type: string) {
  const map: Record<string, string> = { SPECIES: '物种', ECOLOGY: '生态', CONSERVATION: '保护', GENERAL: '通用' }
  return map[type] || type
}

function typeColor(type: string) {
  const map: Record<string, string> = { SPECIES: '#1565c0', ECOLOGY: '#2e7d32', CONSERVATION: '#c62828', GENERAL: '#6a1b9a' }
  return map[type] || '#909399'
}

function typePercent(count: number) {
  const total = stats.value.qaTotal || 1
  return Math.round((count / total) * 100)
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped lang="scss">
.ai-eval-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 16px;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
    margin: 0;
  }

  .refresh-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 16px;
    border: 1px solid var(--el-border-color, #dcdfe6);
    border-radius: 6px;
    background: var(--el-bg-color, #fff);
    color: var(--el-text-color-primary, #303133);
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;

    &:hover { border-color: var(--el-color-primary, #409eff); color: var(--el-color-primary, #409eff); }
    &:disabled { opacity: 0.6; cursor: not-allowed; }

    .spinning { animation: spin 1s linear infinite; }
  }
}

@keyframes spin { to { transform: rotate(360deg); } }

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-card {
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 12px;
  padding: 20px;

  &.wide { grid-column: 1 / -1; }

  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;

    .card-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
    }

    .card-label {
      font-size: 15px;
      font-weight: 600;
      color: var(--el-text-color-primary, #303133);
    }
  }
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
  color: var(--el-text-color-regular, #606266);
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f3f5);

  &:last-child { border-bottom: none; }

  strong { font-weight: 600; color: var(--el-text-color-primary, #303133); }

  &.highlight {
    background: var(--el-fill-color-lighter, #fafafa);
    margin: 4px -8px;
    padding: 8px;
    border-radius: 6px;
    border-bottom: none;
  }
}

.text-green { color: #2e7d32 !important; }
.text-red { color: #c62828 !important; }

.type-distribution, .top-questions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .type-label {
    width: 40px;
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-regular, #606266);
    text-align: right;
  }

  .type-bar-bg {
    flex: 1;
    height: 20px;
    background: var(--el-fill-color-light, #f5f7fa);
    border-radius: 4px;
    overflow: hidden;
  }

  .type-bar-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.5s ease;
    min-width: 2px;
  }

  .type-count {
    width: 30px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
  }
}

.top-q-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light, #f2f3f5);

  &:last-child { border-bottom: none; }

  .rank {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    background: var(--el-fill-color, #e9ecf0);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    color: var(--el-text-color-secondary, #909399);
    flex-shrink: 0;
  }

  .q-text {
    flex: 1;
    font-size: 14px;
    color: var(--el-text-color-primary, #303133);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .q-count {
    flex-shrink: 0;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-secondary, #909399);
  }
}

.empty-hint {
  text-align: center;
  padding: 20px;
  color: var(--el-text-color-secondary, #909399);
  font-size: 14px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
