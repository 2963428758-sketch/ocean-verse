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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getSpeciesList,
  createSpecies,
  updateSpecies,
  deleteSpecies,
  getSpeciesStats
} from '@/api/species'
import type { Species, SpeciesQueryDTO, SpeciesStatistics } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const speciesList = ref<Species[]>([])
const stats = ref<SpeciesStatistics | null>(null)
const familyOptions = ref<string[]>([])
const viewMode = ref<'grid' | 'table'>('grid')
const showAdvanced = ref(false)

const queryParams = reactive<SpeciesQueryDTO>({
  keyword: '',
  family: '',
  iucnStatus: '',
  protectionLevel: '',
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

function gridSpan(status?: string): number {
  const map: Record<string, number> = { CR: 2, EN: 2, VU: 1, NT: 1, LC: 1, EX: 1, EW: 1, DD: 1 }
  return map[status || 'DD'] || 1
}

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? '编辑物种' : '新增物种')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<Species>({
  speciesCode: '', scientificName: '', chineseName: '', commonName: '',
  protectionLevel: '', iucnStatus: '', kingdom: '', phylum: '', className: '',
  orderName: '', family: '', genus: '', species: '',
  description: '', morphology: '', ecology: '',
  isEndemic: 0, isInvasive: 0, source: ''
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

async function search() {
  loading.value = true
  try {
    const res: any = await getSpeciesList(queryParams)
    speciesList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.family = ''
  queryParams.iucnStatus = ''
  queryParams.protectionLevel = ''
  queryParams.page = 1
  queryParams.size = 12
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
    isEndemic: 0, isInvasive: 0, source: ''
  })
  dialogVisible.value = true
}

function openEditDialog(row: Species) {
  isEdit.value = true
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSpecies(formData.id!, formData)
      ElMessage.success('物种更新成功')
    } else {
      await createSpecies(formData)
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
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  min-height: 200px;

  @media (max-width: 1400px) { grid-template-columns: repeat(4, 1fr); }
  @media (max-width: 1100px) { grid-template-columns: repeat(3, 1fr); }
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

  /* Bento sizing based on IUCN */
  &.iucn-cr, &.iucn-en {
    grid-column: span 2;
    .card-body { min-height: 180px; }
    .card-name { font-size: 20px; }
  }
  &.iucn-vu {
    grid-column: span 2;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-lg);
  }

  background: #fff;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--neutral-100);
}

.card-iucn-band {
  height: 4px;
  width: 100%;
  transition: height var(--transition-smooth);
  .species-card:hover & { height: 6px; }
}

.card-body {
  padding: 14px 16px 12px;
  min-height: 140px;
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

/* ===== 分页 ===== */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
