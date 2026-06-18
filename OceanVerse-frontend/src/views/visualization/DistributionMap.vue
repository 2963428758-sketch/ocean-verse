<template>
  <div class="distribution-page">
    <div class="page-header">
      <h2>物种分布地图</h2>
      <p>全球海洋物种分布可视化 · 共 {{ speciesList.length }} 个物种，{{ filteredDistributions.length }} 个分布点</p>
    </div>
    <el-card class="map-card" :body-style="{ padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }">
      <!-- Tab 切换 -->
      <div class="view-tabs">
        <div
          class="view-tab"
          :class="{ active: activeTab === '2d' }"
          @click="activeTab = '2d'"
        >
          <el-icon style="margin-right:4px"><MapLocation /></el-icon> 2D 地图
        </div>
        <div
          class="view-tab"
          :class="{ active: activeTab === '3d' }"
          @click="activeTab = '3d'"
        >
          <el-icon style="margin-right:4px"><Place /></el-icon> 3D 地球
        </div>
      </div>

      <!-- ===== 2D 地图视图 ===== -->
      <div v-show="activeTab === '2d'" class="map-layout">
        <div class="filter-sidebar">
          <div class="filter-section">
            <el-input v-model="keyword" placeholder="搜索物种名称..." :prefix-icon="Search" clearable size="small" />
          </div>
          <div class="filter-section">
            <div class="filter-label">按科分类</div>
            <el-select v-model="selectedFamily" placeholder="全部" clearable size="small" style="width:100%">
              <el-option v-for="f in familyOptions" :key="f" :label="f" :value="f" />
            </el-select>
          </div>
          <div class="filter-section">
            <div class="filter-label">保护等级</div>
            <div class="iucn-tags">
              <span
                v-for="s in iucnOptions" :key="s" class="iucn-tag"
                :class="{ active: selectedIucn.includes(s) }"
                :style="selectedIucn.includes(s) ? { background: iucnColors[s], color: '#fff', borderColor: iucnColors[s] } : {}"
                @click="toggleIucn(s)"
              >{{ s }}</span>
            </div>
          </div>
          <div class="filter-section species-section">
            <div class="filter-label">
              物种列表
              <el-button text size="small" type="primary" @click="resetFilters" class="reset-btn">重置</el-button>
            </div>
            <div class="species-list">
              <div
                v-for="sp in filteredSpecies" :key="sp.species_id"
                class="species-item" :class="{ active: selectedSpecies === sp.species_id }"
                @click="handleSpeciesClick(sp.species_id)"
              >
                <img v-if="sp.image_url" :src="sp.image_url" class="species-thumb" alt="" />
                <div v-else class="species-thumb no-img">?</div>
                <div class="species-info">
                  <div class="species-name">{{ sp.chinese_name }}</div>
                  <div class="species-sci">{{ sp.scientific_name }}</div>
                </div>
                <span class="species-badge" :style="{ background: iucnColors[sp.iucn_status] || '#909399' }">
                  {{ sp.iucn_status || '—' }}
                </span>
              </div>
              <div v-if="filteredSpecies.length === 0" class="empty-tip">暂无匹配物种</div>
            </div>
          </div>
        </div>
        <div ref="mapContainer" class="map-container"></div>
      </div>

      <!-- ===== 3D 地球视图（用 v-if 避免 WebGL 上下文丢失） ===== -->
      <div v-if="activeTab === '3d'" class="globe-wrapper">
        <div ref="globeContainer" class="globe-chart"></div>
        <!-- IUCN 图例 -->
        <div class="globe-legend">
          <div class="legend-title">IUCN 保护等级</div>
          <div v-for="s in iucnOptions" :key="s" class="legend-item">
            <span class="legend-dot" :style="{ background: iucnColors[s] }"></span>
            <span class="legend-label">{{ iucnLabels[s] }}（{{ s }}）</span>
          </div>
          <div class="legend-count">{{ speciesList.length }} 个物种 · {{ allDistributions.length }} 个分布点</div>
        </div>
        <!-- 操作提示 -->
        <div class="globe-hint">拖拽旋转 · 滚轮缩放 · 自动旋转中</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search, MapLocation, Place } from '@element-plus/icons-vue'
import { getSpeciesDistribution } from '@/api/visual'
import * as echarts from 'echarts'
import 'echarts-gl'
// 显式注册 echarts-gl 组件，确保 globe 坐标系和 scatter3D 正确加载
import 'echarts-gl/lib/component/globe.js'
import 'echarts-gl/lib/chart/scatter3D/Scatter3DSeries.js'
import 'echarts-gl/lib/chart/scatter3D/Scatter3DView.js'

const activeTab = ref<'2d' | '3d'>('2d')

const mapContainer = ref<HTMLElement>()
const globeContainer = ref<HTMLElement>()
let globeChart: echarts.ECharts | null = null
const allDistributions = ref<any[]>([])
const mapInstance = ref<any>(null)
const allMarkers = ref<any[]>([])

/* ---- 筛选状态 ---- */
const keyword = ref('')
const selectedFamily = ref('')
const selectedIucn = ref<string[]>([])
const selectedSpecies = ref<number | null>(null)

const iucnColors: Record<string, string> = {
  CR: '#f56c6c', EN: '#e6a23c', VU: '#409eff',
  NT: '#67c23a', LC: '#909399', DD: '#c0c4cc'
}
const iucnLabels: Record<string, string> = {
  CR: '极危', EN: '濒危', VU: '易危',
  NT: '近危', LC: '无危', DD: '数据缺乏'
}
const iucnOptions = ['CR', 'EN', 'VU', 'NT', 'LC', 'DD']

/* ---- 从数据中提取物种列表（去重） ---- */
const speciesList = computed(() => {
  const map = new Map<number, any>()
  allDistributions.value.forEach(d => {
    if (!map.has(d.species_id)) {
      map.set(d.species_id, {
        species_id: d.species_id,
        chinese_name: d.chinese_name,
        scientific_name: d.scientific_name,
        family: d.family,
        iucn_status: d.iucn_status,
        image_url: d.image_url
      })
    }
  })
  return Array.from(map.values())
})

const familyOptions = computed(() => {
  return [...new Set(speciesList.value.map(s => s.family).filter(Boolean))] as string[]
})

const filteredSpecies = computed(() => {
  return speciesList.value.filter(sp => {
    if (selectedFamily.value && sp.family !== selectedFamily.value) return false
    if (selectedIucn.value.length > 0 && !selectedIucn.value.includes(sp.iucn_status)) return false
    if (keyword.value) {
      const kw = keyword.value.toLowerCase()
      if (
        !(sp.chinese_name || '').toLowerCase().includes(kw) &&
        !(sp.scientific_name || '').toLowerCase().includes(kw)
      ) return false
    }
    return true
  })
})

const filteredDistributions = computed(() => {
  const ids = new Set(filteredSpecies.value.map(s => s.species_id))
  return allDistributions.value.filter(d => ids.has(d.species_id))
})

/* ---- 交互 ---- */
function toggleIucn(status: string) {
  const idx = selectedIucn.value.indexOf(status)
  if (idx >= 0) selectedIucn.value.splice(idx, 1)
  else selectedIucn.value.push(status)
}

function handleSpeciesClick(speciesId: number) {
  if (selectedSpecies.value === speciesId) {
    selectedSpecies.value = null
  } else {
    selectedSpecies.value = speciesId
    const points = allDistributions.value.filter(
      d => d.species_id === speciesId && d.latitude && d.longitude
    )
    if (points.length === 1) {
      mapInstance.value.setZoomAndCenter(8, [points[0].longitude, points[0].latitude], false, 500)
    } else if (points.length > 1) {
      const markers = allMarkers.value
        .filter(m => m.data.species_id === speciesId)
        .map(m => m.marker)
      if (markers.length > 0) {
        mapInstance.value.setFitView(markers, false, [80, 80, 80, 320], 15)
      }
    }
  }
}

function resetFilters() {
  selectedFamily.value = ''
  selectedIucn.value = []
  selectedSpecies.value = null
  keyword.value = ''
}

/* ---- 高德地图 ---- */
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
  await loadAMap()
  const AMap = (window as any).AMap
  mapInstance.value = new AMap.Map(mapContainer.value, {
    zoom: 4, center: [112, 25], viewMode: '2D'
  })
  const res = await getSpeciesDistribution()
  allDistributions.value = res.data || []
  await nextTick()
  createAllMarkers()
  updateMarkerVisibility()
})

/* ---- 2D Marker 管理 ---- */
function clearMarkers() {
  allMarkers.value.forEach(m => m.marker.setMap(null))
  allMarkers.value = []
}

function createAllMarkers() {
  clearMarkers()
  const map = mapInstance.value
  const AMap = (window as any).AMap
  allDistributions.value.forEach((item: any) => {
    if (!item.latitude || !item.longitude) return
    const marker = new AMap.Marker({
      position: [item.longitude, item.latitude],
      title: item.chinese_name
    })
    const imgUrl = item.image_url || ''
    const hoverContent =
      `<div style="display:flex;align-items:center;gap:10px;padding:8px 12px;font-size:13px;min-width:180px;background:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.15)">` +
      (imgUrl ? `<img src="${imgUrl}" style="width:80px;height:56px;object-fit:cover;border-radius:6px;flex-shrink:0"/>` : '') +
      `<div><b style="font-size:14px">${item.chinese_name || '未知物种'}</b><br/>` +
      `<span style="color:#888;font-size:11px;font-style:italic">${item.scientific_name || ''}</span>` +
      (item.family ? `<br/><span style="color:#aaa;font-size:11px">${item.family}</span>` : '') +
      `</div></div>`
    const hoverWin = new AMap.InfoWindow({
      content: hoverContent, offset: new AMap.Pixel(0, -30),
      closeWhenClickMap: true, isCustom: true
    })
    const iucnColor = iucnColors[item.iucn_status] || '#909399'
    const detailContent =
      `<div style="width:320px;padding:14px;font-size:13px;line-height:1.6;background:#fff;border-radius:10px;box-shadow:0 4px 20px rgba(0,0,0,.18)">` +
      (imgUrl ? `<img src="${imgUrl}" style="width:100%;height:180px;object-fit:cover;border-radius:8px;margin-bottom:10px"/>` : '') +
      `<div style="font-size:18px;font-weight:700;margin-bottom:2px">${item.chinese_name || '未知物种'}</div>` +
      `<div style="font-style:italic;color:#888;margin-bottom:4px">${item.scientific_name || ''}</div>` +
      (item.family ? `<div style="color:#aaa;font-size:12px;margin-bottom:8px">${item.family}</div>` : '') +
      (item.iucn_status ? `<span style="display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;color:#fff;background:${iucnColor};margin-bottom:8px">IUCN ${item.iucn_status}</span>` : '') +
      `<div style="margin-top:8px"><b>分布区域：</b>${[item.country, item.province, item.region_name].filter(Boolean).join(' / ')}</div>` +
      `<div><b>分布类型：</b>${item.distribution_type || '—'}</div>` +
      `<div><b>生境类型：</b>${item.habitat_type || '—'}</div>` +
      (item.description ? `<div style="margin-top:8px;color:#666;font-size:12px;line-height:1.5;max-height:60px;overflow:hidden;text-overflow:ellipsis">${item.description}</div>` : '') +
      `</div>`
    const detailWin = new AMap.InfoWindow({
      content: detailContent, offset: new AMap.Pixel(0, -30),
      closeWhenClickMap: true, isCustom: true
    })
    marker.on('mouseover', () => hoverWin.open(map, marker.getPosition()))
    marker.on('mouseout', () => hoverWin.close())
    marker.on('click', () => { hoverWin.close(); detailWin.open(map, marker.getPosition()) })
    map.add(marker)
    allMarkers.value.push({ marker, data: item })
  })
}

function updateMarkerVisibility() {
  const filteredIds = new Set(filteredDistributions.value.map(d => `${d.species_id}_${d.latitude}_${d.longitude}`))
  const visibleMarkers: any[] = []
  allMarkers.value.forEach(m => {
    const key = `${m.data.species_id}_${m.data.latitude}_${m.data.longitude}`
    if (filteredIds.has(key)) { m.marker.show(); visibleMarkers.push(m.marker) }
    else { m.marker.hide() }
  })
  if (visibleMarkers.length > 0 && !selectedSpecies.value) {
    mapInstance.value.setFitView(visibleMarkers, false, [60, 60, 60, 60])
  }
}

watch(filteredDistributions, () => updateMarkerVisibility())

/* ===== 3D 地球 (ECharts GL) ===== */

async function initGlobe() {
  if (!globeContainer.value) return

  destroyGlobe()

  // 确保容器有正确的尺寸
  const rect = globeContainer.value.getBoundingClientRect()
  console.log('3D 地球容器尺寸:', Math.round(rect.width), 'x', Math.round(rect.height))
  if (rect.width === 0 || rect.height === 0) {
    console.warn('容器尺寸为 0，等待布局...')
    await new Promise<void>(resolve => {
      const ro = new ResizeObserver(entries => {
        if (entries[0]?.contentRect.width && entries[0]?.contentRect.height) {
          ro.disconnect()
          resolve()
        }
      })
      ro.observe(globeContainer.value!)
      setTimeout(() => { ro.disconnect(); resolve() }, 2000)
    })
  }

  globeChart = echarts.init(globeContainer.value)

  // 构建标记点数据：[经度, 纬度, 高度(0=贴地表)]
  const markerData = allDistributions.value
    .filter(d => d.latitude != null && d.longitude != null)
    .map(d => ({
      name: d.chinese_name || '未知物种',
      value: [d.longitude, d.latitude, 0],
      iucn: d.iucn_status,
      region: [d.country, d.province, d.region_name].filter(Boolean).join(' / '),
      family: d.family || '',
      itemStyle: {
        color: iucnColors[d.iucn_status] || '#ff6b35'
      }
    }))

  globeChart.setOption({
    globe: {
      environment: 'auto',
      baseTexture: '/earth-texture.jpg',
      shading: 'lambert',
      regionHeight: 1.5,
      atmosphere: {
        show: true,
        color: [0.15, 0.35, 0.65],
        glowPower: 8,
        innerGlowPower: 2
      },
      viewControl: {
        autoRotate: true,
        autoRotateSpeed: 4,
        distance: 200,
        minDistance: 80,
        maxDistance: 400,
        alpha: 20,
        beta: 40,
        panMouseButton: 'right',
        rotateSensitivity: 3,
        zoomSensitivity: 1.2
      },
      light: {
        ambient: { intensity: 0.4 },
        main: { intensity: 1.5, shadow: false }
      }
    },
    series: [{
      type: 'scatter3D',
      coordinateSystem: 'globe',
      data: markerData,
      symbolSize: 18,
      itemStyle: {
        borderWidth: 2,
        borderColor: '#fff'
      },
      encode: {
        tooltip: [0, 1]
      },
      label: {
        show: false
      }
    }],
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const d = params.data as any
        const color = iucnColors[d.iucn] || '#909399'
        return `<div style="padding:4px 8px;min-width:140px">` +
          `<b style="font-size:14px">${d.name}</b><br/>` +
          `<span style="color:#999;font-size:11px;font-style:italic">${d.family || ''}</span><br/>` +
          `<span style="display:inline-block;padding:1px 8px;border-radius:8px;font-size:11px;font-weight:600;color:#fff;background:${color};margin:4px 0">IUCN ${d.iucn || '—'}</span><br/>` +
          `<span style="font-size:12px;color:#666">📍 ${d.region || '未知区域'}</span>` +
          `</div>`
      }
    }
  })

  // 监听窗口 resize 保持地球尺寸正确
  window.addEventListener('resize', handleGlobeResize)
}

function handleGlobeResize() {
  globeChart?.resize()
}

function destroyGlobe() {
  window.removeEventListener('resize', handleGlobeResize)
  if (globeChart) {
    globeChart.dispose()
    globeChart = null
  }
}

/* ---- Tab 切换 ---- */
watch(activeTab, async (tab) => {
  if (tab === '3d') {
    await nextTick()
    // 等浏览器完成布局再初始化地球
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        initGlobe()
      })
    })
  } else {
    destroyGlobe()
    if (mapInstance.value) {
      await nextTick()
      mapInstance.value.resize()
    }
  }
})

onBeforeUnmount(() => {
  destroyGlobe()
})
</script>

<style scoped>
.distribution-page { padding: 0; }

.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; }
.page-header p { margin: 0; color: #909399; font-size: 13px; }

.map-card {
  height: calc(100vh - 160px);
  min-height: 600px;
  border-radius: 12px;
  overflow: hidden;
}

/* ---- Tab 切换 ---- */
.view-tabs {
  display: flex;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  flex-shrink: 0;
}
.view-tab {
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  color: #909399;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  user-select: none;
}
.view-tab:hover { color: #409eff; }
.view-tab.active {
  color: #409eff;
  border-bottom-color: #409eff;
}

/* ---- 2D 地图布局 ---- */
.map-layout { display: flex; flex: 1; min-height: 0; }
.map-container { flex: 1; min-width: 0; }

/* ---- 3D 地球布局 ---- */
.globe-wrapper {
  flex: 1;
  min-height: 0;
  position: relative;
  background: linear-gradient(135deg, #0a0e1a 0%, #0f1629 50%, #0a1020 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.globe-chart {
  width: 100%;
  height: 100%;
}

/* ---- 图例 ---- */
.globe-legend {
  position: absolute;
  bottom: 20px;
  left: 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 10px;
  padding: 14px 18px;
  font-size: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 10;
}
.legend-title {
  font-weight: 700;
  font-size: 13px;
  color: #303133;
  margin-bottom: 8px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.15);
}
.legend-label { color: #606266; }
.legend-count {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
  color: #909399;
  font-size: 11px;
}

/* ---- 操作提示 ---- */
.globe-hint {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  padding: 6px 14px;
  border-radius: 20px;
  backdrop-filter: blur(8px);
  z-index: 10;
}

/* ---- 筛选栏（复用之前样式） ---- */
.filter-sidebar {
  width: 280px; flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  display: flex; flex-direction: column;
  background: #fafbfc; overflow: hidden;
}
.filter-section { padding: 12px 16px 0; }
.species-section {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; padding-bottom: 12px;
}
.filter-label {
  font-size: 12px; color: #909399; margin-bottom: 8px;
  font-weight: 500; display: flex; align-items: center;
  justify-content: space-between;
}
.reset-btn { padding: 0; font-size: 12px; }

.iucn-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.iucn-tag {
  display: inline-block; padding: 2px 12px; border-radius: 12px;
  font-size: 11px; font-weight: 600; cursor: pointer;
  border: 1px solid #dcdfe6; background: #fff; color: #606266;
  transition: all 0.2s; user-select: none;
}
.iucn-tag:hover { opacity: 0.85; }
.iucn-tag.active { color: #fff; }

.species-list { flex: 1; overflow-y: auto; margin-top: 8px; }
.species-item {
  display: flex; align-items: center; padding: 8px 10px;
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
  gap: 10px; margin-bottom: 2px;
}
.species-item:hover { background: #f0f2f5; }
.species-item.active { background: #ecf5ff; box-shadow: inset 3px 0 0 #409eff; }
.species-thumb { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.no-img {
  display: flex; align-items: center; justify-content: center;
  background: #f0f2f5; color: #c0c4cc; font-size: 18px; font-weight: 700;
}
.species-info { flex: 1; min-width: 0; }
.species-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.species-sci { font-size: 11px; color: #c0c4cc; font-style: italic; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.species-badge { flex-shrink: 0; padding: 1px 7px; border-radius: 8px; font-size: 10px; font-weight: 700; color: #fff; letter-spacing: 0.5px; }
.empty-tip { text-align: center; color: #c0c4cc; padding: 32px 0; font-size: 13px; }
</style>
