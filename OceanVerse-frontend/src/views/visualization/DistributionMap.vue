<template>
  <div>
    <div class="page-header"><h2>物种分布地图</h2><p>全球海洋物种分布可视化</p></div>
    <el-card>
      <div ref="mapContainer" style="height:600px;border-radius:12px;overflow:hidden"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSpeciesDistribution } from '@/api/visual'

const mapContainer = ref<HTMLElement>()

/** 动态加载高德地图 JS API */
function loadAMap(): Promise<void> {
  return new Promise((resolve, reject) => {
    if ((window as any).AMap) { resolve(); return }
    ;(window as any)._AMapSecurityConfig = {
      securityJsCode: '9cbd4e854789581ae43ffea839fa4cc3'
    }
    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=e07413fbaef5c330b8034a514a9b0537'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  if (!mapContainer.value) return

  // 1. 加载高德地图 SDK
  await loadAMap()
  const AMap = (window as any).AMap

  // 2. 初始化地图，默认中心点为中国南海区域
  const map = new AMap.Map(mapContainer.value, {
    zoom: 5,
    center: [112, 25],
    viewMode: '2D'
  })

  // 3. 调用后端获取真实分布数据
  const res = await getSpeciesDistribution()

  // 4. 添加标记点
  const markers: any[] = []
  res.data.forEach((item: any) => {
    if (item.latitude && item.longitude) {
      const marker = new AMap.Marker({
        position: [item.longitude, item.latitude],
        title: item.chinese_name
      })
      const info = new AMap.InfoWindow({
        content:
          `<div style="padding:4px 8px;font-size:13px">` +
          `<b>${item.chinese_name || '未知物种'}</b><br/>` +
          `${item.region_name || ''}<br/>` +
          `类型: ${item.distribution_type || '-'} · 生境: ${item.habitat_type || '-'}` +
          `</div>`,
        offset: new AMap.Pixel(0, -30)
      })
      marker.on('click', () => info.open(map, marker.getPosition()))
      map.add(marker)
      markers.push(marker)
    }
  })

  // 5. 自动调整视野到数据范围
  if (markers.length > 0) {
    map.setFitView(markers, false, [60, 60, 60, 60])
  }
})
</script>
