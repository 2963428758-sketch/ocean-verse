<template>
  <header class="greeting">
    <div class="greeting-accent"></div>
    <h1 class="greeting-title">{{ greetingText }}，{{ username }}</h1>
    <p class="greeting-date">{{ todayStr }}</p>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  username: string
}>()

// 页面挂载时固定，跨天刷新页面即可
const now = new Date()

const greetingText = (() => {
  const hr = now.getHours()
  if (hr < 6) return '夜深了'
  if (hr < 12) return '早上好'
  if (hr < 14) return '中午好'
  if (hr < 18) return '下午好'
  return '晚上好'
})()

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const todayStr = `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日  星期${weekdays[now.getDay()]}`
</script>

<style scoped lang="scss">
.greeting {
  position: relative;
  margin-bottom: 32px;
  padding-left: 16px;
}

.greeting-accent {
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--primary-main), var(--primary-glow));
}

.greeting-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--neutral-800);
  letter-spacing: -0.03em;
  line-height: 1.25;
  margin-bottom: 4px;
}

.greeting-date {
  font-size: 13px;
  color: var(--neutral-400);
  font-weight: 400;
}

@media (max-width: 480px) {
  .greeting-title {
    font-size: 20px;
  }
}
</style>
