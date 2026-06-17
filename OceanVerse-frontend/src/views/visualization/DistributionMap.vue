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

  // 4. 添加标记点（悬浮显示图片，点击显示详情）
  const markers: any[] = []
  const iucnColors: Record<string, string> = {
    CR: '#f56c6c', EN: '#e6a23c', VU: '#409eff',
    NT: '#67c23a', LC: '#909399', DD: '#c0c4cc'
  }

  res.data.forEach((item: any) => {
    if (item.latitude && item.longitude) {
      const marker = new AMap.Marker({
        position: [item.longitude, item.latitude],
        title: item.chinese_name
      })

      // 悬浮提示卡片（图片 + 名称）
      const imgUrl = item.image_url || ''
      const hoverContent =
        `<div style="display:flex;align-items:center;gap:10px;padding:6px 10px;font-size:13px;min-width:160px">` +
        (imgUrl ? `<img src="${imgUrl}" style="width:80px;height:56px;object-fit:cover;border-radius:6px"/>` : '') +
        `<div><b>${item.chinese_name || '未知物种'}</b><br/>` +
        `<span style="color:#888;font-size:11px;font-style:italic">${item.scientific_name || ''}</span></div>` +
        `</div>`
      const hoverWin = new AMap.InfoWindow({
        content: hoverContent,
        offset: new AMap.Pixel(0, -30),
        closeWhenClickMap: true,
        isCustom: true
      })

      // 点击详情面板
      const iucnColor = iucnColors[item.iucn_status] || '#909399'
      const detailContent =
        `<div style="width:320px;padding:12px;font-size:13px;line-height:1.6">` +
        (imgUrl ? `<img src="${imgUrl}" style="width:100%;height:180px;object-fit:cover;border-radius:8px;margin-bottom:10px"/>` : '') +
        `<div style="font-size:18px;font-weight:700;margin-bottom:2px">${item.chinese_name || '未知物种'}</div>` +
        `<div style="font-style:italic;color:#888;margin-bottom:8px">${item.scientific_name || ''}</div>` +
        (item.iucn_status ? `<span style="display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;color:#fff;background:${iucnColor};margin-bottom:8px">IUCN ${item.iucn_status}</span>` : '') +
        `<div style="margin-top:6px"><b>分布区域：</b>${[item.country, item.province, item.region_name].filter(Boolean).join(' / ')}</div>` +
        `<div><b>分布类型：</b>${item.distribution_type || '-'}</div>` +
        `<div><b>生境类型：</b>${item.habitat_type || '-'}</div>` +
        (item.description ? `<div style="margin-top:8px;color:#666;font-size:12px;line-height:1.5;max-height:60px;overflow:hidden;text-overflow:ellipsis">${item.description}</div>` : '') +
        `</div>`
      const detailWin = new AMap.InfoWindow({
        content: detailContent,
        offset: new AMap.Pixel(0, -30),
        closeWhenClickMap: true
      })

      marker.on('mouseover', () => { hoverWin.open(map, marker.getPosition()) })
      marker.on('mouseout', () => { hoverWin.close() })
      marker.on('click', () => { hoverWin.close(); detailWin.open(map, marker.getPosition()) })
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
