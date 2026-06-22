<template>
  <div class="observation-map-page">
    <div class="page-header">
      <h2>观测地图</h2>
      <p>海洋观测记录空间分布 · 共 {{ observations.length }} 条记录，{{ filteredObservations.length }} 个有效坐标点</p>
    </div>
    <el-card class="map-card" :body-style="{ padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }">
      <div class="map-layout">
        <!-- 左侧筛选栏 -->
        <div class="filter-sidebar">
          <div class="filter-section">
            <el-input v-model="keyword" placeholder="搜索编号/观测员..." :prefix-icon="Search" clearable size="small" />
          </div>
          <div class="filter-section">
            <div class="filter-label">观测类型</div>
            <div class="type-tags">
              <span
                v-for="t in typeOptions" :key="t.value" class="type-tag"
                :class="{ active: selectedTypes.includes(t.value) }"
                :style="selectedTypes.includes(t.value) ? { background: typeColors[t.value], color: '#fff', borderColor: typeColors[t.value] } : {}"
                @click="toggleType(t.value)"
              >{{ t.label }}</span>
            </div>
          </div>
          <div class="filter-section">
            <div class="filter-label">时间范围</div>
            <el-date-picker
              v-model="dateRange" type="daterange" size="small"
              range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
              value-format="YYYY-MM-DD" style="width:100%"
            />
          </div>
          <div class="filter-section obs-section">
            <div class="filter-label">
              观测记录
              <el-button text size="small" type="primary" @click="resetFilters" class="reset-btn">重置</el-button>
            </div>
            <div class="obs-list">
              <div
                v-for="obs in filteredObservations" :key="obs.id"
                class="obs-item" :class="{ active: selectedObs === obs.id }"
                @click="handleObsClick(obs)"
              >
                <div class="obs-info">
                  <div class="obs-code">{{ obs.observationCode }}</div>
                  <div class="obs-meta">
                    <span class="obs-type-badge" :style="{ background: typeColors[obs.observationType] || '#909399' }">
                      {{ typeLabelMap[obs.observationType] || obs.observationType || '—' }}
                    </span>
                    <span class="obs-date">{{ obs.observationDate || '' }}</span>
                  </div>
                </div>
              </div>
              <div v-if="filteredObservations.length === 0" class="empty-tip">暂无匹配记录</div>
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
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getObservationMapData } from '@/api/observation'

const mapContainer = ref<HTMLElement>()
const mapInstance = ref<any>(null)
const allMarkers = ref<any[]>([])
const observations = ref<any[]>([])

/* ---- 筛选状态 ---- */
const keyword = ref('')
const selectedTypes = ref<string[]>([])
const selectedObs = ref<number | null>(null)
const dateRange = ref<[string, string] | null>(null)

const typeOptions = [
  { value: 'DIVE', label: '潜水观测' },
  { value: 'SURVEY', label: '调查观测' },
  { value: 'SIGHTING', label: '目击观测' },
  { value: 'TRACKING', label: '追踪观测' }
]
const typeLabelMap: Record<string, string> = {
  DIVE: '潜水', SURVEY: '调查', SIGHTING: '目击', TRACKING: '追踪'
}
const typeColors: Record<string, string> = {
  DIVE: '#409eff', SURVEY: '#67c23a', SIGHTING: '#e6a23c', TRACKING: '#f56c6c'
}

/* ---- 筛选逻辑 ---- */
const filteredObservations = computed(() => {
  return observations.value.filter(obs => {
    if (!obs.latitude || !obs.longitude) return false
    if (selectedTypes.value.length > 0 && !selectedTypes.value.includes(obs.observationType)) return false
    if (keyword.value) {
      const kw = keyword.value.toLowerCase()
      if (
        !(obs.observationCode || '').toLowerCase().includes(kw) &&
        !(obs.observerName || '').toLowerCase().includes(kw)
      ) return false
    }
    if (dateRange.value && dateRange.value[0] && dateRange.value[1]) {
      if (obs.observationDate) {
        if (obs.observationDate < dateRange.value[0] || obs.observationDate > dateRange.value[1]) return false
      }
    }
    return true
  })
})

function toggleType(type: string) {
  const idx = selectedTypes.value.indexOf(type)
  if (idx >= 0) selectedTypes.value.splice(idx, 1)
  else selectedTypes.value.push(type)
}

function resetFilters() {
  keyword.value = ''
  selectedTypes.value = []
  dateRange.value = null
  selectedObs.value = null
}

function handleObsClick(obs: any) {
  if (selectedObs.value === obs.id) {
    selectedObs.value = null
    return
  }
  selectedObs.value = obs.id
  if (obs.latitude && obs.longitude && mapInstance.value) {
    const map = mapInstance.value
    // 找到该观测对应的实际标记点位置（含 jitter 偏移）
    const entry = allMarkers.value.find(m => m.data.id === obs.id)
    const pos = entry ? entry.marker.getPosition() : [obs.longitude, obs.latitude]
    map.setZoom(10, false, 500)
    map.panTo(pos)
  }
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
  const res = await getObservationMapData()
  observations.value = res.data || []
  await nextTick()
  createAllMarkers()
  updateMarkerVisibility()
})

/* ---- Marker 管理 ---- */
function clearMarkers() {
  allMarkers.value.forEach(m => m.marker.setMap(null))
  allMarkers.value = []
}

/** 对同坐标的多条记录做圆形散开 */
function jitterPositions(items: any[]): { item: any; lng: number; lat: number }[] {
  // 按坐标分组
  const groups = new Map<string, any[]>()
  items.forEach((item: any) => {
    const key = `${item.latitude}_${item.longitude}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(item)
  })

  const result: { item: any; lng: number; lat: number }[] = []
  const SPREAD = 0.06 // 散开半径（约 6km，视觉清晰）

  groups.forEach((group) => {
    const baseLng = Number(group[0].longitude)
    const baseLat = Number(group[0].latitude)
    if (group.length === 1) {
      result.push({ item: group[0], lng: baseLng, lat: baseLat })
    } else {
      // 多条记录：沿圆周均匀分布
      group.forEach((item, i) => {
        const angle = (2 * Math.PI * i) / group.length
        const lng = baseLng + SPREAD * Math.cos(angle)
        const lat = baseLat + SPREAD * Math.sin(angle)
        result.push({ item, lng, lat })
      })
    }
  })
  return result
}

function createAllMarkers() {
  clearMarkers()
  const map = mapInstance.value
  const AMap = (window as any).AMap

  const validObs = observations.value.filter((o: any) => o.latitude && o.longitude)
  const positioned = jitterPositions(validObs)

  positioned.forEach(({ item, lng, lat }) => {
    const color = typeColors[item.observationType] || '#909399'
    const marker = new AMap.Marker({
      position: [lng, lat],
      title: item.observationCode,
      content: `<div style="width:16px;height:16px;border-radius:50%;background:${color};border:2.5px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.35)"></div>`,
      offset: new AMap.Pixel(-8, -8)
    })

    const typeLabel = typeLabelMap[item.observationType] || item.observationType || '—'
    const envParts: string[] = []
    if (item.waterTemperature != null) envParts.push(`水温 ${item.waterTemperature}°C`)
    if (item.salinity != null) envParts.push(`盐度 ${item.salinity}‰`)
    if (item.depth != null) envParts.push(`深度 ${item.depth}m`)
    if (item.ph != null) envParts.push(`pH ${item.ph}`)

    const detailContent =
      `<div style="width:300px;padding:14px;font-size:13px;line-height:1.7;background:#fff;border-radius:10px;box-shadow:0 4px 20px rgba(0,0,0,.18)">` +
      `<div style="font-size:18px;font-weight:700;margin-bottom:4px">${item.observationCode}</div>` +
      `<span style="display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:600;color:#fff;background:${color};margin-bottom:10px">${typeLabel}观测</span>` +
      (item.observationDate ? `<div><b>日期：</b>${item.observationDate}${item.observationTime ? ' ' + item.observationTime : ''}</div>` : '') +
      (item.durationMinutes ? `<div><b>时长：</b>${item.durationMinutes} 分钟</div>` : '') +
      (item.observerName ? `<div><b>观测员：</b>${item.observerName}${item.organization ? ' (' + item.organization + ')' : ''}</div>` : '') +
      (envParts.length ? `<div style="margin-top:6px"><b>环境数据：</b>${envParts.join(' · ')}</div>` : '') +
      (item.weatherCondition ? `<div><b>天气：</b>${item.weatherCondition}</div>` : '') +
      (item.seaCondition ? `<div><b>海况：</b>${item.seaCondition}</div>` : '') +
      (item.notes ? `<div style="margin-top:8px;color:#666;font-size:12px;line-height:1.5;max-height:60px;overflow:hidden;text-overflow:ellipsis"><b>备注：</b>${item.notes}</div>` : '') +
      `</div>`
    const detailWin = new AMap.InfoWindow({
      content: detailContent, offset: new AMap.Pixel(0, -10),
      closeWhenClickMap: true, isCustom: true
    })

    // hover 提示
    const hoverContent =
      `<div style="padding:8px 12px;font-size:13px;min-width:180px;background:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.15)">` +
      `<b style="font-size:14px">${item.observationCode}</b>` +
      `<span style="display:inline-block;padding:1px 8px;border-radius:8px;font-size:11px;font-weight:600;color:#fff;background:${color};margin-left:8px">${typeLabel}</span>` +
      (item.observationDate ? `<br/><span style="color:#888;font-size:12px">📅 ${item.observationDate}</span>` : '') +
      (item.observerName ? `<br/><span style="color:#888;font-size:12px">👤 ${item.observerName}</span>` : '') +
      `</div>`
    const hoverWin = new AMap.InfoWindow({
      content: hoverContent, offset: new AMap.Pixel(0, -10),
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
  const filteredIds = new Set(filteredObservations.value.map(d => d.id))
  const visibleMarkers: any[] = []
  allMarkers.value.forEach(m => {
    if (filteredIds.has(m.data.id)) { m.marker.show(); visibleMarkers.push(m.marker) }
    else { m.marker.hide() }
  })
  if (visibleMarkers.length > 0 && !selectedObs.value) {
    mapInstance.value.setFitView(visibleMarkers, false, [60, 60, 60, 320])
  }
}

watch(filteredObservations, () => updateMarkerVisibility())

onBeforeUnmount(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy()
    mapInstance.value = null
  }
})
</script>

<style scoped>
.observation-map-page { padding: 0; }

.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; }
.page-header p { margin: 0; color: #909399; font-size: 13px; }

.map-card {
  height: calc(100vh - 160px);
  min-height: 600px;
  border-radius: 12px;
  overflow: hidden;
}

.map-layout { display: flex; flex: 1; min-height: 0; }
.map-container { flex: 1; min-width: 0; }

.filter-sidebar {
  width: 280px; flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  display: flex; flex-direction: column;
  background: #fafbfc; overflow: hidden;
}
.filter-section { padding: 12px 16px 0; }
.obs-section {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; padding-bottom: 12px;
}
.filter-label {
  font-size: 12px; color: #909399; margin-bottom: 8px;
  font-weight: 500; display: flex; align-items: center;
  justify-content: space-between;
}
.reset-btn { padding: 0; font-size: 12px; }

.type-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.type-tag {
  display: inline-block; padding: 2px 12px; border-radius: 12px;
  font-size: 11px; font-weight: 600; cursor: pointer;
  border: 1px solid #dcdfe6; background: #fff; color: #606266;
  transition: all 0.2s; user-select: none;
}
.type-tag:hover { opacity: 0.85; }
.type-tag.active { color: #fff; }

.obs-list { flex: 1; overflow-y: auto; margin-top: 8px; }
.obs-item {
  display: flex; align-items: center; padding: 10px 12px;
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
  margin-bottom: 2px;
}
.obs-item:hover { background: #f0f2f5; }
.obs-item.active { background: #ecf5ff; box-shadow: inset 3px 0 0 #409eff; }
.obs-info { flex: 1; min-width: 0; }
.obs-code { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.obs-meta { display: flex; align-items: center; gap: 8px; margin-top: 4px; }
.obs-type-badge {
  display: inline-block; padding: 1px 7px; border-radius: 8px;
  font-size: 10px; font-weight: 700; color: #fff; letter-spacing: 0.5px;
}
.obs-date { font-size: 11px; color: #c0c4cc; }
.empty-tip { text-align: center; color: #c0c4cc; padding: 32px 0; font-size: 13px; }
</style>
