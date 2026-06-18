<template>
  <div class="distribution-page">
    <div class="page-header">
      <h2>物种分布地图</h2>
      <p>全球海洋物种分布可视化 · 共 {{ speciesList.length }} 个物种，{{ filteredDistributions.length }} 个分布点</p>
    </div>
    <el-card class="map-card" :body-style="{ padding: 0, height: '100%' }">
      <div class="map-layout">
        <!-- 左侧筛选栏 -->
        <div class="filter-sidebar">
          <!-- 搜索 -->
          <div class="filter-section">
            <el-input
              v-model="keyword"
              placeholder="搜索物种名称..."
              :prefix-icon="Search"
              clearable
              size="small"
            />
          </div>

          <!-- 按科筛选 -->
          <div class="filter-section">
            <div class="filter-label">按科分类</div>
            <el-select v-model="selectedFamily" placeholder="全部" clearable size="small" style="width: 100%">
              <el-option v-for="f in familyOptions" :key="f" :label="f" :value="f" />
            </el-select>
          </div>

          <!-- 按保护等级 -->
          <div class="filter-section">
            <div class="filter-label">保护等级</div>
            <div class="iucn-tags">
              <span
                v-for="s in iucnOptions"
                :key="s"
                class="iucn-tag"
                :class="{ active: selectedIucn.includes(s) }"
                :style="selectedIucn.includes(s) ? { background: iucnColors[s] || '#909399', color: '#fff', borderColor: iucnColors[s] || '#909399' } : {}"
                @click="toggleIucn(s)"
              >{{ s }}</span>
            </div>
          </div>

          <!-- 物种列表 -->
          <div class="filter-section species-section">
            <div class="filter-label">
              物种列表
              <el-button text size="small" type="primary" @click="resetFilters" class="reset-btn">重置</el-button>
            </div>
            <div class="species-list">
              <div
                v-for="sp in filteredSpecies"
                :key="sp.species_id"
                class="species-item"
                :class="{ active: selectedSpecies === sp.species_id }"
                @click="handleSpeciesClick(sp.species_id)"
              >
                <img
                  v-if="sp.image_url"
                  :src="sp.image_url"
                  class="species-thumb"
                  alt=""
                />
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

        <!-- 右侧地图 -->
        <div ref="mapContainer" class="map-container"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getSpeciesDistribution } from '@/api/visual'

const mapContainer = ref<HTMLElement>()
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

/* ---- 筛选后的物种列表 ---- */
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

/* ---- 筛选后的分布点 ---- */
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

    // 找到该物种所有分布点
    const points = allDistributions.value.filter(
      d => d.species_id === speciesId && d.latitude && d.longitude
    )

    if (points.length === 1) {
      // 只有一个点，直接飞过去
      mapInstance.value.setZoomAndCenter(8, [points[0].longitude, points[0].latitude], false, 500)
    } else if (points.length > 1) {
      // 多个点，用 setFitView 全部收入视野
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

/* ---- 高德地图加载 ---- */
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

/* ---- 地图初始化与数据加载 ---- */
onMounted(async () => {
  if (!mapContainer.value) return

  await loadAMap()
  const AMap = (window as any).AMap

  mapInstance.value = new AMap.Map(mapContainer.value, {
    zoom: 4,
    center: [112, 25],
    viewMode: '2D'
  })

  const res = await getSpeciesDistribution()
  allDistributions.value = res.data || []

  await nextTick()
  createAllMarkers()
  updateMarkerVisibility()
})

/* ---- Marker 管理 ---- */
function clearMarkers() {
  allMarkers.value.forEach(m => {
    m.marker.setMap(null)
  })
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

    // 悬浮提示卡片（图片 + 名称）
    const imgUrl = item.image_url || ''
    const hoverContent =
      `<div style="display:flex;align-items:center;gap:10px;padding:8px 12px;font-size:13px;min-width:180px;background:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.15)">` +
      (imgUrl ? `<img src="${imgUrl}" style="width:80px;height:56px;object-fit:cover;border-radius:6px;flex-shrink:0"/>` : '') +
      `<div><b style="font-size:14px">${item.chinese_name || '未知物种'}</b><br/>` +
      `<span style="color:#888;font-size:11px;font-style:italic">${item.scientific_name || ''}</span>` +
      (item.family ? `<br/><span style="color:#aaa;font-size:11px">${item.family}</span>` : '') +
      `</div></div>`
    const hoverWin = new AMap.InfoWindow({
      content: hoverContent,
      offset: new AMap.Pixel(0, -30),
      closeWhenClickMap: true,
      isCustom: true
    })

    // 点击详情面板
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
      content: detailContent,
      offset: new AMap.Pixel(0, -30),
      closeWhenClickMap: true,
      isCustom: true
    })

    marker.on('mouseover', () => { hoverWin.open(map, marker.getPosition()) })
    marker.on('mouseout', () => { hoverWin.close() })
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
    if (filteredIds.has(key)) {
      m.marker.show()
      visibleMarkers.push(m.marker)
    } else {
      m.marker.hide()
    }
  })

  // 选中具体物种时不调 setFitView，由 handleSpeciesClick 的定位生效
  if (visibleMarkers.length > 0 && !selectedSpecies.value) {
    mapInstance.value.setFitView(visibleMarkers, false, [60, 60, 60, 60])
  }
}

/* ---- 筛选变化时自动更新地图 ---- */
watch(filteredDistributions, () => {
  updateMarkerVisibility()
})
</script>

<style scoped>
.distribution-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.map-card {
  height: calc(100vh - 160px);
  min-height: 600px;
  border-radius: 12px;
  overflow: hidden;
}

.map-layout {
  display: flex;
  height: 100%;
}

/* ---- 筛选栏 ---- */
.filter-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  overflow: hidden;
}

.filter-section {
  padding: 12px 16px 0;
}

.species-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 12px;
}

.filter-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reset-btn {
  padding: 0;
  font-size: 12px;
}

/* ---- IUCN 标签 ---- */
.iucn-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.iucn-tag {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  transition: all 0.2s;
  user-select: none;
}

.iucn-tag:hover {
  opacity: 0.85;
}

.iucn-tag.active {
  color: #fff;
}

/* ---- 物种列表 ---- */
.species-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 8px;
}

.species-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 10px;
  margin-bottom: 2px;
}

.species-item:hover {
  background: #f0f2f5;
}

.species-item.active {
  background: #ecf5ff;
  box-shadow: inset 3px 0 0 #409eff;
}

.species-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.no-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 18px;
  font-weight: 700;
}

.species-info {
  flex: 1;
  min-width: 0;
}

.species-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.species-sci {
  font-size: 11px;
  color: #c0c4cc;
  font-style: italic;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.species-badge {
  flex-shrink: 0;
  padding: 1px 7px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 32px 0;
  font-size: 13px;
}

/* ---- 地图 ---- */
.map-container {
  flex: 1;
  min-width: 0;
}
</style>
