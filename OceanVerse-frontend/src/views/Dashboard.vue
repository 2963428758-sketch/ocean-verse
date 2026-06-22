<template>
  <div class="dashboard">
    <!-- 问候 -->
    <GreetingHeader :username="userStore.nickname || userStore.username" />

    <!-- 加载错误提示 -->
    <div v-if="loadError" class="error-banner">
      <span>{{ loadError }}</span>
      <button class="error-retry" @click="loadAll">重试</button>
    </div>

    <!-- 统计卡片 -->
    <StatsGrid :loading="isLoading" :cards="statCards" />

    <!-- 图表双栏 -->
    <div class="charts-row">
      <TrendChart :loading="isLoading" />
      <IucnPieChart :loading="isLoading" />
    </div>

    <!-- 物种聚焦轮播 -->
    <SpeciesSpotlight ref="spotlightRef" :loading="isLoading" />

    <!-- 底部双栏 -->
    <div class="bottom-row">
      <RecentObservations ref="observationsRef" :loading="isLoading" />
      <LeaderboardPanel ref="leaderboardRef" :loading="isLoading" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useUserStore } from '@/stores/user'
import { getDashboardData } from '@/api/visual'
import GreetingHeader from '@/components/dashboard/GreetingHeader.vue'
import StatsGrid from '@/components/dashboard/StatsGrid.vue'
import SpeciesSpotlight from '@/components/dashboard/SpeciesSpotlight.vue'
import RecentObservations from '@/components/dashboard/RecentObservations.vue'
import TrendChart from '@/components/dashboard/TrendChart.vue'
import IucnPieChart from '@/components/dashboard/IucnPieChart.vue'
import LeaderboardPanel from '@/components/dashboard/LeaderboardPanel.vue'
import { Collection, Compass, ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import type { StatCard } from '@/components/dashboard/StatsGrid.vue'

const userStore = useUserStore()

const isLoading = ref(true)
const loadError = ref<string | null>(null)

const spotlightRef = ref<InstanceType<typeof SpeciesSpotlight>>()
const observationsRef = ref<InstanceType<typeof RecentObservations>>()
const leaderboardRef = ref<InstanceType<typeof LeaderboardPanel>>()

/* -- 统计卡片配置 -- */
const statCards = ref<StatCard[]>([
  { key: 'species', label: '物种总数', value: null, icon: markRaw(Collection), iconColor: '#1a6b8a', iconBg: 'linear-gradient(135deg, #e8f4f8, #d0eaf3)' },
  { key: 'observation', label: '观测记录', value: null, icon: markRaw(Compass), iconColor: '#d4603a', iconBg: 'linear-gradient(135deg, #fef4ee, #fce6d6)' },
  { key: 'posts', label: '社区帖子', value: null, icon: markRaw(ChatDotRound), iconColor: '#7c5cbf', iconBg: 'linear-gradient(135deg, #f0eefa, #e2dcf5)' },
  { key: 'users', label: '用户总数', value: null, icon: markRaw(UserFilled), iconColor: '#2d8a5e', iconBg: 'linear-gradient(135deg, #e8f5ee, #d2ebdc)' },
])

/* -- 加载统计卡片数据 -- */
async function loadStats() {
  const res: any = await getDashboardData()
  const data = res.data
  const findCard = (key: string) => statCards.value.find(c => c.key === key)
  findCard('species')!.value = data.totalSpecies ?? null
  findCard('observation')!.value = data.totalObservations ?? null
  findCard('posts')!.value = data.totalPosts ?? null
  findCard('users')!.value = data.totalUsers ?? null
}

/* -- 并行加载所有数据 -- */
async function loadAll() {
  isLoading.value = true
  loadError.value = null

  try {
    await loadStats()
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
    loadError.value = '数据加载失败，请稍后刷新'
  }

  // 物种轮播、近期观测、排行榜全部并行，互不影响
  await Promise.allSettled([
    spotlightRef.value?.loadSpotlight?.() ?? Promise.resolve(),
    observationsRef.value?.loadRecentObservations?.() ?? Promise.resolve(),
    leaderboardRef.value?.loadLeaderboard?.() ?? Promise.resolve(),
  ])

  isLoading.value = false
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped lang="scss">
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.35s ease;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--surface-card);
  border: 1px solid #f5c6c6;
  border-left: 3px solid #c43535;
  border-radius: var(--radius-md);
  padding: 12px 18px;
  font-size: 14px;
  color: #c43535;
}

.error-retry {
  background: none;
  border: 1px solid #c43535;
  color: #c43535;
  border-radius: var(--radius-sm);
  padding: 4px 14px;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s var(--ease-out);

  &:hover {
    background: #c43535;
    color: #fff;
  }
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .charts-row,
  .bottom-row {
    grid-template-columns: 1fr;
  }
}
</style>
