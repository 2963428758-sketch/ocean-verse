<template>
  <div class="species-list-page">
    <div class="page-header">
      <h2>物种列表</h2>
      <p>浏览和管理海洋物种信息</p>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-section">
      <div class="search-row">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索物种名称、学名..."
          clearable
          class="search-input"
          size="large"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button
          :class="['filter-toggle-btn', { active: showAdvanced }]"
          @click="showAdvanced = !showAdvanced"
        >
          <el-icon><Filter /></el-icon>
          高级筛选
          <el-icon class="toggle-arrow" :class="{ rotated: showAdvanced }"><ArrowDown /></el-icon>
        </el-button>
        <el-button type="primary" size="large" @click="search">
          搜索
        </el-button>
        <el-button type="success" size="large" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增物种
        </el-button>
      </div>

      <div v-show="showAdvanced" class="advanced-filters">
        <div class="filter-group">
          <label>科</label>
          <el-select v-model="queryParams.family" placeholder="全部" clearable filterable>
            <el-option v-for="f in familyOptions" :key="f" :label="f" :value="f" />
          </el-select>
        </div>
        <div class="filter-group">
          <label>IUCN 等级</label>
          <el-select v-model="queryParams.iucnStatus" placeholder="全部" clearable>
            <el-option label="灭绝 (EX)" value="EX" />
            <el-option label="野外灭绝 (EW)" value="EW" />
            <el-option label="极危 (CR)" value="CR" />
            <el-option label="濒危 (EN)" value="EN" />
            <el-option label="易危 (VU)" value="VU" />
            <el-option label="近危 (NT)" value="NT" />
            <el-option label="无危 (LC)" value="LC" />
          </el-select>
        </div>
        <div class="filter-group">
          <label>保护等级</label>
          <el-select v-model="queryParams.protectionLevel" placeholder="全部" clearable>
            <el-option label="一级保护" value="1" />
            <el-option label="二级保护" value="2" />
            <el-option label="三级保护" value="3" />
          </el-select>
        </div>
        <el-button @click="resetQuery" text type="primary" class="reset-btn">重置筛选</el-button>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="view-toolbar">
      <div class="result-info">
        共 <strong>{{ total }}</strong> 个物种
        <template v-if="activeFilterCount > 0">
          <span class="filter-badge">{{ activeFilterCount }} 项筛选</span>
        </template>
      </div>
      <div class="sort-control">
        <span class="sort-label">排序：</span>
        <button
          :class="['sort-btn', { active: queryParams.sort === 'createTime' }]"
          @click="changeSort('createTime')"
        >
          按创建日期
        </button>
        <button
          :class="['sort-btn', { active: queryParams.sort === 'speciesCode' }]"
          @click="changeSort('speciesCode')"
        >
          按编号
        </button>
      </div>
      <div class="view-switch">
        <button
          :class="['switch-btn', { active: viewMode === 'grid' }]"
          @click="viewMode = 'grid'"
          title="卡片视图"
        >
          <el-icon><Grid /></el-icon>
        </button>
        <button
          :class="['switch-btn', { active: viewMode === 'table' }]"
          @click="viewMode = 'table'"
          title="表格视图"
        >
          <el-icon><List /></el-icon>
        </button>
      </div>
    </div>

    <!-- 卡片视图 (Bento Grid) -->
    <div v-if="viewMode === 'grid'" v-loading="loading" class="bento-grid">
      <div
        v-for="(sp, idx) in speciesList"
        :key="sp.id"
        :class="['species-card', `iucn-${(sp.iucnStatus || 'dd').toLowerCase()}`]"
        :style="{ animationDelay: `${idx * 0.04}s` }"
        @click="$router.push(`/species/detail/${sp.id}`)"
      >
        <!-- 图片轮播区域 -->
        <div class="card-media" v-if="speciesMediaMap[sp.id!] && speciesMediaMap[sp.id!].length > 0">
          <el-carousel
            v-if="speciesMediaMap[sp.id!].length > 1"
            :interval="3000"
            :indicator-position="'none'"
            height="140px"
            class="card-carousel"
            arrow="never"
          >
            <el-carousel-item v-for="media in speciesMediaMap[sp.id!]" :key="media.id">
              <img :src="media.fileUrl" :alt="media.fileName" class="card-img" />
            </el-carousel-item>
          </el-carousel>
          <img
            v-else
            :src="speciesMediaMap[sp.id!][0].fileUrl"
            :alt="speciesMediaMap[sp.id!][0].fileName"
            class="card-img"
          />
          <div class="card-media-overlay">
            <span class="media-count" v-if="speciesMediaMap[sp.id!].length > 1">
              {{ speciesMediaMap[sp.id!].length }} 张图片
            </span>
          </div>
        </div>
        <div class="card-media card-media-placeholder" v-else>
          <div class="placeholder-icon">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
              <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
              <circle cx="9" cy="10" r="1" fill="currentColor"/>
              <circle cx="15" cy="10" r="1" fill="currentColor"/>
            </svg>
          </div>
        </div>

        <div class="card-iucn-band" :style="{ background: iucnColor(sp.iucnStatus) }"></div>
        <div class="card-body">
          <div class="card-top">
            <span class="card-code">{{ sp.speciesCode }}</span>
            <span class="card-family" v-if="sp.family">{{ sp.family }}</span>
          </div>
          <h3 class="card-name">{{ sp.chineseName || sp.commonName || '未命名物种' }}</h3>
          <p class="card-sci"><em>{{ sp.scientificName }}</em></p>
          <p class="card-desc" v-if="sp.description">{{ sp.description }}</p>
          <div class="card-footer">
            <span class="iucn-badge" :style="{ background: iucnColor(sp.iucnStatus) }">
              {{ sp.iucnStatus || '—' }}
            </span>
            <span v-if="sp.protectionLevel" class="prot-badge">
              {{ protectionLabel(sp.protectionLevel) }}
            </span>
            <span v-if="sp.isEndemic === 1" class="endemic-dot" title="特有种"></span>
          </div>
        </div>
        <div class="card-actions" @click.stop>
          <el-button link type="primary" size="small" @click="openEditDialog(sp)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(sp.id!)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <div v-if="!loading && speciesList.length === 0" class="grid-empty">
        <el-empty description="暂无物种数据" />
      </div>
    </div>

    <!-- 表格视图 -->
    <el-card v-else class="table-card" v-loading="loading">
      <el-table :data="speciesList" stripe>
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <div class="table-thumb" v-if="speciesMediaMap[row.id] && speciesMediaMap[row.id].length > 0">
              <img :src="speciesMediaMap[row.id][0].fileUrl" class="thumb-img" />
              <span v-if="speciesMediaMap[row.id].length > 1" class="thumb-count">
                {{ speciesMediaMap[row.id].length }}
              </span>
            </div>
            <span v-else class="no-thumb">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="speciesCode" label="编码" width="100" />
        <el-table-column prop="chineseName" label="中文名" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/species/detail/${row.id}`)">
              {{ row.chineseName || '-' }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="scientificName" label="学名" width="200">
          <template #default="{ row }"><em>{{ row.scientificName }}</em></template>
        </el-table-column>
        <el-table-column prop="commonName" label="通用名" width="150">
          <template #default="{ row }">{{ row.commonName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="family" label="科" width="120" />
        <el-table-column prop="protectionLevel" label="保护等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.protectionLevel" :type="protectionTagType(row.protectionLevel)">
              {{ protectionLabel(row.protectionLevel) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="iucnStatus" label="IUCN" width="90">
          <template #default="{ row }">
            <el-tag :type="iucnTagType(row.iucnStatus)" size="small">
              {{ row.iucnStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isEndemic" label="特有种" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isEndemic === 1" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip min-width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/species/detail/${row.id}`)">详情</el-button>
            <el-button link type="warning" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此物种？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分页 -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="search"
        @size-change="search"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="750px" destroy-on-close>
      <el-form :model="formData" label-width="100px" :rules="formRules" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="物种编码" prop="speciesCode">
              <el-input v-model="formData.speciesCode" placeholder="如 SP004" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学名" prop="scientificName">
              <el-input v-model="formData.scientificName" placeholder="如 Dugong dugon" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="中文名">
              <el-input v-model="formData.chineseName" placeholder="如 儒艮" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通用名">
              <el-input v-model="formData.commonName" placeholder="如 Dugong" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="保护等级">
              <el-select v-model="formData.protectionLevel" placeholder="选择" clearable>
                <el-option label="一级" value="1" />
                <el-option label="二级" value="2" />
                <el-option label="三级" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="IUCN等级">
              <el-select v-model="formData.iucnStatus" placeholder="选择" clearable>
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
              <el-input v-model="formData.family" placeholder="如 Dugongidae" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">分类学信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="界"><el-input v-model="formData.kingdom" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="门"><el-input v-model="formData.phylum" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="纲"><el-input v-model="formData.className" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="目"><el-input v-model="formData.orderName" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="属"><el-input v-model="formData.genus" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="种"><el-input v-model="formData.species" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="特有种">
              <el-switch v-model="formData.isEndemic" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="入侵物种">
              <el-switch v-model="formData.isInvasive" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">描述信息</el-divider>
        <el-form-item label="简介">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="形态特征">
          <el-input v-model="formData.morphology" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="生态习性">
          <el-input v-model="formData.ecology" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-input v-model="formData.source" />
        </el-form-item>

        <!-- 地理分布信息 -->
        <el-divider content-position="left">分布信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input-number
                v-model="formData.longitude"
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
                v-model="formData.latitude"
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
            ref="uploadRef"
            action=""
            :auto-upload="false"
            :file-list="uploadFileList"
            :on-change="handleUploadChange"
            :on-remove="handleUploadRemove"
            :before-upload="beforeUpload"
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

        <!-- 已有图片展示（编辑模式） -->
        <el-form-item label="已有图片" v-if="isEdit && existingMedia.length > 0">
          <div class="existing-media-list">
            <div v-for="media in existingMedia" :key="media.id" class="existing-media-item">
              <img :src="media.fileUrl" :alt="media.fileName" />
              <div class="media-actions">
                <el-tag v-if="media.isPrimary === 1" type="warning" size="small">主图</el-tag>
                <el-button
                  v-if="media.isPrimary !== 1"
                  link type="primary" size="small"
                  @click="handleSetPrimary(media.id!)"
                >设为主图</el-button>
                <el-popconfirm title="确定删除此图片？" @confirm="handleDeleteMedia(media.id!)">
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadInstance } from 'element-plus'
import {
  getSpeciesList,
  getSpeciesDetail,
  createSpecies,
  updateSpecies,
  deleteSpecies,
  getSpeciesStats,
  getSpeciesMedia,
  uploadSpeciesMedia,
  deleteSpeciesMedia,
  setPrimaryMedia
} from '@/api/species'
import type { Species, SpeciesQueryDTO, SpeciesStatistics, SpeciesMedia } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const speciesList = ref<Species[]>([])
const stats = ref<SpeciesStatistics | null>(null)
const familyOptions = ref<string[]>([])
const viewMode = ref<'grid' | 'table'>('grid')
const showAdvanced = ref(false)

// 物种图片映射：speciesId -> SpeciesMedia[]
const speciesMediaMap = reactive<Record<number, SpeciesMedia[]>>({})

const queryParams = reactive<SpeciesQueryDTO>({
  keyword: '',
  family: '',
  iucnStatus: '',
  protectionLevel: '',
  sort: 'createTime',
  page: 1,
  size: 12
})

const activeFilterCount = computed(() => {
  let count = 0
  if (queryParams.family) count++
  if (queryParams.iucnStatus) count++
  if (queryParams.protectionLevel) count++
  return count
})

// IUCN 颜色谱 — 从绿到红的渐变，直观映射濒危程度
const iucnColorMap: Record<string, string> = {
  EX: '#1a1a2e',
  EW: '#6b21a8',
  CR: '#dc2626',
  EN: '#ea580c',
  VU: '#d97706',
  NT: '#65a30d',
  LC: '#059669',
  DD: '#9ca3af'
}

function iucnColor(status?: string): string {
  return iucnColorMap[status || 'DD'] || '#9ca3af'
}

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? '编辑物种' : '新增物种')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const uploadFileList = ref<UploadFile[]>([])
const existingMedia = ref<SpeciesMedia[]>([])
const pendingUploadFiles = ref<File[]>([])

const formData = reactive<Species>({
  speciesCode: '', scientificName: '', chineseName: '', commonName: '',
  protectionLevel: '', iucnStatus: '', kingdom: '', phylum: '', className: '',
  orderName: '', family: '', genus: '', species: '',
  description: '', morphology: '', ecology: '',
  isEndemic: 0, isInvasive: 0, source: '',
  longitude: undefined, latitude: undefined
})

const formRules: FormRules = {
  speciesCode: [{ required: true, message: '请输入物种编码', trigger: 'blur' }],
  scientificName: [{ required: true, message: '请输入学名', trigger: 'blur' }]
}

function iucnTagType(status: string) {
  const map: Record<string, string> = { CR: 'danger', EN: 'warning', VU: '', NT: 'success', LC: 'success' }
  return (map[status] || 'info') as any
}

function protectionTagType(level: string) {
  const map: Record<string, string> = { '1': 'danger', '2': 'warning', '3': 'success' }
  return (map[level] || 'info') as any
}

function protectionLabel(level: string) {
  const map: Record<string, string> = { '1': '一级', '2': '二级', '3': '三级' }
  return map[level] || level
}

// ==================== 图片上传相关 ====================

function beforeUpload(file: File) {
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

function handleUploadChange(file: UploadFile) {
  if (file.raw) {
    if (beforeUpload(file.raw)) {
      pendingUploadFiles.value.push(file.raw)
    }
  }
}

function handleUploadRemove(file: UploadFile) {
  const idx = pendingUploadFiles.value.findIndex(f => f.name === file.name)
  if (idx > -1) {
    pendingUploadFiles.value.splice(idx, 1)
  }
}

async function handleDeleteMedia(mediaId: number) {
  try {
    await deleteSpeciesMedia(mediaId)
    ElMessage.success('图片已删除')
    // 刷新已有图片列表
    if (formData.id) {
      const res: any = await getSpeciesMedia(formData.id)
      existingMedia.value = res.data || []
    }
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

async function handleSetPrimary(mediaId: number) {
  try {
    await setPrimaryMedia(mediaId)
    ElMessage.success('主图已更新')
    if (formData.id) {
      const res: any = await getSpeciesMedia(formData.id)
      existingMedia.value = res.data || []
    }
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

// 高级筛选条件变更时自动搜索
watch(
  () => [queryParams.family, queryParams.iucnStatus, queryParams.protectionLevel],
  () => {
    queryParams.page = 1
    search()
  }
)

// ==================== 数据加载 ====================

async function search() {
  loading.value = true
  try {
    const res: any = await getSpeciesList(queryParams)
    speciesList.value = res.data?.records || []
    total.value = res.data?.total || 0
    // 加载每个物种的图片
    await loadAllSpeciesMedia()
  } finally {
    loading.value = false
  }
}

async function loadAllSpeciesMedia() {
  const promises = speciesList.value.map(async (sp) => {
    if (sp.id) {
      try {
        const res: any = await getSpeciesMedia(sp.id)
        speciesMediaMap[sp.id] = res.data || []
      } catch {
        speciesMediaMap[sp.id] = []
      }
    }
  })
  await Promise.all(promises)
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.family = ''
  queryParams.iucnStatus = ''
  queryParams.protectionLevel = ''
  queryParams.page = 1
  queryParams.size = 12
  // watcher 会自动触发 search()，无需手动调用
}

function changeSort(sort: string) {
  queryParams.sort = sort
  queryParams.page = 1
  search()
}

async function loadStats() {
  const res: any = await getSpeciesStats()
  stats.value = res.data
  if (stats.value?.byFamily) {
    familyOptions.value = Object.keys(stats.value.byFamily)
  }
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined, speciesCode: '', scientificName: '', chineseName: '', commonName: '',
    protectionLevel: '', iucnStatus: '', kingdom: '', phylum: '', className: '',
    orderName: '', family: '', genus: '', species: '',
    description: '', morphology: '', ecology: '',
    isEndemic: 0, isInvasive: 0, source: '',
    longitude: undefined, latitude: undefined
  })
  uploadFileList.value = []
  pendingUploadFiles.value = []
  existingMedia.value = []
  dialogVisible.value = true
}

async function openEditDialog(row: Species) {
  isEdit.value = true
  Object.assign(formData, { ...row })
  // 获取完整物种详情（含经纬度，列表接口不返回）
  try {
    const detailRes: any = await getSpeciesDetail(row.id!)
    if (detailRes.data) {
      Object.assign(formData, detailRes.data)
    }
  } catch {
    // 详情加载失败时仍使用列表数据
  }
  uploadFileList.value = []
  pendingUploadFiles.value = []
  // 加载已有图片
  try {
    const res: any = await getSpeciesMedia(row.id!)
    existingMedia.value = res.data || []
  } catch {
    existingMedia.value = []
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSpecies(formData.id!, formData)
      // 上传新图片
      if (pendingUploadFiles.value.length > 0) {
        await uploadSpeciesMedia(formData.id!, pendingUploadFiles.value)
      }
      ElMessage.success('物种更新成功')
    } else {
      const createRes: any = await createSpecies(formData)
      // 创建物种后上传图片（需要获取新创建的物种ID）
      if (pendingUploadFiles.value.length > 0) {
        // 从返回中获取新物种ID
        const newSpeciesId = createRes.data
        if (newSpeciesId) {
          await uploadSpeciesMedia(newSpeciesId, pendingUploadFiles.value)
        }
      }
      ElMessage.success('物种创建成功')
    }
    dialogVisible.value = false
    search()
    loadStats()
  } catch (e: any) {
    // 错误已由 http 拦截器处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteSpecies(id)
    ElMessage.success('物种删除成功')
    search()
    loadStats()
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

onMounted(() => {
  search()
  loadStats()

  // 检测 AI 识别页面跳转的预填数据（一键新增物种）
  const aiPrefillStr = sessionStorage.getItem('speciesAiPrefill')
  if (aiPrefillStr) {
    sessionStorage.removeItem('speciesAiPrefill')
    try {
      const aiPrefill = JSON.parse(aiPrefillStr)
      openAddDialog()
      // openAddDialog 先重置所有字段，再用 aiPrefill 覆盖
      Object.assign(formData, aiPrefill)
      dialogVisible.value = true
      ElMessage.info('已自动填入 AI 识别结果，请核实后提交')
    } catch (e) {
      console.error('解析物种预填数据失败:', e)
    }
  }
})
</script>

<style scoped lang="scss">
.species-list-page {
  animation: fadeIn 0.3s ease;
}

.page-header {
  margin-bottom: 20px;
  h2 { margin: 0; font-size: 22px; font-weight: 700; }
  p { margin: 4px 0 0; color: var(--neutral-500); font-size: 14px; }
}

/* ===== 搜索与筛选区 ===== */
.filter-section {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-smooth);
  &:hover { box-shadow: var(--shadow-md); }
}

.search-row {
  display: flex;
  gap: 12px;
  align-items: center;

  .search-input { flex: 1; }

  .filter-toggle-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border: 1px solid var(--neutral-200);
    border-radius: var(--radius-md);
    background: #fff;
    color: var(--neutral-600);
    cursor: pointer;
    font-size: 14px;
    transition: all var(--transition-fast);
    &:hover { border-color: var(--primary-lighter); color: var(--primary-light); }
    &.active { background: var(--primary-main); color: #fff; border-color: var(--primary-main); }
    .toggle-arrow {
      transition: transform 0.3s ease;
      font-size: 12px;
      &.rotated { transform: rotate(180deg); }
    }
  }
}

.advanced-filters {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--neutral-100);
  animation: fadeSlideUp 0.3s ease;

  .filter-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
    label { font-size: 12px; color: var(--neutral-500); font-weight: 500; letter-spacing: 0.3px; }
    .el-select { width: 160px; }
  }

  .reset-btn {
    margin-left: auto;
    font-size: 13px;
  }
}

/* ===== 工具栏 ===== */
.view-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .result-info {
    font-size: 14px;
    color: var(--neutral-500);
    strong { color: var(--neutral-800); font-weight: 600; }
    .filter-badge {
      display: inline-block;
      margin-left: 8px;
      padding: 2px 8px;
      background: var(--primary-main);
      color: #fff;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 500;
    }
  }

  .sort-control {
    display: flex;
    align-items: center;
    gap: 4px;

    .sort-label {
      font-size: 13px;
      color: var(--neutral-500);
      margin-right: 4px;
    }

    .sort-btn {
      padding: 4px 12px;
      border: 1px solid var(--neutral-200);
      background: #fff;
      color: var(--neutral-500);
      cursor: pointer;
      font-size: 13px;
      transition: all var(--transition-fast);
      &:first-of-type { border-radius: 4px 0 0 4px; }
      &:last-of-type { border-radius: 0 4px 4px 0; border-left: none; }
      &:hover { color: var(--primary-light); border-color: var(--primary-lighter); }
      &.active {
        background: var(--primary-main);
        color: #fff;
        border-color: var(--primary-main);
      }
    }
  }

  .view-switch {
    display: flex;
    background: #fff;
    border-radius: var(--radius-sm);
    padding: 3px;
    box-shadow: var(--shadow-sm);
    border: 1px solid var(--neutral-100);

    .switch-btn {
      padding: 6px 12px;
      border: none;
      background: transparent;
      border-radius: 4px;
      cursor: pointer;
      color: var(--neutral-400);
      font-size: 16px;
      display: flex;
      align-items: center;
      transition: all var(--transition-fast);
      &:hover { color: var(--neutral-600); }
      &.active {
        background: var(--primary-main);
        color: #fff;
        box-shadow: 0 1px 3px rgba(15, 76, 117, 0.3);
      }
    }
  }
}

/* ===== Bento Grid ===== */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  min-height: 200px;

  @media (max-width: 1100px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 480px) { grid-template-columns: 1fr; }
}

.species-card {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-smooth);
  animation: fadeSlideUp 0.5s ease both;

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-lg);
  }

  background: #fff;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--neutral-100);
}

/* ===== 卡片图片区域 ===== */
.card-media {
  position: relative;
  height: 140px;
  overflow: hidden;
  background: var(--neutral-100);
}

.card-media-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4f8, #d4e8f0);

  .placeholder-icon {
    color: var(--neutral-300);
    opacity: 0.6;
  }
}

.card-img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}

.card-carousel {
  :deep(.el-carousel__container) {
    height: 140px !important;
  }
}

.card-media-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 4px 10px;
  background: linear-gradient(transparent, rgba(0,0,0,0.4));
  display: flex;
  justify-content: flex-end;

  .media-count {
    font-size: 11px;
    color: #fff;
    background: rgba(0,0,0,0.5);
    padding: 1px 8px;
    border-radius: 10px;
    backdrop-filter: blur(4px);
  }
}

.card-iucn-band {
  height: 4px;
  width: 100%;
  transition: height var(--transition-smooth);
  .species-card:hover & { height: 6px; }
}

.card-body {
  padding: 14px 16px 12px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-code {
  font-size: 11px;
  font-weight: 600;
  color: var(--neutral-400);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.card-family {
  font-size: 11px;
  color: var(--primary-lighter);
  background: rgba(50, 130, 184, 0.08);
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.card-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--neutral-800);
  margin: 0 0 4px;
  line-height: 1.3;
}

.card-sci {
  font-size: 13px;
  color: var(--neutral-500);
  margin: 0 0 8px;
  em { font-style: italic; }
}

.card-desc {
  font-size: 12px;
  color: var(--neutral-500);
  line-height: 1.5;
  flex: 1;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin: 0 0 8px;
}

.card-footer {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: auto;
}

.iucn-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 10px;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.prot-badge {
  font-size: 11px;
  color: var(--neutral-600);
  background: var(--neutral-100);
  padding: 2px 8px;
  border-radius: 10px;
}

.endemic-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-glow);
  animation: pulseGlow 2s ease infinite;
  flex-shrink: 0;
}

.card-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transform: translateY(-4px);
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-radius: var(--radius-sm);
  padding: 4px 8px;
  box-shadow: var(--shadow-sm);
  z-index: 10;

  .species-card:hover & {
    opacity: 1;
    transform: translateY(0);
  }
}

.grid-empty {
  grid-column: 1 / -1;
  padding: 60px 0;
}

/* ===== 表格视图 ===== */
.table-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  :deep(.el-card__body) { padding: 0; }
}

.table-thumb {
  position: relative;
  width: 50px;
  height: 50px;

  .thumb-img {
    width: 50px;
    height: 50px;
    object-fit: cover;
    border-radius: 6px;
    border: 1px solid var(--neutral-100);
  }

  .thumb-count {
    position: absolute;
    bottom: -4px;
    right: -4px;
    background: var(--primary-main);
    color: #fff;
    font-size: 10px;
    font-weight: 600;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid #fff;
  }
}

.no-thumb {
  color: var(--neutral-300);
  font-size: 18px;
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

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
