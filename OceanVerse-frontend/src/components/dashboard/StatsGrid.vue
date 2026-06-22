<template>
  <section class="stats-grid" role="group" aria-label="平台统计数据">
    <template v-if="loading">
      <div v-for="i in 4" :key="i" class="stat-tile stat-skeleton">
        <div class="skel-icon"></div>
        <div class="skel-data">
          <div class="skel-bar skel-bar-lg"></div>
          <div class="skel-bar skel-bar-sm"></div>
        </div>
      </div>
    </template>

    <template v-else>
      <div
        v-for="(card, idx) in cards"
        :key="card.key"
        class="stat-tile"
        :style="{ '--accent': card.iconColor, animationDelay: `${idx * 0.05}s` }"
      >
        <div class="stat-icon" :style="{ background: card.iconBg }">
          <el-icon :size="22" :color="card.iconColor"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-data">
          <span class="stat-value">{{ formattedValue(card.value) }}</span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

export interface StatCard {
  key: string
  label: string
  value: string | number | null
  icon: Component
  iconColor: string
  iconBg: string
}

defineProps<{
  loading: boolean
  cards: StatCard[]
}>()

const formattedValue = (val: string | number | null) => {
  if (val === null || val === undefined) return '-'
  const num = Number(val)
  if (Number.isNaN(num)) return String(val)
  return num.toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
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

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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

/* 骨架屏 */
.stat-skeleton {
  &:hover {
    transform: none;
    box-shadow: none;
  }
}

.skel-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--neutral-100);
  animation: shimmer 1.5s infinite ease-in-out;
}

.skel-data {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skel-bar {
  border-radius: 4px;
  background: var(--neutral-100);
  animation: shimmer 1.5s infinite ease-in-out;
}

.skel-bar-lg {
  height: 28px;
  width: 70%;
}

.skel-bar-sm {
  height: 13px;
  width: 50%;
}

@keyframes shimmer {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
