<template>
  <div>
    <div class="page-header"><h2>数据统计</h2><p>海洋生物多样性数据可视化</p></div>

    <!-- 世界地图物种分布可视化 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card shadow="hover" :body-style="{ padding: 0 }">
          <div ref="mapChart" style="height:600px;background:#051b4a;border-radius:4px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 原有三个图表 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card shadow="hover" :body-style="{ padding: 0 }">
          <div ref="familyChart" style="height:400px;background:#051b4a;border-radius:4px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" :body-style="{ padding: 0 }">
          <div ref="iucnChart" style="height:400px;background:#051b4a;border-radius:4px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card shadow="hover" :body-style="{ padding: 0 }">
          <div ref="trendChart" style="height:400px;background:#051b4a;border-radius:4px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getSpeciesByFamily, getSpeciesByIucn, getObservationTrend, getSpeciesDistribution } from '@/api/visual'

const familyChart = ref<HTMLElement>()
const iucnChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()
const mapChart = ref<HTMLElement>()

const charts: echarts.ECharts[] = []

/* ---- IUCN 配色 ---- */
const iucnColors: Record<string, string> = {
  CR: '#f56c6c', EN: '#e6a23c', VU: '#409eff',
  NT: '#67c23a', LC: '#909399', DD: '#c0c4cc'
}

/* ---- 飞线目标点（太平洋中部） ---- */
const flyTarget: [number, number] = [160, 0]

/* ---- 加载世界 GeoJSON ---- */
async function loadWorldGeoJson(): Promise<any> {
  const urls = [
    'https://unpkg.com/echarts@4.9.0/map/json/world.json',
    'https://cdn.jsdelivr.net/npm/echarts@4.9.0/map/json/world.json'
  ]
  for (const url of urls) {
    try {
      const res = await fetch(url)
      if (res.ok) {
        const json = await res.json()
        echarts.registerMap('world', json)
        return json
      }
    } catch (e) {
      console.warn('GeoJSON 加载失败:', url, e)
    }
  }
  return null
}

/* ---- 按国家聚合分布数据 ---- */
function aggregateByCountry(distributions: any[], iucnFilter?: string) {
  const filtered = iucnFilter
    ? distributions.filter(d => d.iucn_status === iucnFilter)
    : distributions

  const countryMap = new Map<string, { count: number; lat: number; lng: number }>()
  filtered.forEach(d => {
    const key = d.country || d.region_name || '未知'
    if (!countryMap.has(key)) {
      countryMap.set(key, { count: 0, lat: d.latitude, lng: d.longitude })
    }
    const entry = countryMap.get(key)!
    entry.count++
    if (d.latitude && d.longitude) {
      entry.lat = d.latitude
      entry.lng = d.longitude
    }
  })

  return Array.from(countryMap.entries())
    .map(([name, data]) => ({ name, ...data }))
    .sort((a, b) => b.count - a.count)
}

/* ---- 构建每个 IUCN 等级的 timeline option ---- */
function buildTimelineOptions(distributions: any[]) {
  const iucnLevels = [
    { key: 'CR', label: '极危 CR' },
    { key: 'EN', label: '濒危 EN' },
    { key: 'VU', label: '易危 VU' },
    { key: 'NT', label: '近危 NT' },
    { key: 'LC', label: '无危 LC' }
  ]
  const colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399']

  return iucnLevels.map((level, idx) => {
    const levelData = distributions.filter(d => d.iucn_status === level.key && d.latitude && d.longitude)
    const countryData = aggregateByCountry(distributions, level.key)

    // 涟漪散点
    const scatterData = levelData.map(d => ({
      name: d.chinese_name || '未知物种',
      value: [d.longitude, d.latitude, d.species_id ? 1 : 0.5]
    }))

    // 按坐标去重聚合（同一地点多个物种）
    const pointMap = new Map<string, { lng: number; lat: number; count: number; names: string[] }>()
    levelData.forEach(d => {
      const key = `${d.latitude}_${d.longitude}`
      if (!pointMap.has(key)) {
        pointMap.set(key, { lng: d.longitude, lat: d.latitude, count: 0, names: [] })
      }
      const p = pointMap.get(key)!
      p.count++
      if (d.chinese_name) p.names.push(d.chinese_name)
    })
    const effectData = Array.from(pointMap.values()).map(p => ({
      name: p.names.join(', '),
      value: [p.lng, p.lat, p.count]
    }))

    // 飞线数据
    const linesData = effectData.map(p => [
      { coord: [p.value[0], p.value[1]] },
      { coord: flyTarget }
    ])

    // 柱状图数据
    const barNames = countryData.slice(0, 15).map(c => c.name).reverse()
    const barValues = countryData.slice(0, 15).map(c => c.count).reverse()

    return {
      title: [{
        text: level.label + ' 物种分布',
        left: 'center',
        top: 10,
        textStyle: { color: '#fff', fontSize: 18 }
      }, {
        text: `${levelData.length} 个分布点 · ${countryData.length} 个国家/地区`,
        left: 'center',
        top: 38,
        textStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 12 }
      }],
      yAxis: { data: barNames },
      series: [
        // 涟漪散点
        {
          type: 'effectScatter',
          coordinateSystem: 'geo',
          data: effectData,
          symbolSize: (val: any) => Math.max(8, val[2] * 6),
          showEffectOn: 'render',
          rippleEffect: { brushType: 'stroke', scale: 4 },
          label: { show: false },
          itemStyle: { color: colors[idx], shadowBlur: 10, shadowColor: colors[idx] },
          zlevel: 1
        },
        // 普通散点
        {
          type: 'scatter',
          coordinateSystem: 'geo',
          data: scatterData,
          symbolSize: 6,
          label: { show: false },
          itemStyle: { color: colors[idx], opacity: 0.7 }
        },
        // 飞线
        {
          type: 'lines',
          coordinateSystem: 'geo',
          zlevel: 2,
          effect: {
            show: true,
            period: 4,
            trailLength: 0.03,
            symbol: 'arrow',
            symbolSize: 4,
            color: colors[idx]
          },
          lineStyle: {
            color: colors[idx],
            width: 0.5,
            opacity: 0.4,
            curveness: 0.3
          },
          data: linesData
        },
        // 柱状图
        {
          type: 'bar',
          zlevel: 1.5,
          symbol: 'none',
          itemStyle: { color: colors[idx] },
          data: barValues
        }
      ]
    }
  })
}

onMounted(async () => {
  // 科分布饼图
  if (familyChart.value) {
    const chart = echarts.init(familyChart.value)
    charts.push(chart)
    const res = await getSpeciesByFamily()
    chart.setOption({
      title: { text: '物种按科分布', left: 'center', textStyle: { color: '#fff', fontSize: 16 } },
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(0,0,0,0.8)',
        borderColor: 'rgba(147,235,248,0.5)',
        textStyle: { color: '#fff' }
      },
      legend: {
        type: 'scroll',
        bottom: 10,
        textStyle: { color: '#ccc', fontSize: 11 }
      },
      series: [{
        type: 'pie', radius: ['40%', '65%'], center: ['50%', '48%'],
        label: { color: '#ddd', fontSize: 11 },
        data: res.data.map((item: any) => ({ value: item.count, name: item.family }))
      }]
    })
  }

  // IUCN 柱状图
  if (iucnChart.value) {
    const chart = echarts.init(iucnChart.value)
    charts.push(chart)
    const res = await getSpeciesByIucn()
    chart.setOption({
      title: { text: 'IUCN 保护等级分布', left: 'center', textStyle: { color: '#fff', fontSize: 16 } },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0,0,0,0.8)',
        borderColor: 'rgba(147,235,248,0.5)',
        textStyle: { color: '#fff' }
      },
      xAxis: {
        type: 'category',
        data: res.data.map((item: any) => item.iucn_status),
        axisLabel: { color: '#ccc' },
        axisLine: { lineStyle: { color: '#555' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#ccc' },
        axisLine: { lineStyle: { color: '#555' } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
      },
      series: [{
        type: 'bar',
        data: res.data.map((item: any) => ({
          value: item.count,
          itemStyle: { color: iucnColors[item.iucn_status] || '#909399' }
        }))
      }]
    })
  }

  // 观测趋势折线图
  if (trendChart.value) {
    const chart = echarts.init(trendChart.value)
    charts.push(chart)
    const res = await getObservationTrend()
    chart.setOption({
      title: { text: '观测记录趋势', left: 'center', textStyle: { color: '#fff', fontSize: 16 } },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0,0,0,0.8)',
        borderColor: 'rgba(147,235,248,0.5)',
        textStyle: { color: '#fff' }
      },
      xAxis: {
        type: 'category',
        data: res.data.map((item: any) => item.period),
        axisLabel: { color: '#ccc' },
        axisLine: { lineStyle: { color: '#555' } }
      },
      yAxis: {
        type: 'value',
        name: '观测次数',
        nameTextStyle: { color: '#ccc' },
        axisLabel: { color: '#ccc' },
        axisLine: { lineStyle: { color: '#555' } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
      },
      series: [{
        type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
        lineStyle: { width: 3, color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.15)' },
        itemStyle: { color: '#409eff' },
        data: res.data.map((item: any) => item.count)
      }]
    })
  }

  // ===== 世界地图可视化 =====
  if (mapChart.value) {
    // 1. 加载 GeoJSON
    const geoJson = await loadWorldGeoJson()
    if (!geoJson) {
      console.error('世界地图 GeoJSON 加载失败，地图组件无法渲染')
      return
    }

    // 2. 获取分布数据
    const distRes = await getSpeciesDistribution()
    const distributions = distRes.data || []

    // 3. 构建 timeline options
    const timelineOptions = buildTimelineOptions(distributions)

    // 4. 初始化地图图表
    const chart = echarts.init(mapChart.value)
    charts.push(chart)

    chart.setOption({
      timeline: {
        data: ['极危 CR', '濒危 EN', '易危 VU', '近危 NT', '无危 LC'],
        axisType: 'category',
        autoPlay: true,
        playInterval: 3000,
        left: '5%',
        right: '25%',
        bottom: '3%',
        label: {
          textStyle: { color: '#ddd' },
          emphasis: { textStyle: { color: '#fff' } }
        },
        symbolSize: 10,
        lineStyle: { color: '#555' },
        checkpointStyle: { borderColor: '#777', borderWidth: 2 },
        controlStyle: {
          showNextBtn: true,
          showPrevBtn: true,
          color: '#666',
          borderColor: '#666'
        }
      },
      baseOption: {
        animation: true,
        animationDuration: 1000,
        animationEasing: 'cubicInOut',
        animationDurationUpdate: 1000,
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(0,0,0,0.8)',
          borderColor: 'rgba(147,235,248,0.5)',
          textStyle: { color: '#fff' }
        },
        geo: {
          show: true,
          map: 'world',
          roam: true,
          zoom: 1.5,
          center: [100, 20],
          label: { emphasis: { show: false } },
          itemStyle: {
            borderColor: 'rgba(147, 235, 248, 1)',
            borderWidth: 1,
            areaColor: {
              type: 'radial',
              x: 0.5, y: 0.5, r: 0.8,
              colorStops: [
                { offset: 0, color: 'rgba(147, 235, 248, 0)' },
                { offset: 1, color: 'rgba(147, 235, 248, .2)' }
              ],
              globalCoord: false
            },
            shadowColor: 'rgba(128, 217, 248, 1)',
            shadowOffsetX: -2,
            shadowOffsetY: 2,
            shadowBlur: 10
          },
          emphasis: {
            areaColor: '#389BB7',
            borderWidth: 0
          }
        },
        grid: {
          right: '1%',
          top: '15%',
          bottom: '12%',
          width: '18%'
        },
        xAxis: {
          type: 'value',
          scale: true,
          position: 'top',
          min: 0,
          splitLine: { show: false },
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { margin: 2, color: '#aaa' }
        },
        yAxis: {
          type: 'category',
          nameGap: 16,
          axisLine: { show: true, lineStyle: { color: '#ddd' } },
          axisTick: { show: false },
          axisLabel: { interval: 0, color: '#ddd', fontSize: 11 }
        }
      },
      options: timelineOptions
    })

    // 鼠标悬停时暂停时间轴，移开后恢复
    chart.on('mouseover', () => {
      chart.dispatchAction({ type: 'timelinePlayChange', playState: false })
    })
    chart.on('mouseout', () => {
      chart.dispatchAction({ type: 'timelinePlayChange', playState: true })
    })
  }

  // 全局 resize 监听
  window.addEventListener('resize', handleResize)
})

function handleResize() {
  charts.forEach(c => c.resize())
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
  charts.length = 0
})
</script>
