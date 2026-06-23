<template>
  <div class="observation-list-page">
    <div class="page-header">
      <h2>观测记录</h2>
      <p>海洋生物观测数据管理</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row" v-if="stats">
      <div class="stat-card">
        <div class="stat-icon"><el-icon><DataLine /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalCount }}</span>
          <span class="stat-label">总记录数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><Calendar /></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.thisMonthCount }}</span>
          <span class="stat-label">本月新增</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><Sunny /></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.avgWaterTemperature }}<small> ℃</small></span>
          <span class="stat-label">平均水温</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><Odometer /></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.avgSalinity }}<small> PSU</small></span>
          <span class="stat-label">平均盐度</span>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-section">
      <div class="search-row">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索编号、观测员..."
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
        <el-button type="primary" size="large" @click="search">搜索</el-button>
        <el-button type="success" size="large" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增观测
        </el-button>
      </div>

      <div v-show="showAdvanced" class="advanced-filters">
        <div class="filter-group">
          <label>观测类型</label>
          <el-select v-model="queryParams.observationType" placeholder="全部" clearable>
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </div>
        <div class="filter-group">
          <label>日期范围</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
            style="width: 260px"
          />
        </div>
        <div class="filter-group">
          <label>生态系统</label>
          <el-select v-model="queryParams.ecosystemId" placeholder="全部" clearable>
            <el-option v-for="eco in ecosystemList" :key="eco.id" :label="eco.ecosystemName" :value="eco.id" />
          </el-select>
        </div>
        <div class="filter-group">
          <label>观测地点</label>
          <el-select v-model="queryParams.locationId" placeholder="全部" clearable filterable>
            <el-option v-for="loc in locationList" :key="loc.id" :label="loc.locationName" :value="loc.id" />
          </el-select>
        </div>
        <el-button @click="resetQuery" text type="primary" class="reset-btn">重置筛选</el-button>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="view-toolbar">
      <div class="result-info">
        共 <strong>{{ total }}</strong> 条记录
        <template v-if="activeFilterCount > 0">
          <span class="filter-badge">{{ activeFilterCount }} 项筛选</span>
        </template>
      </div>
      <div class="sort-control">
        <span class="sort-label">排序：</span>
        <button
          :class="['sort-btn', { active: queryParams.sort === 'createTime' }]"
          @click="changeSort('createTime')"
        >按创建日期</button>
        <button
          :class="['sort-btn', { active: queryParams.sort === 'observationCode' }]"
          @click="changeSort('observationCode')"
        >按编号</button>
        <button
          :class="['sort-btn', { active: queryParams.sort === 'observationDate' }]"
          @click="changeSort('observationDate')"
        >按观测日期</button>
      </div>
      <div class="view-switch">
        <button :class="['switch-btn', { active: viewMode === 'grid' }]" @click="viewMode = 'grid'" title="卡片视图">
          <el-icon><Grid /></el-icon>
        </button>
        <button :class="['switch-btn', { active: viewMode === 'table' }]" @click="viewMode = 'table'" title="表格视图">
          <el-icon><List /></el-icon>
        </button>
      </div>
    </div>

    <!-- 卡片视图 -->
    <div v-if="viewMode === 'grid'" v-loading="loading" class="bento-grid">
      <div
        v-for="(obs, idx) in observationList"
        :key="obs.id"
        class="observation-card"
        :style="{ animationDelay: `${idx * 0.04}s` }"
        @click="$router.push(`/observation/detail/${obs.id}`)"
      >
        <div class="card-body">
          <div class="card-top">
            <span class="card-code">{{ obs.observationCode }}</span>
            <span class="card-type-badge" :style="{ background: typeColor(obs.observationType), color: '#fff' }">
              {{ typeLabel(obs.observationType) }}
            </span>
          </div>
          <h3 class="card-title">{{ locationName(obs.locationId) || '未知地点' }}</h3>
          <p class="card-date">{{ obs.observationDate }} {{ obs.observationTime ? obs.observationTime.substring(0, 5) : '' }}</p>
          <p class="card-observer" v-if="obs.observerName">{{ obs.observerName }} · {{ obs.organization || '' }}</p>
          <div class="card-env-data">
            <span v-if="obs.waterTemperature" class="env-chip">{{ obs.waterTemperature }}℃</span>
            <span v-if="obs.salinity" class="env-chip">盐度 {{ obs.salinity }}</span>
            <span v-if="obs.depth" class="env-chip">{{ obs.depth }}m</span>
          </div>
          <div class="card-footer">
            <span class="sea-badge" v-if="obs.seaCondition">{{ seaLabel(obs.seaCondition) }}</span>
            <span class="weather-badge" v-if="obs.weatherCondition">{{ weatherLabel(obs.weatherCondition) }}</span>
          </div>
        </div>
        <div class="card-actions" @click.stop>
          <el-button link type="primary" size="small" @click="openEditDialog(obs)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(obs.id!)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <div v-if="!loading && observationList.length === 0" class="grid-empty">
        <el-empty description="暂无观测数据" />
      </div>
    </div>

    <!-- 表格视图 -->
    <el-card v-else class="table-card" v-loading="loading">
      <el-table :data="observationList" stripe>
        <el-table-column prop="observationCode" label="编号" width="110" />
        <el-table-column prop="observationType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :color="typeColor(row.observationType)" effect="dark" size="small" style="border: none">
              {{ typeLabel(row.observationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="observationDate" label="观测日期" width="120" />
        <el-table-column label="地点" width="140">
          <template #default="{ row }">{{ locationName(row.locationId) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="observerName" label="观测员" width="100" />
        <el-table-column prop="waterTemperature" label="水温(℃)" width="90" />
        <el-table-column prop="salinity" label="盐度" width="80" />
        <el-table-column label="生态系统" width="120">
          <template #default="{ row }">{{ ecosystemName(row.ecosystemId) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" show-overflow-tooltip min-width="200" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/observation/detail/${row.id}`)">详情</el-button>
            <el-button link type="warning" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此记录？" @confirm="handleDelete(row.id)">
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
            <el-form-item label="观测编号" prop="observationCode">
              <el-input v-model="formData.observationCode" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="观测类型" prop="observationType">
              <el-select v-model="formData.observationType" placeholder="选择类型">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="观测日期" prop="observationDate">
              <el-date-picker v-model="formData.observationDate" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="观测时间">
              <el-time-picker v-model="formData.observationTime" format="HH:mm" value-format="HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="formData.durationMinutes" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">位置与环境</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="观测地点">
              <el-select v-model="formData.locationId" placeholder="选择地点" clearable filterable @change="handleLocationChange">
                <el-option v-for="loc in locationList" :key="loc.id" :label="loc.locationName" :value="loc.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生态系统">
              <el-select v-model="formData.ecosystemId" placeholder="选择生态系统" clearable>
                <el-option v-for="eco in ecosystemList" :key="eco.id" :label="eco.ecosystemName" :value="eco.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="纬度">
              <el-input-number v-model="formData.latitude" :precision="6" :step="0.001" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经度">
              <el-input-number v-model="formData.longitude" :precision="6" :step="0.001" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="深度(m)">
              <el-input-number v-model="formData.depth" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">水质数据</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="水温(℃)">
              <el-input-number v-model="formData.waterTemperature" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="盐度(PSU)">
              <el-input-number v-model="formData.salinity" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="pH">
              <el-input-number v-model="formData.ph" :precision="2" :min="0" :max="14" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="天气状况">
              <el-select v-model="formData.weatherCondition" placeholder="选择" clearable>
                <el-option label="晴天" value="SUNNY" />
                <el-option label="多云" value="CLOUDY" />
                <el-option label="阴天" value="OVERCAST" />
                <el-option label="小雨" value="LIGHT_RAIN" />
                <el-option label="大雨" value="HEAVY_RAIN" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="海况">
              <el-select v-model="formData.seaCondition" placeholder="选择" clearable>
                <el-option label="平静" value="CALM" />
                <el-option label="轻微" value="SLIGHT" />
                <el-option label="中等" value="MODERATE" />
                <el-option label="粗糙" value="ROUGH" />
                <el-option label="非常粗糙" value="VERY_ROUGH" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">观测员信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="观测员" prop="observerName">
              <el-input v-model="formData.observerName" placeholder="姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属机构">
              <el-input v-model="formData.organization" placeholder="机构名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用设备">
              <el-input v-model="formData.equipmentUsed" placeholder="设备列表" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="观测备注">
          <el-input v-model="formData.notes" type="textarea" :rows="3" placeholder="观测详细记录..." />
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
import type { FormInstance, FormRules } from 'element-plus'
import {
  getObservationList,
  createObservation,
  updateObservation,
  deleteObservation,
  getObservationStats,
  getObservationLocations,
  getObservationEcosystems
} from '@/api/observation'
import type {
  Observation,
  ObservationQueryDTO,
  ObservationStatistics,
  ObservationLocation,
  Ecosystem
} from '@/types'

const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const observationList = ref<Observation[]>([])
const stats = ref<ObservationStatistics | null>(null)
const locationList = ref<ObservationLocation[]>([])
const ecosystemList = ref<Ecosystem[]>([])
const viewMode = ref<'grid' | 'table'>('table')
const showAdvanced = ref(false)
const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive<ObservationQueryDTO>({
  keyword: '',
  observationType: '',
  startDate: undefined,
  endDate: undefined,
  ecosystemId: undefined,
  locationId: undefined,
  observerName: '',
  sort: 'createTime',
  page: 1,
  size: 12
})

const typeOptions = [
  { value: 'DIVE', label: '潜水观测' },
  { value: 'SURVEY', label: '科学调查' },
  { value: 'SIGHTING', label: '目视观测' },
  { value: 'TRACKING', label: '追踪观测' },
  { value: 'AI_RECOGNITION', label: 'AI 识别' }
]

const activeFilterCount = computed(() => {
  let count = 0
  if (queryParams.observationType) count++
  if (queryParams.startDate || queryParams.endDate) count++
  if (queryParams.ecosystemId) count++
  if (queryParams.locationId) count++
  return count
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? '编辑观测记录' : '新增观测记录')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultFormData: Observation = {
  observationCode: '', observationType: '', observationDate: '',
  observationTime: undefined, durationMinutes: undefined,
  locationId: undefined, ecosystemId: undefined,
  latitude: undefined, longitude: undefined, depth: undefined,
  waterTemperature: undefined, salinity: undefined, ph: undefined,
  weatherCondition: '', seaCondition: '',
  observerName: '', organization: '', equipmentUsed: '', notes: ''
}

const formData = reactive<Observation>({ ...defaultFormData })

const formRules: FormRules = {
  observationType: [{ required: true, message: '请选择观测类型', trigger: 'change' }],
  observationDate: [{ required: true, message: '请选择观测日期', trigger: 'change' }],
  observerName: [{ required: true, message: '请输入观测员姓名', trigger: 'blur' }]
}

// ==================== 辅助函数 ====================

function typeColor(type: string): string {
  const map: Record<string, string> = {
    DIVE: '#0ea5e9', SURVEY: '#10b981', SIGHTING: '#f59e0b', TRACKING: '#8b5cf6'
  }
  return map[type] || '#6b7280'
}

function typeLabel(type: string): string {
  const map: Record<string, string> = {
    DIVE: '潜水', SURVEY: '调查', SIGHTING: '目视', TRACKING: '追踪'
  }
  return map[type] || type
}

function seaLabel(condition?: string): string {
  if (!condition) return ''
  const map: Record<string, string> = {
    CALM: '平静', SLIGHT: '轻微', MODERATE: '中等', ROUGH: '粗糙', VERY_ROUGH: '非常粗糙'
  }
  return map[condition] || condition
}

function weatherLabel(condition?: string): string {
  if (!condition) return ''
  const map: Record<string, string> = {
    SUNNY: '晴天', CLOUDY: '多云', OVERCAST: '阴天', LIGHT_RAIN: '小雨', HEAVY_RAIN: '大雨'
  }
  return map[condition] || condition
}

function locationName(id?: number): string {
  if (!id) return ''
  const loc = locationList.value.find(l => l.id === id)
  return loc?.locationName || ''
}

function ecosystemName(id?: number): string {
  if (!id) return ''
  const eco = ecosystemList.value.find(e => e.id === id)
  return eco?.ecosystemName || ''
}

function handleDateChange(val: [string, string] | null) {
  if (val) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = undefined
    queryParams.endDate = undefined
  }
  queryParams.page = 1
  search()
}

function handleLocationChange(locId: number | undefined) {
  if (locId) {
    const loc = locationList.value.find(l => l.id === locId)
    if (loc) {
      formData.latitude = loc.latitude
      formData.longitude = loc.longitude
    }
  }
}

// 高级筛选条件变更时自动搜索
watch(
  () => [queryParams.observationType, queryParams.ecosystemId, queryParams.locationId],
  () => {
    queryParams.page = 1
    search()
  }
)

// ==================== 数据加载 ====================

async function search() {
  loading.value = true
  try {
    const res: any = await getObservationList(queryParams)
    observationList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.observationType = ''
  queryParams.startDate = undefined
  queryParams.endDate = undefined
  queryParams.ecosystemId = undefined
  queryParams.locationId = undefined
  queryParams.observerName = ''
  queryParams.page = 1
  queryParams.size = 12
  dateRange.value = null
  search()
}

function changeSort(sort: string) {
  queryParams.sort = sort
  queryParams.page = 1
  search()
}

async function loadStats() {
  const res: any = await getObservationStats()
  stats.value = res.data
}

async function loadLocations() {
  const res: any = await getObservationLocations()
  locationList.value = res.data || []
}

async function loadEcosystems() {
  const res: any = await getObservationEcosystems()
  ecosystemList.value = res.data || []
}

// ==================== 对话框操作 ====================

function openAddDialog() {
  isEdit.value = false
  Object.assign(formData, { ...defaultFormData })
  dialogVisible.value = true
}

function openEditDialog(row: Observation) {
  isEdit.value = true
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateObservation(formData.id!, formData)
      ElMessage.success('观测记录更新成功')
    } else {
      await createObservation(formData)
      ElMessage.success('观测记录创建成功')
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
    await deleteObservation(id)
    ElMessage.success('观测记录删除成功')
    search()
    loadStats()
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

onMounted(() => {
  Promise.all([loadLocations(), loadEcosystems()]).then(() => {
    search()
    loadStats()

    // 检测 AI 识别页面跳转的预填数据（任务 3.5）
    const aiPrefill = history.state?.aiPrefill
    if (aiPrefill) {
      Object.assign(formData, { ...defaultFormData, ...aiPrefill })
      dialogVisible.value = true
      // 清除 state，避免刷新后重复触发
      history.replaceState({ ...history.state, aiPrefill: undefined }, '')
    }
  })
})
</script>

<style scoped lang="scss">
.observation-list-page {
  animation: fadeIn 0.3s ease;
}

.page-header {
  margin-bottom: 20px;
  h2 { margin: 0; font-size: 22px; font-weight: 700; }
  p { margin: 4px 0 0; color: var(--neutral-500); font-size: 14px; }
}

/* ===== 统计卡片 ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;

  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-smooth);
  &:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }

  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: linear-gradient(135deg, var(--primary-main), var(--primary-light));
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 20px;
    flex-shrink: 0;
  }

  .stat-info {
    display: flex;
    flex-direction: column;
  }

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: var(--neutral-800);
    line-height: 1.2;
    small { font-size: 13px; font-weight: 400; color: var(--neutral-500); }
  }

  .stat-label {
    font-size: 12px;
    color: var(--neutral-500);
    margin-top: 2px;
  }
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
  flex-wrap: wrap;

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
      &:not(:first-of-type) { border-left: none; }
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

.observation-card {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-smooth);
  animation: fadeSlideUp 0.5s ease both;
  background: #fff;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--neutral-100);

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-lg);
  }
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

.card-type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
  letter-spacing: 0.3px;
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--neutral-800);
  margin: 0 0 6px;
  line-height: 1.3;
}

.card-date {
  font-size: 13px;
  color: var(--neutral-500);
  margin: 0 0 4px;
}

.card-observer {
  font-size: 12px;
  color: var(--neutral-500);
  margin: 0 0 8px;
}

.card-env-data {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;

  .env-chip {
    font-size: 11px;
    color: var(--primary-light);
    background: rgba(50, 130, 184, 0.08);
    padding: 2px 8px;
    border-radius: 10px;
    font-weight: 500;
  }
}

.card-footer {
  display: flex;
  gap: 6px;
  margin-top: auto;
}

.sea-badge, .weather-badge {
  font-size: 11px;
  color: var(--neutral-600);
  background: var(--neutral-100);
  padding: 2px 8px;
  border-radius: 10px;
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

  .observation-card:hover & {
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
