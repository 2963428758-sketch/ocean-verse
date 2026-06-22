<template>
  <section class="panel leaderboard-panel">
    <div class="panel-head">
      <h3 class="panel-title">
        <el-icon class="panel-title-icon"><Trophy /></el-icon>
        贡献者排行
      </h3>
    </div>

    <div v-if="loading" class="lb-list">
      <div v-for="i in 5" :key="i" class="lb-row lb-skeleton">
        <div class="skel-rank"></div>
        <div class="skel-avatar"></div>
        <div class="skel-info">
          <div class="skel-bar" style="width: 60%; height: 14px"></div>
          <div class="skel-bar" style="width: 40%; height: 12px; margin-top: 4px"></div>
        </div>
      </div>
    </div>

    <div v-else-if="isStub || leaders.length === 0" class="lb-empty">
      <el-icon :size="32" class="lb-empty-icon"><Trophy /></el-icon>
      <p>{{ isStub ? '排行榜功能开发中，敬请期待' : '暂无排行数据' }}</p>
    </div>

    <div v-else class="lb-list">
      <div v-for="(user, idx) in leaders" :key="user.userId ?? idx" class="lb-row">
        <span :class="['lb-rank', `rank-${idx + 1}`]">{{ idx + 1 }}</span>
        <el-avatar :size="32" :src="user.avatarUrl || undefined">
          {{ user.username?.charAt(0)?.toUpperCase() }}
        </el-avatar>
        <div class="lb-info">
          <span class="lb-name">{{ user.username }}</span>
          <span class="lb-stat">{{ user.score ?? 0 }} 次观测</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Trophy } from '@element-plus/icons-vue'
import { getLeaderboard } from '@/api/community'

defineProps<{
  loading: boolean
}>()

interface LeaderUser {
  userId: number
  username: string
  avatarUrl?: string
  score?: number
}

const leaders = ref<LeaderUser[]>([])
const isStub = ref(false)

async function loadLeaderboard() {
  try {
    const res: any = await getLeaderboard('points')
    const data = res.data
    // 后端排行榜暂未实现，返回字符串而非数组
    if (Array.isArray(data)) {
      leaders.value = data.slice(0, 5)
      isStub.value = false
    } else {
      leaders.value = []
      isStub.value = true
    }
  } catch {
    leaders.value = []
    isStub.value = false
  }
}

defineExpose({ loadLeaderboard })
</script>

<style scoped lang="scss">
.leaderboard-panel { animation-delay: 0.18s; }
.panel-head { margin-bottom: 16px; }
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

.lb-list { display: flex; flex-direction: column; }

.lb-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 0; border-bottom: 1px solid var(--neutral-75);
  &:last-child { border-bottom: none; }
}

.lb-rank {
  width: 24px; font-size: 14px; font-weight: 700;
  text-align: center; flex-shrink: 0; color: var(--neutral-400);
  &.rank-1 { color: #d4a574; }
  &.rank-2 { color: #b8b4ac; }
  &.rank-3 { color: #cc8a30; }
}

.lb-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.lb-name { font-size: 14px; font-weight: 500; color: var(--neutral-700); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.lb-stat { font-size: 12px; color: var(--neutral-400); }

.lb-empty {
  padding: 40px 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  .lb-empty-icon { color: var(--neutral-200); }
  p { color: var(--neutral-400); font-size: 13px; }
}

.lb-skeleton { &:hover { background: none; } }
.skel-rank { width: 24px; height: 14px; border-radius: 4px; background: var(--neutral-100); animation: shimmer 1.5s infinite; }
.skel-avatar { width: 32px; height: 32px; border-radius: 50%; background: var(--neutral-100); animation: shimmer 1.5s infinite; }
.skel-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.skel-bar { border-radius: 4px; background: var(--neutral-100); animation: shimmer 1.5s infinite; }

.panel {
  background: var(--surface-card); border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100); padding: 22px;
  animation: fadeSlideUp 0.45s var(--ease-out) both;
}

@keyframes shimmer { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }
</style>
