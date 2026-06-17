<template>
  <div class="observation-detail-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="$router.push('/observation/list')" text>
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
      </div>
      <div class="header-right" v-if="observation">
        <el-button type="warning" @click="openEditDialog">编辑</el-button>
        <el-popconfirm title="确定删除此观测记录？" @confirm="handleDelete">
          <template #reference>
            <el-button type="danger">删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <div v-if="observation" class="detail-body">
      <!-- Hero 卡片 -->
      <div class="hero-card">
        <div class="hero-bg-accent"></div>
        <div class="hero-content">
          <div class="hero-main">
            <h2 class="hero-code">{{ observation.observationCode }}</h2>
            <p class="hero-type">
              <span class="type-badge" :style="{ background: typeColor(observation.observationType) }">
                {{ typeLabel(observation.observationType) }}
              </span>
            </p>
            <div class="hero-badges">
              <span class="hero-date">{{ observation.observationDate }}</span>
              <span v-if="observation.observationTime" class="hero-time">
                {{ observation.observationTime.substring(0, 5) }}
              </span>
              <span v-if="observation.durationMinutes" class="hero-duration">
                {{ observation.durationMinutes }} 分钟
              </span>
            </div>
          </div>
          <div class="hero-observer">
            <div class="observer-avatar">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="observer-info">
              <span class="observer-name">{{ observation.observerName || '未知' }}</span>
              <span class="observer-org">{{ observation.organization || '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 详细信息区域 -->
      <div class="info-grid">
        <!-- 基础信息 -->
        <div class="info-card">
          <h3 class="section-title">基础信息</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="观测编号">{{ observation.observationCode }}</el-descriptions-item>
            <el-descriptions-item label="观测类型">
              <el-tag :color="typeColor(observation.observationType)" effect="dark" size="small" style="border: none">
                {{ typeLabel(observation.observationType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="观测日期">{{ observation.observationDate }}</el-descriptions-item>
            <el-descriptions-item label="观测时间">{{ observation.observationTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="持续时长">{{ observation.durationMinutes ? observation.durationMinutes + ' 分钟' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ observation.createTime }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 位置信息 -->
        <div class="info-card">
          <h3 class="section-title">位置信息</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="观测地点">{{ locationName(observation.locationId) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生态系统">{{ ecosystemName(observation.ecosystemId) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="纬度">{{ observation.latitude ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="经度">{{ observation.longitude ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="深度">{{ observation.depth ? observation.depth + ' m' : '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 环境数据 -->
        <div class="info-card">
          <h3 class="section-title">环境数据</h3>
          <div class="env-metrics">
            <div class="metric-item">
              <div class="metric-icon" style="background: linear-gradient(135deg, #0ea5e9, #38bdf8)">
                <el-icon><Sunny /></el-icon>
              </div>
              <div class="metric-data">
                <span class="metric-value">{{ observation.waterTemperature ?? '-' }}</span>
                <span class="metric-unit">℃</span>
                <span class="metric-label">水温</span>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-icon" style="background: linear-gradient(135deg, #10b981, #34d399)">
                <el-icon><Odometer /></el-icon>
              </div>
              <div class="metric-data">
                <span class="metric-value">{{ observation.salinity ?? '-' }}</span>
                <span class="metric-unit">PSU</span>
                <span class="metric-label">盐度</span>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-icon" style="background: linear-gradient(135deg, #f59e0b, #fbbf24)">
                <el-icon><DataLine /></el-icon>
              </div>
              <div class="metric-data">
                <span class="metric-value">{{ observation.ph ?? '-' }}</span>
                <span class="metric-unit"></span>
                <span class="metric-label">pH值</span>
              </div>
            </div>
          </div>
          <el-descriptions :column="2" border size="small" style="margin-top: 16px">
            <el-descriptions-item label="天气状况">{{ weatherLabel(observation.weatherCondition) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="海况">{{ seaLabel(observation.seaCondition) || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 观测员信息 -->
        <div class="info-card">
          <h3 class="section-title">观测员信息</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="观测员">{{ observation.observerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属机构">{{ observation.organization || '-' }}</el-descriptions-item>
            <el-descriptions-item label="使用设备" :span="2">{{ observation.equipmentUsed || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <!-- 观测备注 -->
      <div class="info-card notes-card" v-if="observation.notes">
        <h3 class="section-title">观测备注</h3>
        <p class="notes-text">{{ observation.notes }}</p>
      </div>
    </div>

    <div v-else v-loading="loading" class="detail-loading"></div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" title="编辑观测记录" width="750px" destroy-on-close>
      <el-form :model="formData" label-width="100px" :rules="formRules" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="观测编号" prop="observationCode">
              <el-input v-model="formData.observationCode" />
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
              <el-input v-model="formData.observerName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属机构">
              <el-input v-model="formData.organization" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用设备">
              <el-input v-model="formData.equipmentUsed" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="观测备注">
          <el-input v-model="formData.notes" type="textarea" :rows="3" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getObservationDetail,
  updateObservation,
  deleteObservation,
  getObservationLocations,
  getObservationEcosystems
} from '@/api/observation'
import type { Observation, ObservationLocation, Ecosystem } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const observation = ref<Observation | null>(null)
const locationList = ref<ObservationLocation[]>([])
const ecosystemList = ref<Ecosystem[]>([])

const typeOptions = [
  { value: 'DIVE', label: '潜水观测' },
  { value: 'SURVEY', label: '科学调查' },
  { value: 'SIGHTING', label: '目视观测' },
  { value: 'TRACKING', label: '追踪观测' }
]

// 编辑对话框
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<Observation>({
  observationCode: '', observationType: '', observationDate: '',
  observationTime: undefined, durationMinutes: undefined,
  locationId: undefined, ecosystemId: undefined,
  latitude: undefined, longitude: undefined, depth: undefined,
  waterTemperature: undefined, salinity: undefined, ph: undefined,
  weatherCondition: '', seaCondition: '',
  observerName: '', organization: '', equipmentUsed: '', notes: ''
})

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
    DIVE: '潜水观测', SURVEY: '科学调查', SIGHTING: '目视观测', TRACKING: '追踪观测'
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

function handleLocationChange(locId: number | undefined) {
  if (locId) {
    const loc = locationList.value.find(l => l.id === locId)
    if (loc) {
      formData.latitude = loc.latitude
      formData.longitude = loc.longitude
    }
  }
}

// ==================== 数据加载 ====================

async function loadDetail() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res: any = await getObservationDetail(id)
    observation.value = res.data
  } catch (e: any) {
    ElMessage.error('加载观测记录失败')
    router.push('/observation/list')
  } finally {
    loading.value = false
  }
}

async function loadLocations() {
  const res: any = await getObservationLocations()
  locationList.value = res.data || []
}

async function loadEcosystems() {
  const res: any = await getObservationEcosystems()
  ecosystemList.value = res.data || []
}

// ==================== 操作 ====================

function openEditDialog() {
  if (observation.value) {
    Object.assign(formData, { ...observation.value })
    dialogVisible.value = true
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await updateObservation(formData.id!, formData)
    ElMessage.success('观测记录更新成功')
    dialogVisible.value = false
    loadDetail()
  } catch (e: any) {
    // 错误已由 http 拦截器处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!observation.value?.id) return
  try {
    await deleteObservation(observation.value.id)
    ElMessage.success('观测记录已删除')
    router.push('/observation/list')
  } catch (e: any) {
    // 错误已由拦截器处理
  }
}

onMounted(() => {
  Promise.all([loadLocations(), loadEcosystems()]).then(() => {
    loadDetail()
  })
})
</script>

<style scoped lang="scss">
.observation-detail-page {
  animation: fadeIn 0.3s ease;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-loading {
  min-height: 300px;
}

/* ===== Hero 卡片 ===== */
.hero-card {
  position: relative;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.hero-bg-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-main), var(--primary-light), var(--primary-glow));
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.hero-main {
  flex: 1;
}

.hero-code {
  font-size: 28px;
  font-weight: 800;
  color: var(--neutral-800);
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.hero-type {
  margin: 0 0 12px;
}

.type-badge {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.hero-badges {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  > span {
    font-size: 14px;
    color: var(--neutral-600);
    background: var(--neutral-100);
    padding: 4px 14px;
    border-radius: 20px;
    font-weight: 500;
  }
}

.hero-observer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: var(--neutral-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);

  .observer-avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-main), var(--primary-light));
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
  }

  .observer-info {
    display: flex;
    flex-direction: column;
  }

  .observer-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--neutral-800);
  }

  .observer-org {
    font-size: 12px;
    color: var(--neutral-500);
    margin-top: 2px;
  }
}

/* ===== 信息网格 ===== */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

.info-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-smooth);
  &:hover { box-shadow: var(--shadow-md); }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--neutral-700);
  margin: 0 0 16px;
  padding-left: 10px;
  border-left: 3px solid var(--primary-main);
}

/* ===== 环境数据指标 ===== */
.env-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--neutral-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--neutral-100);

  .metric-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    flex-shrink: 0;
  }

  .metric-data {
    display: flex;
    flex-direction: column;
  }

  .metric-value {
    font-size: 18px;
    font-weight: 700;
    color: var(--neutral-800);
    line-height: 1.2;
  }

  .metric-unit {
    font-size: 11px;
    color: var(--neutral-500);
    display: inline;
  }

  .metric-label {
    font-size: 11px;
    color: var(--neutral-500);
    margin-top: 1px;
  }
}

/* ===== 备注区域 ===== */
.notes-card {
  margin-bottom: 20px;
}

.notes-text {
  font-size: 14px;
  color: var(--neutral-700);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}
</style>
