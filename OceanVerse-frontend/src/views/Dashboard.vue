<template>
  <div class="dashboard">
    <!-- 问候 -->
    <header class="greeting">
      <h1 class="greeting-title">{{ greetingText }}，{{ userStore.username }}</h1>
      <p class="greeting-date">{{ todayStr }}</p>
    </header>

    <!-- 统计 -->
    <section class="stats-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-tile">
        <div class="stat-icon" :style="{ background: card.iconBg }">
          <span>{{ card.icon }}</span>
        </div>
        <div class="stat-data">
          <span class="stat-value">{{ card.value }}</span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
      </div>
    </section>

    <!-- 物种聚焦 (全宽轮播) -->
    <section class="panel spotlight-panel"
             @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
      <div class="spotlight-head">
        <h3 class="panel-title">物种聚焦</h3>
        <div v-if="spotlightSpecies.length > 1" class="carousel-nav">
          <span class="carousel-counter">
            {{ currentSpotlight + 1 }} / {{ spotlightSpecies.length }}
          </span>
          <div class="carousel-dots">
            <button
              v-for="(_, idx) in spotlightSpecies" :key="idx"
              :class="['carousel-dot', { active: idx === currentSpotlight }]"
              @click="goToSlide(idx)"
            ></button>
          </div>
        </div>
      </div>

      <div v-if="currentSpecies" class="carousel-body" :key="currentSpotlight">
        <!-- 主图 -->
        <div class="carousel-main">
          <div v-if="currentSpecies.images.length > 0" class="carousel-image" @click="nextImage">
            <img
              :src="currentSpecies.images[currentImageIndex]"
              :alt="currentSpecies.chineseName"
              :key="currentImageIndex"
            />
          </div>
          <div v-else class="carousel-fallback">
            <span>{{ currentSpecies.chineseName?.[0] || '?' }}</span>
          </div>
          <div v-if="currentSpecies.images.length > 1" class="carousel-thumbs">
            <img
              v-for="(url, idx) in currentSpecies.images.slice(0, 3)"
              :key="idx"
              :src="url"
              :class="{ active: idx === currentImageIndex }"
              :alt="''"
              @click.stop="switchImage(idx)"
            />
          </div>
        </div>

        <!-- 物种信息 -->
        <div class="carousel-info">
          <h4 class="carousel-name">{{ currentSpecies.chineseName }}</h4>
          <p class="carousel-sci">{{ currentSpecies.scientificName }}</p>
          <span class="carousel-badge" :style="{
            color: currentSpecies.badgeColor,
            background: currentSpecies.badgeBg
          }">
            {{ currentSpecies.statusLabel }}
          </span>

          <div class="carousel-meta">
            <span v-if="currentSpecies.family" class="meta-item">
              <span class="meta-label">科</span>
              {{ currentSpecies.family }}
            </span>
            <span v-if="currentSpecies.orderName" class="meta-item">
              <span class="meta-label">目</span>
              {{ currentSpecies.orderName }}
            </span>
            <span v-if="currentSpecies.protectionLevel" class="meta-item">
              <span class="meta-label">保护等级</span>
              国家{{ currentSpecies.protectionLevel }}级
            </span>
          </div>

          <p v-if="currentSpecies.description" class="carousel-desc">
            {{ currentSpecies.description }}
          </p>

          <button class="carousel-link" @click="openSpecies">
            查看详情
          </button>
        </div>
      </div>

      <div v-else class="carousel-empty">
        <p>暂无物种数据</p>
      </div>
    </section>

    <!-- 近期观测 -->
    <section class="panel activity-panel">
      <div class="panel-head">
        <h3 class="panel-title">近期观测</h3>
        <button class="panel-link" @click="$router.push('/observation/list')">查看全部</button>
      </div>
      <div class="activity-list">
        <div v-for="(item, i) in recentActivities" :key="i" class="activity-row">
          <div class="activity-dot" :style="{ background: item.tagColor }"></div>
          <div class="activity-info">
            <span class="activity-name">{{ item.species }}</span>
            <span class="activity-meta">{{ item.observer }}<span class="meta-dot">&middot;</span>{{ item.date }}</span>
          </div>
          <span class="activity-tag" :style="{ color: item.tagColor, background: item.tagBg }">
            {{ item.type }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardData } from '@/api/visual'
import { getSpeciesList, getSpeciesMedia } from '@/api/species'

const userStore = useUserStore()
const router = useRouter()

/* -- 问候 -- */
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

/* -- 统计卡片 -- */
const statCards = ref([
  { key: 'species', label: '物种总数', value: '-', icon: '🐋', iconBg: 'linear-gradient(135deg, #e8f4f8, #d0eaf3)' },
  { key: 'observation', label: '观测记录', value: '-', icon: '🔭', iconBg: 'linear-gradient(135deg, #fef4ee, #fce6d6)' },
  { key: 'posts', label: '社区帖子', value: '-', icon: '📝', iconBg: 'linear-gradient(135deg, #f0eefa, #e2dcf5)' },
  { key: 'users', label: '用户总数', value: '-', icon: '👥', iconBg: 'linear-gradient(135deg, #e8f5ee, #d2ebdc)' }
])

/* -- 近期活动 -- */
const recentActivities = [
  { species: '绿海龟 Chelonia mydas', observer: '张三', date: '今天 14:30', type: '目击', tagColor: '#2d8a5e', tagBg: '#e8f5ee' },
  { species: '中华白海豚 Sousa chinensis', observer: '李四', date: '今天 10:15', type: '声学', tagColor: '#1a6b8a', tagBg: '#e8f4f8' },
  { species: '蓝珊瑚 Heliopora coerulea', observer: '王五', date: '昨天', type: '采样', tagColor: '#cc8a30', tagBg: '#fef4ee' },
  { species: '儒艮 Dugong dugon', observer: '赵六', date: '昨天', type: '目击', tagColor: '#2d8a5e', tagBg: '#e8f5ee' },
  { species: '鲸鲨 Rhincodon typus', observer: '张三', date: '3 天前', type: '影像', tagColor: '#7c5cbf', tagBg: '#f0eefa' },
]

/* -- 物种轮播 -- */
const spotlightSpecies = ref<any[]>([])
const currentSpotlight = ref(0)
let carouselTimer: ReturnType<typeof setInterval> | null = null

const IUCN_MAP: Record<string, { label: string; color: string; bg: string }> = {
  CR: { label: '极危 CR', color: '#c43535', bg: '#fdf0f0' },
  EN: { label: '濒危 EN', color: '#d4603a', bg: '#fef2ec' },
  VU: { label: '易危 VU', color: '#cc8a30', bg: '#fef4ee' },
  NT: { label: '近危 NT', color: '#6a9c42', bg: '#f2f7ed' },
  LC: { label: '无危 LC', color: '#2d8a5e', bg: '#e8f5ee' },
  EX: { label: '灭绝 EX', color: '#666', bg: '#f0f0f0' },
  EW: { label: '野灭 EW', color: '#666', bg: '#f0f0f0' },
}

const currentSpecies = computed(() => spotlightSpecies.value[currentSpotlight.value] || null)
const currentImageIndex = ref(0)

const switchImage = (idx: number) => {
  currentImageIndex.value = idx
}

const nextImage = () => {
  if (!currentSpecies.value) return
  const len = currentSpecies.value.images.length
  if (len > 1) currentImageIndex.value = (currentImageIndex.value + 1) % len
}

const openSpecies = () => {
  if (currentSpecies.value) {
    router.push(`/species/detail/${currentSpecies.value.id}`)
  }
}

const nextSlide = () => {
  if (spotlightSpecies.value.length > 1) {
    currentSpotlight.value = (currentSpotlight.value + 1) % spotlightSpecies.value.length
    currentImageIndex.value = 0
  }
}

const goToSlide = (idx: number) => {
  currentSpotlight.value = idx
  currentImageIndex.value = 0
  restartCarousel()
}

const startCarousel = () => {
  carouselTimer = setInterval(nextSlide, 5000)
}

const pauseCarousel = () => {
  if (carouselTimer) clearInterval(carouselTimer)
}

const resumeCarousel = () => {
  startCarousel()
}

const restartCarousel = () => {
  pauseCarousel()
  startCarousel()
}

const truncate = (text: string, max: number) => {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

/* -- 初始化 -- */
onMounted(async () => {
  // 统计卡片
  try {
    const res = await getDashboardData()
    const data = res.data
    statCards.value.find(c => c.key === 'species')!.value = String(data.totalSpecies)
    statCards.value.find(c => c.key === 'observation')!.value = String(data.totalObservations)
    statCards.value.find(c => c.key === 'posts')!.value = String(data.totalPosts)
    statCards.value.find(c => c.key === 'users')!.value = String(data.totalUsers)
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
  }

  // 物种轮播 - 随机抽取10个
  try {
    const countRes = await getSpeciesList({ page: 1, size: 1 })
    const total = countRes.data?.total || 0
    const maxPage = Math.max(1, Math.ceil(total / 10))
    const randomPage = Math.floor(Math.random() * maxPage) + 1
    const res = await getSpeciesList({ page: randomPage, size: 10 })
    const records = res.data?.records || []
    const mediaPromises = records.map((s: any) =>
      getSpeciesMedia(s.id).then(r => {
        const media = r.data || []
        const primary = media.find((m: any) => m.isPrimary === 1) || media[0]
        const urls = media
          .filter((m: any) => m.mediaType === 'IMAGE')
          .map((m: any) => m.fileUrl)
        const primaryUrl = primary?.fileUrl || null
        if (primaryUrl && !urls.includes(primaryUrl)) urls.unshift(primaryUrl)
        return urls.slice(0, 3)
      }).catch(() => [])
    )
    const images = await Promise.all(mediaPromises)
    spotlightSpecies.value = records.map((s: any, i: number) => {
      const badge = IUCN_MAP[s.iucnStatus] || { label: s.iucnStatus || '', color: '#999', bg: '#f5f5f5' }
      return {
        id: s.id,
        chineseName: s.chineseName,
        scientificName: s.scientificName,
        family: s.family || null,
        orderName: s.orderName || null,
        protectionLevel: s.protectionLevel || null,
        description: truncate(s.description || '', 80),
        images: images[i],
        statusLabel: badge.label,
        badgeColor: badge.color,
        badgeBg: badge.bg,
      }
    })
    if (spotlightSpecies.value.length > 1) startCarousel()
  } catch (e) {
    console.error('获取物种数据失败', e)
  }
})

onUnmounted(() => {
  if (carouselTimer) clearInterval(carouselTimer)
})
</script>

<style scoped lang="scss">
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.35s ease;
}

/* ══════ 问候 ══════ */
.greeting {
  margin-bottom: 36px;
}

.greeting-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--neutral-800);
  letter-spacing: -0.025em;
  line-height: 1.2;
  margin-bottom: 6px;
}

.greeting-date {
  font-size: 14px;
  color: var(--neutral-400);
  font-weight: 400;
}

/* ══════ 统计卡片 ══════ */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat-tile {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100);
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow 0.25s var(--ease-out), transform 0.25s var(--ease-out);
  animation: fadeSlideUp 0.45s var(--ease-out) both;

  &:nth-child(1) { animation-delay: 0s; }
  &:nth-child(2) { animation-delay: 0.05s; }
  &:nth-child(3) { animation-delay: 0.1s; }
  &:nth-child(4) { animation-delay: 0.15s; }

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  line-height: 1;
}

.stat-data {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--neutral-800);
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: var(--neutral-400);
  margin-top: 3px;
  font-weight: 500;
}

/* ══════ 物种聚焦 (全宽轮播) ══════ */
.spotlight-panel {
  margin-bottom: 20px;
  animation-delay: 0.12s;
}

.spotlight-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.carousel-nav {
  display: flex;
  align-items: center;
  gap: 14px;
}

.carousel-counter {
  font-size: 12px;
  color: var(--neutral-400);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.carousel-dots {
  display: flex;
  gap: 6px;
}

.carousel-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--neutral-200);
  border: none;
  padding: 0;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;

  &.active {
    background: var(--primary-main);
    transform: scale(1.3);
  }

  &:hover:not(.active) {
    background: var(--neutral-300);
  }
}

.carousel-body {
  display: flex;
  gap: 32px;
  animation: fadeIn 0.3s ease;
}

.carousel-main {
  flex-shrink: 0;
}

.carousel-image {
  width: 260px;
  height: 200px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s var(--ease-out);
  }

  &:hover img {
    transform: scale(1.03);
  }
}

.carousel-fallback {
  width: 260px;
  height: 200px;
  border-radius: var(--radius-lg);
  background: var(--gradient-mist);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  span {
    font-size: 64px;
    font-weight: 700;
    color: var(--primary-main);
    opacity: 0.25;
  }
}

.carousel-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;

  img {
    width: 44px;
    height: 44px;
    border-radius: var(--radius-xs);
    object-fit: cover;
    opacity: 0.5;
    transition: opacity 0.2s;
    cursor: pointer;

    &.active,
    &:hover {
      opacity: 1;
    }
  }
}

.carousel-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.carousel-name {
  font-size: 22px;
  font-weight: 600;
  color: var(--neutral-800);
  letter-spacing: -0.02em;
  margin-bottom: 4px;
}

.carousel-sci {
  font-size: 14px;
  color: var(--neutral-400);
  font-style: italic;
  margin-bottom: 12px;
}

.carousel-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  letter-spacing: 0.03em;
  width: fit-content;
  margin-bottom: 14px;
}

.carousel-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;
  margin-bottom: 14px;
}

.meta-item {
  font-size: 13px;
  color: var(--neutral-600);
}

.meta-label {
  font-size: 12px;
  color: var(--neutral-400);
  margin-right: 6px;
}

.carousel-desc {
  font-size: 13px;
  color: var(--neutral-500);
  line-height: 1.65;
  margin-bottom: 16px;
  flex: 1;
}

.carousel-link {
  font-size: 13px;
  color: var(--primary-main);
  background: none;
  border: 1px solid var(--primary-main);
  border-radius: var(--radius-sm);
  padding: 6px 16px;
  cursor: pointer;
  font-weight: 500;
  width: fit-content;
  transition: all 0.2s var(--ease-out);

  &:hover {
    background: var(--primary-main);
    color: #fff;
  }
}

.carousel-empty {
  padding: 40px 0;
  text-align: center;

  p {
    color: var(--neutral-400);
    font-size: 13px;
  }
}

/* ══════ 面板通用 ══════ */
.panel {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100);
  padding: 22px;
  animation: fadeSlideUp 0.45s var(--ease-out) both;
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
.activity-panel {
  animation-delay: 0.18s;
}

.activity-list {
  display: flex;
  flex-direction: column;
}

.activity-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 0;
  border-bottom: 1px solid var(--neutral-75);
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

.meta-dot {
  margin: 0 5px;
  opacity: 0.5;
}

.activity-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}

/* ══════ 响应式 ══════ */
@media (max-width: 1100px) {
  .carousel-image,
  .carousel-fallback {
    width: 220px;
    height: 170px;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
  .carousel-body {
    flex-direction: column;
    gap: 20px;
  }
  .carousel-image,
  .carousel-fallback {
    width: 100%;
    height: 200px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .greeting-title {
    font-size: 22px;
  }
  .carousel-name {
    font-size: 18px;
  }
}
</style>
