<template>
  <div class="dashboard">
    <!-- 问候区 -->
    <section class="greeting">
      <div>
        <h1 class="greeting-title">{{ greetingText }}，{{ userStore.username }}</h1>
        <p class="greeting-sub">{{ todayStr }}</p>
      </div>
    </section>

    <!-- 统计 Bento Grid -->
    <section class="stats-bento">
      <div
        v-for="(card, i) in statCards"
        :key="card.key"
        :class="['stat-tile', card.size]"
        :style="{ animationDelay: `${i * 0.06}s` }"
      >
        <div class="tile-inner">
          <div class="tile-icon-wrap" :style="{ background: card.iconBg }">
            <component :is="card.iconComp" class="tile-icon" />
          </div>
          <div class="tile-data">
            <span class="tile-value">{{ card.value }}</span>
            <span class="tile-label">{{ card.label }}</span>
          </div>
          <span class="tile-trend" :class="card.trendDir" v-if="card.trend">
            {{ card.trend }}
          </span>
        </div>
      </div>
    </section>

    <!-- 主内容区 -->
    <section class="content-grid">
      <!-- 左侧：近期观测 -->
      <div class="panel panel-activity">
        <div class="panel-head">
          <h3 class="panel-title">近期观测</h3>
          <button class="panel-link" @click="$router.push('/observation/list')">查看全部</button>
        </div>
        <div class="activity-list">
          <div v-for="(item, i) in recentActivities" :key="i" class="activity-row"
               :style="{ animationDelay: `${0.3 + i * 0.05}s` }">
            <div class="activity-dot" :style="{ background: item.color }"></div>
            <div class="activity-info">
              <span class="activity-name">{{ item.species }}</span>
              <span class="activity-meta">{{ item.observer }} · {{ item.date }}</span>
            </div>
            <span class="activity-tag" :style="{ color: item.tagColor, background: item.tagBg }">
              {{ item.type }}
            </span>
          </div>
        </div>
        <div v-if="recentActivities.length === 0" class="panel-empty">
          暂无近期观测记录
        </div>
      </div>

      <!-- 右侧栏 -->
      <div class="side-column">
        <!-- 快捷操作 -->
        <div class="panel panel-actions">
          <div class="panel-head">
            <h3 class="panel-title">快捷操作</h3>
          </div>
          <div class="action-grid">
            <button
              v-for="action in quickActions"
              :key="action.label"
              class="action-btn"
              @click="$router.push(action.route)"
            >
              <span class="action-icon" :style="{ color: action.color }">{{ action.icon }}</span>
              <span class="action-label">{{ action.label }}</span>
            </button>
          </div>
        </div>

        <!-- 物种聚焦 -->
        <div class="panel panel-spotlight">
          <div class="panel-head">
            <h3 class="panel-title">物种聚焦</h3>
          </div>
          <div class="spotlight-card">
            <div class="spotlight-visual">
              <div class="spotlight-circle">
                <span class="spotlight-emoji">{{ spotlightSpecies.emoji }}</span>
              </div>
            </div>
            <div class="spotlight-info">
              <h4 class="spotlight-name">{{ spotlightSpecies.name }}</h4>
              <p class="spotlight-sci">{{ spotlightSpecies.sciName }}</p>
              <span class="spotlight-status" :style="{
                color: spotlightSpecies.statusColor,
                background: spotlightSpecies.statusBg
              }">
                {{ spotlightSpecies.status }}
              </span>
            </div>
          </div>
        </div>

        <!-- 系统信息 -->
        <div class="panel panel-sys">
          <div class="sys-row">
            <span class="sys-label">OceanVerse</span>
            <span class="sys-value">v1.0.0</span>
          </div>
          <div class="sys-row">
            <span class="sys-label">技术栈</span>
            <span class="sys-value">Vue 3 + Spring Boot</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getDashboardData } from '@/api/visual'

const userStore = useUserStore()

/* ── 问候 ── */
const greetingText = computed(() => {
  const hr = new Date().getHours()
  if (hr < 6) return '夜深了'
  if (hr < 12) return '早上好'
  if (hr < 14) return '中午好'
  if (hr < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const d = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日  星期${weekdays[d.getDay()]}`
})

/* ── 统计卡片 ── */
const statCards = ref([
  {
    key: 'species',
    label: '物种总数',
    value: '-',
    size: 'tile-wide',
    iconBg: '#e8f4f8',
    iconComp: h('span', { innerHTML: '🐋', style: 'font-size:22px' }),
    trend: null,
    trendDir: ''
  },
  {
    key: 'observation',
    label: '观测记录',
    value: '-',
    size: '',
    iconBg: '#fef4ee',
    iconComp: h('span', { innerHTML: '🔭', style: 'font-size:22px' }),
    trend: null,
    trendDir: ''
  },
  {
    key: 'ai',
    label: 'AI 识别',
    value: '-',
    size: '',
    iconBg: '#f0eefa',
    iconComp: h('span', { innerHTML: '✨', style: 'font-size:22px' }),
    trend: null,
    trendDir: ''
  },
  {
    key: 'users',
    label: '用户总数',
    value: '-',
    size: 'tile-wide',
    iconBg: '#f4f8e8',
    iconComp: h('span', { innerHTML: '👥', style: 'font-size:22px' }),
    trend: null,
    trendDir: ''
  }
])

onMounted(async () => {
  try {
    const res = await getDashboardData()
    const data = res.data
    statCards.value.find(c => c.key === 'species')!.value = String(data.totalSpecies)
    statCards.value.find(c => c.key === 'observation')!.value = String(data.totalObservations)
    statCards.value.find(c => c.key === 'ai')!.value = String(data.totalRecognitions)
    statCards.value.find(c => c.key === 'users')!.value = String(data.totalUsers)
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
  }
})

/* ── 近期活动 ── */
const recentActivities = [
  { species: '绿海龟 Chelonia mydas', observer: '张三', date: '今天 14:30', type: '目击', color: '#2d8a5e', tagColor: '#2d8a5e', tagBg: '#e8f5ee' },
  { species: '中华白海豚 Sousa chinensis', observer: '李四', date: '今天 10:15', type: '声学', color: '#1a6b8a', tagColor: '#1a6b8a', tagBg: '#e8f4f8' },
  { species: '蓝珊瑚 Heliopora coerulea', observer: '王五', date: '昨天', type: '采样', color: '#cc8a30', tagColor: '#cc8a30', tagBg: '#fef4ee' },
  { species: '儒艮 Dugong dugon', observer: '赵六', date: '昨天', type: '目击', color: '#2d8a5e', tagColor: '#2d8a5e', tagBg: '#e8f5ee' },
  { species: '鲸鲨 Rhincodon typus', observer: '张三', date: '3 天前', type: '影像', color: '#7c5cbf', tagColor: '#7c5cbf', tagBg: '#f0eefa' },
]

/* ── 快捷操作 ── */
const quickActions = [
  { label: 'AI 识别', icon: '📷', color: '#1a6b8a', route: '/ai/recognize' },
  { label: '智能问答', icon: '💬', color: '#e07850', route: '/ai/chat' },
  { label: '分布地图', icon: '🗺', color: '#2d8a5e', route: '/visualization/map' },
  { label: '社区动态', icon: '👥', color: '#7c5cbf', route: '/community/feed' },
]

/* ── 物种聚焦 ── */
const spotlightSpecies = {
  name: '中华白海豚',
  sciName: 'Sousa chinensis',
  emoji: '🐬',
  status: '易危 VU',
  statusColor: '#cc8a30',
  statusBg: '#fef4ee'
}
</script>

<style scoped lang="scss">
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

/* ══════ 问候 ══════ */
.greeting {
  margin-bottom: 32px;
}

.greeting-title {
  font-size: 26px;
  font-weight: 600;
  color: var(--neutral-800);
  letter-spacing: -0.025em;
  margin-bottom: 4px;
}

.greeting-sub {
  font-size: 14px;
  color: var(--neutral-400);
  font-weight: 400;
}

/* ══════ 统计 Bento ══════ */
.stats-bento {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.stat-tile {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100);
  padding: 20px;
  transition: box-shadow 0.25s var(--ease-out), transform 0.25s var(--ease-out);
  animation: fadeSlideUp 0.5s var(--ease-out) both;

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }

  &.tile-wide {
    grid-column: span 2;
  }
}

.tile-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tile-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tile-icon {
  font-size: 22px;
}

.tile-data {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.tile-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--neutral-800);
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.tile-label {
  font-size: 13px;
  color: var(--neutral-400);
  margin-top: 3px;
  font-weight: 500;
}

.tile-trend {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  white-space: nowrap;
  flex-shrink: 0;

  &.up {
    color: var(--success);
    background: rgba(45, 138, 94, 0.08);
  }

  &.down {
    color: var(--danger);
    background: rgba(196, 53, 53, 0.08);
  }
}

/* ══════ 主内容网格 ══════ */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: start;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ══════ 面板通用 ══════ */
.panel {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100);
  padding: 20px;
  animation: fadeSlideUp 0.5s var(--ease-out) both;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--neutral-700);
  letter-spacing: -0.01em;
}

.panel-link {
  font-size: 13px;
  color: var(--primary-main);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  padding: 0;
  transition: color 0.15s;

  &:hover {
    color: var(--primary-light);
  }
}

/* ══════ 近期观测 ══════ */
.panel-activity {
  animation-delay: 0.15s;
}

.activity-list {
  display: flex;
  flex-direction: column;
}

.activity-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid var(--neutral-75);
  animation: fadeSlideUp 0.4s var(--ease-out) both;
  transition: background 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--neutral-25);
    margin: 0 -12px;
    padding-left: 12px;
    padding-right: 12px;
    border-radius: var(--radius-sm);
  }
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.activity-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--neutral-700);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-meta {
  font-size: 12px;
  color: var(--neutral-400);
}

.activity-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}

.panel-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--neutral-400);
  font-size: 14px;
}

/* ══════ 快捷操作 ══════ */
.panel-actions {
  animation-delay: 0.2s;
}

.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
  background: var(--neutral-50);
  border: 1px solid var(--neutral-100);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s var(--ease-out);

  &:hover {
    background: var(--surface-card);
    border-color: var(--neutral-200);
    box-shadow: var(--shadow-sm);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

.action-icon {
  font-size: 24px;
  line-height: 1;
}

.action-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--neutral-600);
}

/* ══════ 物种聚焦 ══════ */
.panel-spotlight {
  animation-delay: 0.25s;
}

.spotlight-card {
  display: flex;
  align-items: center;
  gap: 18px;
}

.spotlight-visual {
  flex-shrink: 0;
}

.spotlight-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--gradient-mist);
  display: flex;
  align-items: center;
  justify-content: center;
}

.spotlight-emoji {
  font-size: 36px;
}

.spotlight-info {
  flex: 1;
  min-width: 0;
}

.spotlight-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: 2px;
  letter-spacing: -0.01em;
}

.spotlight-sci {
  font-size: 13px;
  color: var(--neutral-400);
  font-style: italic;
  margin-bottom: 8px;
}

.spotlight-status {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  letter-spacing: 0.03em;
}

/* ══════ 系统信息 ══════ */
.panel-sys {
  padding: 16px 20px;
  animation-delay: 0.3s;
}

.sys-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;

  &:not(:last-child) {
    border-bottom: 1px solid var(--neutral-75);
  }
}

.sys-label {
  font-size: 12px;
  color: var(--neutral-400);
  font-weight: 500;
}

.sys-value {
  font-size: 12px;
  color: var(--neutral-500);
  font-weight: 500;
}

/* ══════ 响应式 ══════ */
@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .side-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }
}

@media (max-width: 768px) {
  .stats-bento {
    grid-template-columns: 1fr 1fr;
  }
  .stat-tile.tile-wide {
    grid-column: span 2;
  }
  .side-column {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-bento {
    grid-template-columns: 1fr;
  }
  .stat-tile.tile-wide {
    grid-column: span 1;
  }
  .greeting-title {
    font-size: 22px;
  }
}
</style>
