<template>
  <section
    class="panel spotlight-panel"
    role="region"
    aria-roledescription="轮播"
    aria-label="物种聚焦"
    @mouseenter="pauseCarousel"
    @mouseleave="resumeCarousel"
  >
    <div class="spotlight-head">
      <h3 class="panel-title">
        <el-icon class="panel-title-icon"><View /></el-icon>
        物种聚焦
      </h3>
      <div v-if="speciesList.length > 1" class="carousel-nav">
        <span class="carousel-counter">
          {{ currentIndex + 1 }} / {{ speciesList.length }}
        </span>
        <div class="carousel-dots">
          <button
            v-for="(_, idx) in speciesList"
            :key="idx"
            :class="['carousel-dot', { active: idx === currentIndex }]"
            :aria-label="`跳转到第 ${idx + 1} 个物种`"
            :aria-current="idx === currentIndex ? 'true' : undefined"
            @click="goToSlide(idx)"
          ></button>
        </div>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="carousel-body">
      <div class="carousel-main">
        <div class="skel-img"></div>
      </div>
      <div class="carousel-info">
        <div class="skel-bar" style="width: 60%; height: 22px"></div>
        <div class="skel-bar" style="width: 40%; height: 14px; margin-top: 8px"></div>
        <div class="skel-bar" style="width: 30%; height: 20px; margin-top: 14px; border-radius: 20px"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="speciesList.length === 0" class="carousel-empty">
      <p>暂无物种数据</p>
    </div>

    <!-- 轮播内容 -->
    <div v-else-if="currentSpecies" class="carousel-body" :key="currentIndex" aria-live="polite">
      <!-- 主图 -->
      <div class="carousel-main">
        <div v-if="currentSpecies.images.length > 0" class="carousel-image" @click="nextImage">
          <img
            :src="currentSpecies.images[currentImageIndex]"
            :alt="currentSpecies.chineseName"
            :key="currentImageIndex"
            loading="lazy"
            decoding="async"
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
            :alt="`${currentSpecies.chineseName} 图片 ${idx + 1}`"
            loading="lazy"
            decoding="async"
            @click.stop="switchImage(idx)"
          />
        </div>
      </div>

      <!-- 物种信息 -->
      <div class="carousel-info">
        <h4 class="carousel-name">{{ currentSpecies.chineseName }}</h4>
        <p class="carousel-sci">{{ currentSpecies.scientificName }}</p>
        <span
          v-if="currentBadge"
          class="carousel-badge"
          :style="{ color: currentBadge.color, background: currentBadge.bg }"
        >
          {{ currentBadge.label }}
        </span>

        <div class="carousel-meta">
          <span v-if="currentSpecies.family" class="meta-item">
            <span class="meta-label">科</span>{{ currentSpecies.family }}
          </span>
          <span v-if="currentSpecies.orderName" class="meta-item">
            <span class="meta-label">目</span>{{ currentSpecies.orderName }}
          </span>
          <span v-if="currentSpecies.protectionLevel" class="meta-item">
            <span class="meta-label">保护等级</span>国家{{ currentSpecies.protectionLevel }}级
          </span>
        </div>

        <p v-if="currentSpecies.description" class="carousel-desc">
          {{ currentSpecies.description }}
        </p>

        <button class="carousel-link" @click="openSpecies">查看详情</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { View } from '@element-plus/icons-vue'
import { getSpeciesList, getSpeciesMedia } from '@/api/species'
import type { Species, SpeciesMedia } from '@/types'

const router = useRouter()

defineProps<{
  loading: boolean
}>()

interface SpotlightSpecies {
  id: number
  chineseName: string
  scientificName: string
  family: string | null
  orderName: string | null
  protectionLevel: string | null
  iucnStatus: string
  description: string
  images: string[]
}

const IUCN_MAP: Record<string, { label: string; color: string; bg: string }> = {
  CR: { label: '极危 CR', color: '#c43535', bg: '#fdf0f0' },
  EN: { label: '濒危 EN', color: '#d4603a', bg: '#fef2ec' },
  VU: { label: '易危 VU', color: '#cc8a30', bg: '#fef4ee' },
  NT: { label: '近危 NT', color: '#6a9c42', bg: '#f2f7ed' },
  LC: { label: '无危 LC', color: '#2d8a5e', bg: '#e8f5ee' },
  EX: { label: '灭绝 EX', color: '#666', bg: '#f0f0f0' },
  EW: { label: '野灭 EW', color: '#666', bg: '#f0f0f0' },
}

const speciesList = ref<SpotlightSpecies[]>([])
const currentIndex = ref(0)
const currentImageIndex = ref(0)
let carouselTimer: ReturnType<typeof setInterval> | null = null

const currentSpecies = computed(() => speciesList.value[currentIndex.value] || null)
const currentBadge = computed(() => {
  if (!currentSpecies.value) return null
  return IUCN_MAP[currentSpecies.value.iucnStatus] || null
})

watch(currentIndex, () => { currentImageIndex.value = 0 })

const switchImage = (idx: number) => { currentImageIndex.value = idx }

const nextImage = () => {
  if (!currentSpecies.value) return
  const len = currentSpecies.value.images.length
  if (len > 1) currentImageIndex.value = (currentImageIndex.value + 1) % len
}

const openSpecies = () => {
  if (currentSpecies.value) {
    router.push({ name: 'SpeciesDetail', params: { id: currentSpecies.value.id } })
  }
}

const nextSlide = () => {
  if (speciesList.value.length > 1) {
    currentIndex.value = (currentIndex.value + 1) % speciesList.value.length
  }
}

const goToSlide = (idx: number) => {
  currentIndex.value = idx
  restartCarousel()
}

const startCarousel = () => { carouselTimer = setInterval(nextSlide, 5000) }
const pauseCarousel = () => { if (carouselTimer) { clearInterval(carouselTimer); carouselTimer = null } }
const resumeCarousel = () => { if (speciesList.value.length > 1) startCarousel() }
const restartCarousel = () => { pauseCarousel(); startCarousel() }

const handleVisibility = () => { document.hidden ? pauseCarousel() : resumeCarousel() }

async function loadSpotlight() {
  const countRes = await getSpeciesList({ page: 1, size: 1 })
  const total = (countRes as any).data?.total || 0
  if (total === 0) return

  const maxPage = Math.max(1, Math.ceil(total / 10))
  const randomPage = Math.floor(Math.random() * maxPage) + 1
  const res = await getSpeciesList({ page: randomPage, size: 10 })
  const records: Species[] = (res as any).data?.records || []

  const mediaPromises = records.map((s) =>
    getSpeciesMedia(s.id!)
      .then((r) => {
        const media: SpeciesMedia[] = (r as any).data || []
        const primary = media.find((m) => m.isPrimary === 1) || media[0]
        const urls = media.filter((m) => m.mediaType === 'IMAGE').map((m) => m.fileUrl)
        const primaryUrl = primary?.fileUrl || null
        if (primaryUrl && !urls.includes(primaryUrl)) urls.unshift(primaryUrl)
        return urls.slice(0, 3)
      })
      .catch(() => [] as string[])
  )
  const images = await Promise.all(mediaPromises)

  speciesList.value = records.map((s, i) => ({
    id: s.id!,
    chineseName: s.chineseName || '',
    scientificName: s.scientificName || '',
    family: s.family || null,
    orderName: s.orderName || null,
    protectionLevel: s.protectionLevel || null,
    iucnStatus: s.iucnStatus || '',
    description: s.description || '',
    images: images[i],
  }))

  if (speciesList.value.length > 1) startCarousel()
}

defineExpose({ loadSpotlight })

onMounted(() => { document.addEventListener('visibilitychange', handleVisibility) })
onUnmounted(() => { pauseCarousel(); document.removeEventListener('visibilitychange', handleVisibility) })
</script>

<style scoped lang="scss">
.spotlight-panel { animation-delay: 0.12s; }

.spotlight-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.carousel-nav { display: flex; align-items: center; gap: 14px; }

.carousel-counter {
  font-size: 12px;
  color: var(--neutral-400);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.carousel-dots { display: flex; gap: 6px; }

.carousel-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--neutral-200);
  border: none; padding: 0; cursor: pointer;
  transition: background 0.2s, transform 0.2s;
  &.active { background: var(--primary-main); transform: scale(1.3); }
  &:hover:not(.active) { background: var(--neutral-300); }
}

.carousel-body { display: flex; gap: 32px; animation: fadeIn 0.3s ease; }
.carousel-main { flex-shrink: 0; }

.carousel-image {
  width: 260px; height: 200px;
  border-radius: var(--radius-lg);
  overflow: hidden; cursor: pointer;
  img {
    width: 100%; height: 100%; object-fit: cover;
    transition: transform 0.4s var(--ease-out);
  }
  &:hover img { transform: scale(1.03); }
}

.carousel-fallback {
  width: 260px; height: 200px;
  border-radius: var(--radius-lg);
  background: var(--gradient-mist);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  span { font-size: 64px; font-weight: 700; color: var(--primary-main); opacity: 0.25; }
}

.carousel-thumbs {
  display: flex; gap: 8px; margin-top: 10px;
  img {
    width: 44px; height: 44px;
    border-radius: var(--radius-xs);
    object-fit: cover; opacity: 0.5;
    transition: opacity 0.2s; cursor: pointer;
    &.active, &:hover { opacity: 1; }
  }
}

.carousel-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column;
}

.carousel-name {
  font-size: 22px; font-weight: 600;
  color: var(--neutral-800);
  letter-spacing: -0.02em; margin-bottom: 4px;
}

.carousel-sci {
  font-size: 14px; color: var(--neutral-400);
  font-style: italic; margin-bottom: 12px;
}

.carousel-badge {
  display: inline-block; font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: 20px;
  letter-spacing: 0.03em; width: fit-content; margin-bottom: 14px;
}

.carousel-meta { display: flex; flex-wrap: wrap; gap: 6px 20px; margin-bottom: 14px; }
.meta-item { font-size: 13px; color: var(--neutral-600); }
.meta-label { font-size: 12px; color: var(--neutral-400); margin-right: 6px; }

.carousel-desc {
  font-size: 13px; color: var(--neutral-500);
  line-height: 1.65; margin-bottom: 16px; flex: 1;
  display: -webkit-box; -webkit-line-clamp: 3;
  -webkit-box-orient: vertical; overflow: hidden;
}

.carousel-link {
  font-size: 13px; color: var(--primary-main);
  background: none; border: 1px solid var(--primary-main);
  border-radius: var(--radius-sm); padding: 6px 16px;
  cursor: pointer; font-weight: 500; width: fit-content;
  transition: all 0.2s var(--ease-out);
  &:hover { background: var(--primary-main); color: #fff; }
}

.carousel-empty { padding: 40px 0; text-align: center; p { color: var(--neutral-400); font-size: 13px; } }

.panel {
  background: var(--surface-card); border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100); padding: 22px;
  animation: fadeSlideUp 0.45s var(--ease-out) both;
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

.skel-img {
  width: 260px; height: 200px;
  border-radius: var(--radius-lg);
  background: var(--neutral-100);
  animation: shimmer 1.5s infinite ease-in-out;
}

.skel-bar {
  border-radius: 4px; background: var(--neutral-100);
  animation: shimmer 1.5s infinite ease-in-out;
}

@keyframes shimmer { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }

@media (max-width: 1100px) {
  .carousel-image, .skel-img, .carousel-fallback { width: 220px; height: 170px; }
}

@media (max-width: 768px) {
  .carousel-body { flex-direction: column; gap: 20px; }
  .carousel-image, .skel-img, .carousel-fallback { width: 100%; height: 200px; }
}

@media (max-width: 480px) {
  .carousel-name { font-size: 18px; }
}
</style>
