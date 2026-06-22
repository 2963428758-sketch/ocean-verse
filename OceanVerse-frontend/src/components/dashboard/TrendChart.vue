<template>
  <section class="panel chart-panel">
    <div class="panel-head">
      <h3 class="panel-title">
        <el-icon class="panel-title-icon"><TrendCharts /></el-icon>
        观测趋势
      </h3>
    </div>
    <div v-if="loading" class="chart-skeleton">
      <div class="skel-bar" style="width: 100%; height: 100%"></div>
    </div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import { TrendCharts } from '@element-plus/icons-vue'
import { getObservationTrend } from '@/api/visual'

const props = defineProps<{ loading: boolean }>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
}

function updateChart(data: { period: string; count: number }[]) {
  if (!chartInstance) return
  const dates = data.map(d => d.period)
  const counts = data.map(d => d.count)

  chartInstance.setOption({
    grid: { top: 16, right: 12, bottom: 28, left: 40 },
    xAxis: {
      type: 'category', data: dates,
      axisLine: { lineStyle: { color: '#e8e6e1' } },
      axisLabel: { color: '#948f86', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0eee9' } },
      axisLabel: { color: '#948f86', fontSize: 11 },
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff', borderColor: '#e8e6e1',
      textStyle: { color: '#36332c', fontSize: 12 },
    },
    series: [{
      type: 'line', data: counts, smooth: true,
      symbol: 'circle', symbolSize: 6,
      itemStyle: { color: '#1a6b8a' },
      lineStyle: { width: 2.5, color: '#1a6b8a' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(26, 107, 138, 0.15)' },
          { offset: 1, color: 'rgba(26, 107, 138, 0.01)' },
        ]),
      },
    }],
  })
}

const resizeObserver = ref<ResizeObserver>()

async function loadTrend() {
  try {
    const res: any = await getObservationTrend({ period: 'monthly' })
    updateChart(res.data || [])
  } catch { /* silent */ }
}

watch(() => props.loading, (isLoading) => {
  if (!isLoading) {
    setTimeout(() => { initChart(); loadTrend() }, 0)
  }
})

onMounted(() => {
  if (!props.loading && chartRef.value) { initChart(); loadTrend() }
  resizeObserver.value = new ResizeObserver(() => { chartInstance?.resize() })
  if (chartRef.value) resizeObserver.value.observe(chartRef.value)
})

onBeforeUnmount(() => { chartInstance?.dispose(); resizeObserver.value?.disconnect() })

defineExpose({ loadTrend })
</script>

<style scoped lang="scss">
.chart-panel { }
.panel-head { margin-bottom: 12px; }
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
.chart-container { width: 100%; height: 200px; }
.chart-skeleton {
  width: 100%; height: 200px;
  .skel-bar { border-radius: var(--radius-sm); background: var(--neutral-100); animation: shimmer 1.5s infinite; }
}
.panel {
  background: var(--surface-card); border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100); padding: 22px;
  animation: fadeSlideUp 0.45s var(--ease-out) both;
}
@keyframes shimmer { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }
</style>
