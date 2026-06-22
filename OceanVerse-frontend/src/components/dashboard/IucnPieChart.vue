<template>
  <section class="panel chart-panel">
    <div class="panel-head">
      <h3 class="panel-title">
        <el-icon class="panel-title-icon"><PieChart /></el-icon>
        IUCN 保护状态
      </h3>
    </div>
    <div v-if="loading" class="chart-skeleton">
      <div class="skel-circle"></div>
    </div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import { PieChart } from '@element-plus/icons-vue'
import { getSpeciesByIucn } from '@/api/visual'

const props = defineProps<{ loading: boolean }>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const IUCN_COLORS: Record<string, string> = {
  CR: '#c43535', EN: '#d4603a', VU: '#cc8a30', NT: '#6a9c42',
  LC: '#2d8a5e', DD: '#948f86', EX: '#666666', EW: '#888888', NE: '#b8b4ac',
}

const IUCN_LABELS: Record<string, string> = {
  CR: '极危', EN: '濒危', VU: '易危', NT: '近危',
  LC: '无危', DD: '数据不足', EX: '灭绝', EW: '野外灭绝', NE: '未评估',
}

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
}

function updateChart(data: { iucn_status: string; count: number }[]) {
  if (!chartInstance) return
  const chartData = data.map(d => ({
    name: IUCN_LABELS[d.iucn_status] || d.iucn_status,
    value: d.count,
    itemStyle: { color: IUCN_COLORS[d.iucn_status] || '#948f86' },
  }))

  chartInstance.setOption({
    tooltip: {
      trigger: 'item', backgroundColor: '#fff', borderColor: '#e8e6e1',
      textStyle: { color: '#36332c', fontSize: 12 },
      formatter: (params: any) => `${params.name}: ${params.value} (${params.percent}%)`,
    },
    legend: { show: false },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '50%'],
      avoidLabelOverlap: true,
      label: {
        show: true,
        formatter: '{b}\n{d}%',
        fontSize: 11,
        color: '#6e695f',
        lineHeight: 16,
      },
      labelLine: {
        show: true,
        length: 12,
        length2: 10,
        smooth: true,
        lineStyle: { color: '#d5d2cb', width: 1 },
      },
      emphasis: {
        label: { fontSize: 13, fontWeight: 600, color: '#36332c' },
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.08)' },
      },
      data: chartData,
    }],
  })
}

const resizeObserver = ref<ResizeObserver>()

async function loadIucnData() {
  try {
    const res: any = await getSpeciesByIucn()
    updateChart(res.data || [])
  } catch { /* silent */ }
}

watch(() => props.loading, (isLoading) => {
  if (!isLoading) { setTimeout(() => { initChart(); loadIucnData() }, 0) }
})

onMounted(() => {
  if (!props.loading && chartRef.value) { initChart(); loadIucnData() }
  resizeObserver.value = new ResizeObserver(() => { chartInstance?.resize() })
  if (chartRef.value) resizeObserver.value.observe(chartRef.value)
})

onBeforeUnmount(() => { chartInstance?.dispose(); resizeObserver.value?.disconnect() })
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
  display: flex; align-items: center; justify-content: center;
}
.skel-circle {
  width: 140px; height: 140px; border-radius: 50%;
  border: 20px solid var(--neutral-100);
  animation: shimmer 1.5s infinite;
}
.panel {
  background: var(--surface-card); border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-100); padding: 22px;
  animation: fadeSlideUp 0.45s var(--ease-out) both;
}
@keyframes shimmer { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }
</style>
