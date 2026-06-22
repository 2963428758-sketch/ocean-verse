<template>
  <section class="panel activity-panel">
    <div class="panel-head">
      <h3 class="panel-title">
        <el-icon class="panel-title-icon"><Compass /></el-icon>
        近期观测
      </h3>
      <button class="panel-link" @click="router.push({ name: 'ObservationList' })">查看全部</button>
    </div>

    <div v-if="loading" class="activity-list">
      <div v-for="i in 5" :key="i" class="activity-row activity-skeleton">
        <div class="skel-dot"></div>
        <div class="skel-info">
          <div class="skel-bar" style="width: 70%; height: 14px"></div>
          <div class="skel-bar" style="width: 45%; height: 12px; margin-top: 4px"></div>
        </div>
        <div class="skel-tag"></div>
      </div>
    </div>

    <div v-else-if="activities.length === 0" class="activity-empty">
      <p>暂无观测记录</p>
    </div>

    <div v-else class="activity-list">
      <div v-for="(item, i) in activities" :key="item.id ?? i" class="activity-row">
        <div class="activity-dot" :style="{ background: getTypeColor(item.observationType) }"></div>
        <div class="activity-info">
          <span class="activity-name">{{ item.observerName || '未知观测者' }}</span>
          <span class="activity-meta">
            {{ formatType(item.observationType) }}
            <span class="meta-dot">&middot;</span>
            {{ formatDate(item.observationDate) }}
          </span>
        </div>
        <span
          class="activity-tag"
          :style="{ color: getTypeColor(item.observationType), background: getTypeBg(item.observationType) }"
        >
          {{ formatType(item.observationType) }}
        </span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Compass } from '@element-plus/icons-vue'
import { getObservationList } from '@/api/observation'
import type { Observation } from '@/types'

const router = useRouter()

defineProps<{
  loading: boolean
}>()

const activities = ref<Observation[]>([])

const TYPE_MAP: Record<string, { label: string; color: string; bg: string }> = {
  SIGHTING: { label: '目击', color: '#2d8a5e', bg: '#e8f5ee' },
  DIVE: { label: '潜水', color: '#1a6b8a', bg: '#e8f4f8' },
  SURVEY: { label: '采样', color: '#cc8a30', bg: '#fef4ee' },
  TRACKING: { label: '追踪', color: '#7c5cbf', bg: '#f0eefa' },
  ACOUSTIC: { label: '声学', color: '#1a6b8a', bg: '#e8f4f8' },
  PHOTO: { label: '影像', color: '#7c5cbf', bg: '#f0eefa' },
}

const getTypeColor = (type: string) => TYPE_MAP[type]?.color || '#948f86'
const getTypeBg = (type: string) => TYPE_MAP[type]?.bg || '#f5f5f5'
const formatType = (type: string) => TYPE_MAP[type]?.label || type || '未知'

const formatDate = (date: string) => {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

async function loadRecentObservations() {
  try {
    const res: any = await getObservationList({ page: 1, size: 5 })
    activities.value = res.data?.records || []
  } catch {
    activities.value = []
  }
}

defineExpose({ loadRecentObservations })
</script>

<style scoped lang="scss">
.activity-panel { animation-delay: 0.18s; }

.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--neutral-700);
  letter-spacing: -0.01em;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-title-icon {
  color: var(--primary-main);
  font-size: 16px;
}

.panel-link {
  font-size: 13px; color: var(--primary-main); background: none; border: none;
  cursor: pointer; font-weight: 500; padding: 0; transition: color 0.15s;
  &:hover { color: var(--primary-light); }
}

.activity-list { display: flex; flex-direction: column; }

.activity-row {
  display: flex; align-items: center; gap: 14px;
  padding: 13px 0; border-bottom: 1px solid var(--neutral-75);
  transition: background 0.15s;
  &:last-child { border-bottom: none; }
  &:hover {
    background: var(--neutral-25); margin: 0 -12px;
    padding-left: 12px; padding-right: 12px; border-radius: var(--radius-sm);
  }
}

.activity-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

.activity-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }

.activity-name {
  font-size: 14px; font-weight: 500; color: var(--neutral-700);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.activity-meta { font-size: 12px; color: var(--neutral-400); }
.meta-dot { margin: 0 5px; opacity: 0.5; }

.activity-tag {
  font-size: 11px; font-weight: 600; padding: 3px 10px;
  border-radius: 20px; flex-shrink: 0; letter-spacing: 0.02em;
}

.activity-empty { padding: 40px 0; text-align: center; p { color: var(--neutral-400); font-size: 13px; } }

.activity-skeleton { &:hover { background: none; margin: 0; padding: 13px 0; } }
.skel-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--neutral-100); animation: shimmer 1.5s infinite; }
.skel-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.skel-bar { border-radius: 4px; background: var(--neutral-100); animation: shimmer 1.5s infinite; }
.skel-tag { width: 48px; height: 20px; border-radius: 20px; background: var(--neutral-100); animation: shimmer 1.5s infinite; }

.panel {
  background: var(--surface-card); border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100); padding: 22px;
  animation: fadeSlideUp 0.45s var(--ease-out) both;
}

@keyframes shimmer { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }
</style>
