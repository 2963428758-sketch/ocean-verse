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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const familyChart = ref<HTMLElement>()
const iucnChart = ref<HTMLElement>()

onMounted(() => {
  // 科分布图
  if (familyChart.value) {
    const chart = echarts.init(familyChart.value)
    chart.setOption({
      title: { text: '物种按科分布', left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: [
          { value: 35, name: 'Cheloniidae' },
          { value: 28, name: 'Syngnathidae' },
          { value: 22, name: 'Delphinidae' },
          { value: 18, name: 'Scombridae' },
          { value: 15, name: '其他' }
        ]
      }]
    })
  }
  // IUCN 状态分布
  if (iucnChart.value) {
    const chart = echarts.init(iucnChart.value)
    chart.setOption({
      title: { text: 'IUCN 保护等级分布', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['CR', 'EN', 'VU', 'NT', 'LC', 'DD'] },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: [
          { value: 5, itemStyle: { color: '#f56c6c' } },
          { value: 12, itemStyle: { color: '#e6a23c' } },
          { value: 23, itemStyle: { color: '#409eff' } },
          { value: 18, itemStyle: { color: '#67c23a' } },
          { value: 56, itemStyle: { color: '#909399' } },
          { value: 14, itemStyle: { color: '#c0c4cc' } }
        ]
      }]
    })
  }
})
</script>
