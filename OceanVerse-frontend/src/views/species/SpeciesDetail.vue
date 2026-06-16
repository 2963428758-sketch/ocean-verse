<template>
  <div class="species-detail-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="$router.push('/species/list')" text>
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
      </div>
      <div class="header-right">
        <el-button type="warning" @click="openEditDialog">编辑</el-button>
        <el-popconfirm title="确定删除此物种？" @confirm="handleDelete">
          <template #reference>
            <el-button type="danger">删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <div v-if="species" class="detail-body">
      <!-- Hero 卡片：物种名称 + IUCN 色带 -->
      <div class="hero-card">
        <div class="hero-bg-accent"></div>
        <div class="hero-content">
          <div class="hero-main">
            <h2 class="hero-name">{{ species.chineseName || species.commonName || '未知物种' }}</h2>
            <p class="hero-sci"><em>{{ species.scientificName }}</em></p>
            <div class="hero-badges">
              <span v-if="species.speciesCode" class="hero-code">{{ species.speciesCode }}</span>
              <span v-if="species.protectionLevel" class="hero-prot">
                {{ protectionLabel(species.protectionLevel) }}保护
              </span>
              <span v-if="species.isEndemic === 1" class="hero-endemic">特有种</span>
              <span v-if="species.isInvasive === 1" class="hero-invasive">入侵物种</span>
            </div>
          </div>
          <div class="hero-iucn">
            <div class="iucn-current" :style="{ background: iucnColor(species.iucnStatus) }">
              <span class="iucn-code">{{ species.iucnStatus || '—' }}</span>
              <span class="iucn-text">{{ iucnLabel(species.iucnStatus) }}</span>
            </div>
          </div>
        </div>

        <!-- IUCN 色带谱 -->
        <div class="iucn-spectrum">
          <div class="spectrum-bar">
            <div class="spectrum-gradient"></div>
            <div
              v-if="species.iucnStatus && iucnPosition(species.iucnStatus) >= 0"
              class="spectrum-marker"
              :style="{ left: iucnPosition(species.iucnStatus) + '%' }"
            >
              <div class="marker-dot"></div>
              <div class="marker-line"></div>
            </div>
          </div>
          <div class="spectrum-labels">
            <span v-for="item in iucnLevels" :key="item.code" class="spectrum-label"
              :class="{ active: item.code === species.iucnStatus }"
            >
              {{ item.code }}
            </span>
          </div>
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="info-card">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="中文名">{{ species.chineseName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通用名">{{ species.commonName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数据质量">{{ species.dataQuality || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">{{ species.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ species.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 分类学进化链 -->
      <div class="taxonomy-card">
        <h3 class="section-title">分类学信息</h3>
        <div class="taxonomy-chain">
          <div
            v-for="(item, idx) in taxonomyItems"
            :key="item.rank"
            class="chain-node"
            :style="{
              '--node-color': rankColors[idx],
              '--node-delay': `${idx * 0.08}s`
            }"
          >
            <div class="node-connector" v-if="idx > 0">
              <svg width="28" height="2" viewBox="0 0 28 2">
                <line x1="0" y1="1" x2="28" y2="1"
                  stroke="var(--node-color)" stroke-width="2" stroke-dasharray="4 3" />
              </svg>
            </div>
            <div class="node-pill">
              <span class="node-rank">{{ item.rank }}</span>
              <span class="node-value">{{ item.value || '—' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 形态特征 & 生态习性 -->
      <div class="two-col" v-if="species.morphology || species.ecology">
        <div class="text-card" v-if="species.morphology">
          <h3 class="section-title">形态特征</h3>
          <p class="detail-text">{{ species.morphology }}</p>
        </div>
        <div class="text-card" v-if="species.ecology">
          <h3 class="section-title">生态习性</h3>
          <p class="detail-text">{{ species.ecology }}</p>
        </div>
      </div>

      <!-- 分布地图 -->
      <div class="distribution-card">
        <h3 class="section-title">分布信息</h3>

        <div v-if="distributions.length > 0" class="map-wrapper">
          <div ref="mapContainer" class="leaf-map"></div>
          <div class="map-legend">
            <div v-for="item in distributionSummary" :key="item.type" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              {{ item.label }} ({{ item.count }})
            </div>
          </div>
        </div>

        <div v-else class="map-empty">
          <el-empty description="暂无分布数据" :image-size="80" />
        </div>

        <!-- 分布数据表格 (折叠) -->
        <el-collapse class="dist-table-collapse">
          <el-collapse-item title="查看详细分布数据">
            <el-table :data="distributions" stripe size="small">
              <el-table-column prop="distributionType" label="类型" width="90">
                <template #default="{ row }">
                  <el-tag size="small">{{ distTypeLabel(row.distributionType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="regionName" label="区域" width="140" />
              <el-table-column prop="country" label="国家" width="100" />
              <el-table-column prop="province" label="省份" width="100" />
              <el-table-column label="经纬度" width="160">
                <template #default="{ row }">
                  {{ row.latitude && row.longitude ? `${row.latitude}, ${row.longitude}` : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="深度(m)" width="110">
                <template #default="{ row }">
                  {{ row.depthMin != null && row.depthMax != null ? `${row.depthMin}~${row.depthMax}` : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="habitatType" label="栖息地" />
              <el-table-column prop="populationEstimate" label="种群估计" width="100" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="物种不存在" />

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑物种" width="700px" destroy-on-close>
      <el-form :model="editForm" label-width="100px" :rules="editRules" ref="editFormRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="物种编码" prop="speciesCode">
              <el-input v-model="editForm.speciesCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学名" prop="scientificName">
              <el-input v-model="editForm.scientificName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="中文名">
              <el-input v-model="editForm.chineseName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通用名">
              <el-input v-model="editForm.commonName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="保护等级">
              <el-select v-model="editForm.protectionLevel" clearable>
                <el-option label="一级" value="1" />
                <el-option label="二级" value="2" />
                <el-option label="三级" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="IUCN等级">
              <el-select v-model="editForm.iucnStatus" clearable>
                <el-option label="EX" value="EX" />
                <el-option label="EW" value="EW" />
                <el-option label="CR" value="CR" />
                <el-option label="EN" value="EN" />
                <el-option label="VU" value="VU" />
                <el-option label="NT" value="NT" />
                <el-option label="LC" value="LC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="科">
              <el-input v-model="editForm.family" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">分类学信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="界"><el-input v-model="editForm.kingdom" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="门"><el-input v-model="editForm.phylum" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="纲"><el-input v-model="editForm.className" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="目"><el-input v-model="editForm.orderName" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="属"><el-input v-model="editForm.genus" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="种"><el-input v-model="editForm.species" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="特有种">
              <el-switch v-model="editForm.isEndemic" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="入侵物种">
              <el-switch v-model="editForm.isInvasive" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">描述信息</el-divider>
        <el-form-item label="简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="形态特征">
          <el-input v-model="editForm.morphology" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="生态习性">
          <el-input v-model="editForm.ecology" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-input v-model="editForm.source" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editSubmitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {
  getSpeciesDetail,
  getSpeciesDistributions,
  updateSpecies,
  deleteSpecies
} from '@/api/species'
import type { Species, SpeciesDistribution } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const species = ref<Species | null>(null)
const distributions = ref<SpeciesDistribution[]>([])
const mapContainer = ref<HTMLElement>()

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<Species>({
  speciesCode: '',
  scientificName: '',
  chineseName: '',
  commonName: '',
  protectionLevel: '',
  iucnStatus: '',
  family: '',
  isEndemic: 0,
  isInvasive: 0
})
const editRules: FormRules = {
  speciesCode: [{ required: true, message: '请输入物种编码', trigger: 'blur' }],
  scientificName: [{ required: true, message: '请输入学名', trigger: 'blur' }]
}

// ===== IUCN 相关 =====
const iucnColorMap: Record<string, string> = {
  EX: '#1a1a2e', EW: '#6b21a8', CR: '#dc2626', EN: '#ea580c',
  VU: '#d97706', NT: '#65a30d', LC: '#059669', DD: '#9ca3af'
}

const iucnLevels = [
  { code: 'EX', label: '灭绝' },
  { code: 'EW', label: '野外灭绝' },
  { code: 'CR', label: '极危' },
  { code: 'EN', label: '濒危' },
  { code: 'VU', label: '易危' },
  { code: 'NT', label: '近危' },
  { code: 'LC', label: '无危' }
]

// 在渐变条上的百分比位置 (EX 最左 = 最危险, LC 最右 = 最安全)
function iucnPosition(status?: string): number {
  const positions: Record<string, number> = {
    EX: 3, EW: 14, CR: 28, EN: 43, VU: 57, NT: 72, LC: 90
  }
  return positions[status || ''] ?? -1
}

function iucnColor(status?: string): string {
  return iucnColorMap[status || ''] || '#9ca3af'
}

function iucnLabel(status?: string): string {
  if (!status) return '未评估'
  const found = iucnLevels.find(l => l.code === status)
  return found ? `${status} (${found.label})` : status
}

// ===== 分类学链 =====
const rankColors = [
  '#0f4c75', '#156a9e', '#1b88b8', '#22a0c4',
  '#2bb5c4', '#3cc4a8', '#4ecdc4'
]

const taxonomyItems = computed(() => {
  if (!species.value) return []
  const s = species.value
  return [
    { rank: '界', value: s.kingdom },
    { rank: '门', value: s.phylum },
    { rank: '纲', value: s.className },
    { rank: '目', value: s.orderName },
    { rank: '科', value: s.family },
    { rank: '属', value: s.genus },
    { rank: '种', value: s.species }
  ]
})

// ===== 分布相关 =====
function distTypeLabel(type: string) {
  const map: Record<string, string> = { NATIVE: '原生', INTRODUCED: '引入', MIGRATORY: '迁徙' }
  return map[type] || type
}

const distTypeColors: Record<string, string> = {
  NATIVE: '#059669', INTRODUCED: '#d97706', MIGRATORY: '#2563eb'
}

const distributionSummary = computed(() => {
  const counts: Record<string, number> = {}
  distributions.value.forEach(d => {
    const t = d.distributionType || 'OTHER'
    counts[t] = (counts[t] || 0) + 1
  })
  const labelMap: Record<string, string> = { NATIVE: '原生', INTRODUCED: '引入', MIGRATORY: '迁徙' }
  return Object.entries(counts).map(([type, count]) => ({
    type,
    label: labelMap[type] || type,
    count,
    color: distTypeColors[type] || '#6b7280'
  }))
})

// ===== 辅助函数 =====
function protectionLabel(level: string) {
  const map: Record<string, string> = { '1': '一级', '2': '二级', '3': '三级' }
  return map[level] || level
}

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const speciesId = Number(route.params.id)
    const res: any = await getSpeciesDetail(speciesId)
    species.value = res.data

    const distRes: any = await getSpeciesDistributions(speciesId)
    distributions.value = distRes.data || []

    await nextTick()
    initMap()
  } finally {
    loading.value = false
  }
}

function initMap() {
  if (!mapContainer.value || distributions.value.length === 0) return

  const points = distributions.value
    .filter(d => d.latitude != null && d.longitude != null)

  if (points.length === 0) return

  const map = L.map(mapContainer.value, {
    scrollWheelZoom: false,
    zoomControl: true
  })

  L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap &copy; CARTO',
    maxZoom: 18
  }).addTo(map)

  const bounds = L.latLngBounds([])

  points.forEach(d => {
    const color = distTypeColors[d.distributionType] || '#6b7280'
    const marker = L.circleMarker([d.latitude!, d.longitude!], {
      radius: 8,
      fillColor: color,
      color: '#fff',
      weight: 2,
      fillOpacity: 0.85
    }).addTo(map)

    const typeLabel = distTypeLabel(d.distributionType)
    marker.bindPopup(`
      <div style="min-width:160px;line-height:1.6">
        <strong>${d.regionName || '未知区域'}</strong><br/>
        <span style="color:#666">${d.country || ''} ${d.province || ''}</span><br/>
        类型: ${typeLabel}<br/>
        ${d.depthMin != null && d.depthMax != null ? `深度: ${d.depthMin}~${d.depthMax}m` : ''}
        ${d.habitatType ? `<br/>栖息地: ${d.habitatType}` : ''}
      </div>
    `)

    bounds.extend([d.latitude!, d.longitude!])
  })

  if (points.length === 1) {
    map.setView([points[0].latitude!, points[0].longitude!], 8)
  } else {
    map.fitBounds(bounds, { padding: [40, 40] })
  }
}

// ===== 编辑/删除 =====
function openEditDialog() {
  if (species.value) {
    Object.assign(editForm, { ...species.value })
    editDialogVisible.value = true
  }
}

async function handleEditSubmit() {
  await editFormRef.value?.validate()
  editSubmitting.value = true
  try {
    await updateSpecies(editForm.id!, editForm)
    ElMessage.success('物种更新成功')
    editDialogVisible.value = false
    loadData()
  } finally {
    editSubmitting.value = false
  }
}

async function handleDelete() {
  try {
    await deleteSpecies(species.value!.id!)
    ElMessage.success('物种删除成功')
    router.push('/species/list')
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.species-detail-page {
  animation: fadeIn 0.3s ease;
  max-width: 1100px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .header-left, .header-right { display: flex; align-items: center; gap: 8px; }
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== Hero 卡片 ===== */
.hero-card {
  position: relative;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  animation: fadeSlideUp 0.4s ease;
}

.hero-bg-accent {
  position: absolute;
  top: 0;
  right: 0;
  width: 40%;
  height: 100%;
  background: linear-gradient(135deg, rgba(15, 76, 117, 0.03), rgba(50, 130, 184, 0.06));
  clip-path: polygon(30% 0, 100% 0, 100% 100%, 0 100%);
  pointer-events: none;
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 28px 32px 20px;
  gap: 24px;
}

.hero-main { flex: 1; }

.hero-name {
  font-size: 28px;
  font-weight: 800;
  color: var(--neutral-800);
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}

.hero-sci {
  font-size: 16px;
  color: var(--neutral-500);
  margin: 0 0 14px;
}

.hero-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-code {
  font-size: 11px;
  font-weight: 600;
  color: var(--neutral-400);
  background: var(--neutral-100);
  padding: 3px 10px;
  border-radius: 10px;
  letter-spacing: 0.5px;
}

.hero-prot {
  font-size: 12px;
  color: #ea580c;
  background: rgba(234, 88, 12, 0.08);
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.hero-endemic {
  font-size: 12px;
  color: var(--primary-glow);
  background: rgba(0, 212, 170, 0.08);
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.hero-invasive {
  font-size: 12px;
  color: #dc2626;
  background: rgba(220, 38, 38, 0.08);
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.hero-iucn {
  flex-shrink: 0;
}

.iucn-current {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 90px;
  border-radius: var(--radius-md);
  color: #fff;
  .iucn-code { font-size: 26px; font-weight: 800; letter-spacing: 1px; line-height: 1; }
  .iucn-text { font-size: 10px; margin-top: 4px; opacity: 0.85; }
}

/* ===== IUCN 色带谱 ===== */
.iucn-spectrum {
  padding: 0 32px 24px;
}

.spectrum-bar {
  position: relative;
  height: 8px;
  border-radius: 4px;
  overflow: visible;
  margin-bottom: 8px;
}

.spectrum-gradient {
  width: 100%;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(
    to right,
    #1a1a2e 0%,
    #6b21a8 12%,
    #dc2626 25%,
    #ea580c 40%,
    #d97706 55%,
    #65a30d 72%,
    #059669 100%
  );
}

.spectrum-marker {
  position: absolute;
  top: -4px;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: left 0.5s cubic-bezier(0.4, 0, 0.2, 1);

  .marker-dot {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    border: 3px solid var(--neutral-800);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }

  .marker-line {
    width: 2px;
    height: 8px;
    background: var(--neutral-800);
    margin-top: -2px;
  }
}

.spectrum-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 1%;
}

.spectrum-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--neutral-400);
  letter-spacing: 0.3px;
  transition: all 0.2s;

  &.active {
    color: var(--neutral-800);
    transform: scale(1.2);
  }
}

/* ===== 通用卡片样式 ===== */
.info-card,
.taxonomy-card,
.text-card,
.distribution-card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px 28px;
  animation: fadeSlideUp 0.5s ease both;
}

.info-card { animation-delay: 0.1s; }
.taxonomy-card { animation-delay: 0.15s; }

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--neutral-800);
  margin: 0 0 18px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--neutral-100);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 40px;
    height: 2px;
    background: var(--primary-main);
  }
}

/* ===== 分类学进化链 ===== */
.taxonomy-chain {
  display: flex;
  align-items: center;
  gap: 0;
  overflow-x: auto;
  padding: 8px 0 12px;

  &::-webkit-scrollbar { height: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--neutral-200); border-radius: 2px; }
}

.chain-node {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  animation: fadeSlideUp 0.4s ease both;
  animation-delay: var(--node-delay, 0s);
}

.node-connector {
  display: flex;
  align-items: center;
  margin: 0 2px;
  svg { display: block; }
}

.node-pill {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 18px;
  border-radius: var(--radius-md);
  background: var(--neutral-50);
  border: 1.5px solid transparent;
  border-color: var(--node-color, var(--neutral-200));
  transition: all var(--transition-smooth);
  min-width: 80px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    background: #fff;
  }
}

.node-rank {
  font-size: 10px;
  font-weight: 600;
  color: var(--node-color, var(--neutral-400));
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.node-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--neutral-800);
  text-align: center;
  word-break: break-all;
}

/* ===== 形态 & 生态 双列 ===== */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.text-card {
  animation-delay: 0.2s;
}

.detail-text {
  line-height: 1.8;
  color: var(--neutral-600);
  font-size: 14px;
}

/* ===== 分布地图 ===== */
.distribution-card {
  animation-delay: 0.25s;
}

.map-wrapper {
  position: relative;
}

.leaf-map {
  height: 400px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--neutral-100);
}

.map-legend {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--neutral-600);

  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
  }
}

.map-empty {
  padding: 20px 0;
}

.dist-table-collapse {
  margin-top: 16px;

  :deep(.el-collapse-item__header) {
    font-size: 13px;
    color: var(--neutral-500);
    font-weight: 500;
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding-top: 8px;
  }
}
</style>
