<template>
  <div>
    <div class="page-header"><h2>数据统计</h2><p>海洋生物多样性数据可视化</p></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover"><div ref="familyChart" style="height:400px"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover"><div ref="iucnChart" style="height:400px"></div></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card shadow="hover"><div ref="trendChart" style="height:400px"></div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getSpeciesByFamily, getSpeciesByIucn, getObservationTrend } from '@/api/visual'

const familyChart = ref<HTMLElement>()
const iucnChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()

onMounted(async () => {
  // 科分布图 — 调用后端真实数据
  if (familyChart.value) {
    const chart = echarts.init(familyChart.value)
    const res = await getSpeciesByFamily()
    chart.setOption({
      title: { text: '物种按科分布', left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: res.data.map((item: any) => ({
          value: item.count,
          name: item.family
        }))
      }]
    })
  }
  // IUCN 状态分布 — 调用后端真实数据
  if (iucnChart.value) {
    const chart = echarts.init(iucnChart.value)
    const res = await getSpeciesByIucn()
    const colorMap: Record<string, string> = {
      CR: '#f56c6c', EN: '#e6a23c', VU: '#409eff',
      NT: '#67c23a', LC: '#909399', DD: '#c0c4cc'
    }
    chart.setOption({
      title: { text: 'IUCN 保护等级分布', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: res.data.map((item: any) => item.iucn_status)
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: res.data.map((item: any) => ({
          value: item.count,
          itemStyle: { color: colorMap[item.iucn_status] || '#909399' }
        }))
      }]
    })
  }
  // 观测趋势图 — 调用后端真实数据
  if (trendChart.value) {
    const chart = echarts.init(trendChart.value)
    const res = await getObservationTrend()
    chart.setOption({
      title: { text: '观测记录趋势', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: res.data.map((item: any) => item.period)
      },
      yAxis: { type: 'value', name: '观测次数' },
      series: [{
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.15)' },
        itemStyle: { color: '#409eff' },
        data: res.data.map((item: any) => item.count)
      }]
    })
  }
})
</script>
