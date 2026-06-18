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

      <!-- 物种图片画廊 -->
      <div class="media-gallery-card" v-if="mediaList.length > 0">
        <h3 class="section-title">物种图片</h3>
        <div class="gallery-main">
          <div class="gallery-carousel">
            <el-carousel
              :interval="4000"
              height="400px"
              indicator-position="outside"
              arrow="always"
              class="detail-carousel"
            >
              <el-carousel-item v-for="media in mediaList" :key="media.id">
                <div class="carousel-slide">
                  <img :src="media.fileUrl" :alt="media.mediaTitle || media.fileName" />
                  <div class="slide-caption" v-if="media.mediaTitle">
                    {{ media.mediaTitle }}
                    <el-tag v-if="media.isPrimary === 1" type="warning" size="small" class="primary-tag">主图</el-tag>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>
          <div class="gallery-thumbs" v-if="mediaList.length > 1">
            <div
              v-for="(media, idx) in mediaList"
              :key="media.id"
              :class="['thumb-item', { active: idx === activeThumbIdx }]"
              @click="activeThumbIdx = idx"
            >
              <img :src="media.fileUrl" :alt="media.fileName" />
              <el-tag v-if="media.isPrimary === 1" type="warning" size="small" class="thumb-primary-tag">主</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 无图片占位 -->
      <div class="media-gallery-card media-empty" v-else>
        <h3 class="section-title">物种图片</h3>
        <div class="gallery-placeholder">
          <div class="placeholder-content">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <path d="M21 15l-5-5L5 21"/>
            </svg>
            <p>暂无图片，点击编辑上传</p>
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
          <div ref="mapContainer" class="detail-map"></div>
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

        <!-- 分布数据表格 (按钮展开) -->
        <div class="dist-table-section">
          <button class="dist-toggle-btn" @click="showDistTable = !showDistTable">
            <el-icon class="dist-toggle-icon" :class="{ expanded: showDistTable }">
              <ArrowDown />
            </el-icon>
            {{ showDistTable ? '收起分布数据' : '查看详细分布数据' }}
          </button>
          <el-table v-show="showDistTable" :data="distributions" stripe size="small" class="dist-table">
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
        </div>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="物种不存在" />

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑物种" width="750px" destroy-on-close>
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

        <!-- 地理分布信息 -->
        <el-divider content-position="left">分布信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number
                v-model="editForm.longitude"
                :min="-180"
                :max="180"
                :precision="6"
                :step="1"
                controls-position="right"
                placeholder="如 120.345678"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input-number
                v-model="editForm.latitude"
                :min="-90"
                :max="90"
                :precision="6"
                :step="1"
                controls-position="right"
                placeholder="如 30.123456"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 图片上传区域 -->
        <el-divider content-position="left">物种图片</el-divider>
        <el-form-item label="上传图片">
          <el-upload
            action=""
            :auto-upload="false"
            :file-list="editUploadFileList"
            :on-change="handleEditUploadChange"
            :on-remove="handleEditUploadRemove"
            :before-upload="beforeEditUpload"
            multiple
            accept="image/*"
            list-type="picture-card"
            :limit="10"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="upload-tip">支持 jpg/png/gif 格式，单张不超过 20MB，最多 10 张</div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 已有图片管理 -->
        <el-form-item label="已有图片" v-if="editExistingMedia.length > 0">
          <div class="existing-media-list">
            <div v-for="media in editExistingMedia" :key="media.id" class="existing-media-item">
              <img :src="media.fileUrl" :alt="media.fileName" />
              <div class="media-actions">
                <el-tag v-if="media.isPrimary === 1" type="warning" size="small">主图</el-tag>
                <el-button
                  v-if="media.isPrimary !== 1"
                  link type="primary" size="small"
                  @click="handleEditSetPrimary(media.id!)"
                >设为主图</el-button>
                <el-popconfirm title="确定删除此图片？" @confirm="handleEditDeleteMedia(media.id!)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
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
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ArrowLeft, ArrowDown } from '@element-plus/icons-vue'
import {
  getSpeciesDetail,
  getSpeciesDistributions,
  updateSpecies,
  deleteSpecies,
  getSpeciesMedia,
  uploadSpeciesMedia,
  deleteSpeciesMedia,
  setPrimaryMedia
} from '@/api/species'
import type { Species, SpeciesDistribution, SpeciesMedia } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const species = ref<Species | null>(null)
const distributions = ref<SpeciesDistribution[]>([])
const showDistTable = ref(false)
const mediaList = ref<SpeciesMedia[]>([])
const activeThumbIdx = ref(0)
const mapContainer = ref<HTMLElement>()
const mapInstance = ref<any>(null)

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editUploadFileList = ref<UploadFile[]>([])
const editExistingMedia = ref<SpeciesMedia[]>([])
const editPendingFiles = ref<File[]>([])

const editForm = reactive<Species>({
  speciesCode: '',
  scientificName: '',
  chineseName: '',
  commonName: '',
  protectionLevel: '',
  iucnStatus: '',
  family: '',
  isEndemic: 0,
  isInvasive: 0,
  longitude: undefined,
  latitude: undefined
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

// ===== 图片上传相关 =====
function beforeEditUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt20M) {
    ElMessage.error('图片大小不能超过 20MB')
    return false
  }
  return true
}

function handleEditUploadChange(file: UploadFile) {
  if (file.raw) {
    if (beforeEditUpload(file.raw)) {
      editPendingFiles.value.push(file.raw)
    }
  }
}

function handleEditUploadRemove(file: UploadFile) {
  const idx = editPendingFiles.value.findIndex(f => f.name === file.name)
  if (idx > -1) {
    editPendingFiles.value.splice(idx, 1)
  }
}

async function handleEditDeleteMedia(mediaId: number) {
  try {
    await deleteSpeciesMedia(mediaId)
    ElMessage.success('图片已删除')
    // 刷新已有图片
    if (species.value?.id) {
      const res: any = await getSpeciesMedia(species.value.id)
      editExistingMedia.value = res.data || []
    }
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

async function handleEditSetPrimary(mediaId: number) {
  try {
    await setPrimaryMedia(mediaId)
    ElMessage.success('主图已更新')
    if (species.value?.id) {
      const res: any = await getSpeciesMedia(species.value.id)
      editExistingMedia.value = res.data || []
    }
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

// ===== 高德地图动态加载 =====
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

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const speciesId = Number(route.params.id)
    const res: any = await getSpeciesDetail(speciesId)
    species.value = res.data

    const distRes: any = await getSpeciesDistributions(speciesId)
    distributions.value = distRes.data || []

    // 加载媒体资源
    const mediaRes: any = await getSpeciesMedia(speciesId)
    mediaList.value = mediaRes.data || []

    await nextTick()
    await initMap()
  } finally {
    loading.value = false
  }
}

/** 销毁旧地图实例，释放 WebGL 资源和 DOM 引用 */
function destroyMap() {
  if (mapInstance.value) {
    mapInstance.value.destroy()
    mapInstance.value = null
  }
}

async function initMap() {
  destroyMap()
  if (!mapContainer.value || distributions.value.length === 0) return

  const points = distributions.value
    .filter(d => d.latitude != null && d.longitude != null)

  if (points.length === 0) return

  await loadAMap()
  const AMap = (window as any).AMap

  const map = new AMap.Map(mapContainer.value, {
    zoom: 5,
    center: [112, 25],
    viewMode: '2D'
  })
  mapInstance.value = map

  const markers: any[] = []

  points.forEach(d => {
    const color = distTypeColors[d.distributionType] || '#6b7280'
    const circleMarker = new AMap.CircleMarker({
      center: [d.longitude!, d.latitude!],
      radius: 8,
      fillColor: color,
      fillOpacity: 0.85,
      strokeColor: '#ffffff',
      strokeWeight: 2,
      strokeOpacity: 1,
      cursor: 'pointer'
    })

    const typeLabel = distTypeLabel(d.distributionType)
    const infoWindow = new AMap.InfoWindow({
      content:
        `<div style="min-width:160px;line-height:1.6;padding:4px 8px;font-size:13px">` +
        `<strong>${d.regionName || '未知区域'}</strong><br/>` +
        `<span style="color:#666">${d.country || ''} ${d.province || ''}</span><br/>` +
        `类型: <span style="color:${color};font-weight:600">${typeLabel}</span><br/>` +
        (d.depthMin != null && d.depthMax != null ? `深度: ${d.depthMin}~${d.depthMax}m<br/>` : '') +
        (d.habitatType ? `栖息地: ${d.habitatType}` : '') +
        `</div>`,
      offset: new AMap.Pixel(0, -10)
    })

    circleMarker.on('click', () => {
      infoWindow.open(map, circleMarker.getCenter())
    })

    map.add(circleMarker)
    markers.push(circleMarker)
  })

  if (markers.length > 0) {
    map.setFitView(markers, false, [60, 60, 60, 60], 10)
  }
}

// 路由参数变化时重新加载数据（处理同组件不同路由的复用场景）
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadData()
  }
})

onBeforeUnmount(() => {
  destroyMap()
})

// ===== 编辑/删除 =====
async function openEditDialog() {
  if (species.value) {
    Object.assign(editForm, { ...species.value })
    editUploadFileList.value = []
    editPendingFiles.value = []
    // 加载已有图片
    try {
      const res: any = await getSpeciesMedia(species.value.id!)
      editExistingMedia.value = res.data || []
    } catch {
      editExistingMedia.value = []
    }
    editDialogVisible.value = true
  }
}

async function handleEditSubmit() {
  await editFormRef.value?.validate()
  editSubmitting.value = true
  try {
    await updateSpecies(editForm.id!, editForm)
    // 上传新图片
    if (editPendingFiles.value.length > 0 && editForm.id) {
      await uploadSpeciesMedia(editForm.id, editPendingFiles.value)
    }
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

/* ===== 图片画廊 ===== */
.media-gallery-card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 24px 28px;
  animation: fadeSlideUp 0.45s ease both;
  animation-delay: 0.05s;
}

.gallery-main {
  display: flex;
  gap: 16px;
}

.gallery-carousel {
  flex: 1;
  min-width: 0;
}

.detail-carousel {
  border-radius: var(--radius-md);
  overflow: hidden;

  :deep(.el-carousel__container) {
    border-radius: var(--radius-md);
  }
}

.carousel-slide {
  width: 100%;
  height: 100%;
  position: relative;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .slide-caption {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px 16px;
    background: linear-gradient(transparent, rgba(0,0,0,0.6));
    color: #fff;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;

    .primary-tag {
      margin-left: auto;
    }
  }
}

.gallery-thumbs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 80px;
  flex-shrink: 0;
  overflow-y: auto;
  max-height: 400px;
  padding-right: 4px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--neutral-200); border-radius: 2px; }
}

.thumb-item {
  position: relative;
  width: 80px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &:hover {
    border-color: var(--primary-lighter);
  }

  &.active {
    border-color: var(--primary-main);
    box-shadow: 0 2px 8px rgba(15, 76, 117, 0.3);
  }

  .thumb-primary-tag {
    position: absolute;
    top: 2px;
    right: 2px;
    transform: scale(0.7);
    transform-origin: top right;
  }
}

.gallery-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;

  .placeholder-content {
    text-align: center;
    color: var(--neutral-300);

    p {
      margin: 12px 0 0;
      font-size: 14px;
      color: var(--neutral-400);
    }
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
  animation: fadeSlideUp 0.4s ease;
}

.map-wrapper {
  position: relative;
}

.detail-map {
  height: 400px;
  border-radius: var(--radius-md);
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

.dist-table-section {
  margin-top: 16px;
}

.dist-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--primary-600, #2563eb), var(--primary-700, #1d4ed8));
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);

  &:hover {
    background: linear-gradient(135deg, var(--primary-700, #1d4ed8), var(--primary-800, #1e40af));
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(37, 99, 235, 0.2);
  }
}

.dist-toggle-icon {
  font-size: 14px;
  transition: transform 0.25s ease;

  &.expanded {
    transform: rotate(180deg);
  }
}

.dist-table {
  margin-top: 12px;
}

/* ===== 图片上传区域 ===== */
.upload-tip {
  font-size: 12px;
  color: var(--neutral-400);
  margin-top: 4px;
}

.existing-media-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.existing-media-item {
  position: relative;
  width: 104px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--neutral-100);

  img {
    width: 104px;
    height: 104px;
    object-fit: cover;
    display: block;
  }

  .media-actions {
    display: flex;
    justify-content: center;
    gap: 4px;
    padding: 4px 0;
    background: var(--neutral-50);
  }
}
</style>
