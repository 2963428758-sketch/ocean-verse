<template>
  <div class="data-export">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>数据导出</h2>
      <p>导出海洋生物分布与观测数据，支持 Excel / CSV / PDF / PNG 多种格式</p>
    </div>

    <!-- Step 1: 选择数据类型 -->
    <el-card shadow="hover" class="step-card">
      <div class="step-title">
        <span class="step-num">1</span>
        <span>选择数据类型</span>
      </div>
      <div class="type-cards">
        <div
          v-for="item in dataTypes"
          :key="item.key"
          :class="['type-card', { active: selectedType === item.key }]"
          @click="selectType(item.key)"
        >
          <el-icon :size="28" class="type-icon"><component :is="item.icon" /></el-icon>
          <div class="type-name">{{ item.label }}</div>
          <div class="type-desc">{{ item.desc }}</div>
        </div>
      </div>
    </el-card>

    <!-- Step 2: 筛选条件 -->
    <el-card v-if="selectedType" shadow="hover" class="step-card">
      <div class="step-title">
        <span class="step-num">2</span>
        <span>筛选条件</span>
      </div>

      <!-- 物种分布筛选 -->
      <div v-if="selectedType === 'species'" class="filter-row">
        <div class="filter-item">
          <label>IUCN 保护等级</label>
          <el-select v-model="filters.iucnStatus" multiple placeholder="全部等级" clearable style="width: 100%">
            <el-option v-for="s in iucnOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>科</label>
          <el-select v-model="filters.family" multiple filterable placeholder="全部科" clearable style="width: 100%">
            <el-option v-for="f in familyOptions" :key="f.value" :label="f.label" :value="f.value" />
          </el-select>
        </div>
      </div>

      <!-- 观测地点筛选 -->
      <div v-if="selectedType === 'observation'" class="filter-row">
        <div class="filter-item">
          <label>观测类型</label>
          <el-select v-model="filters.observationType" multiple placeholder="全部类型" clearable style="width: 100%">
            <el-option v-for="t in obsTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>日期范围</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </div>
      </div>

      <!-- 统计汇总筛选 -->
      <div v-if="selectedType === 'statistics'" class="filter-row">
        <div class="filter-item">
          <label>趋势粒度</label>
          <el-radio-group v-model="filters.period">
            <el-radio-button value="monthly">按月</el-radio-button>
            <el-radio-button value="weekly">按周</el-radio-button>
            <el-radio-button value="daily">按日</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 管理员选项 -->
      <div v-if="isAdmin && selectedType !== 'statistics'" class="admin-option">
        <el-checkbox v-model="filters.all">导出全量数据（忽略筛选，仅管理员可用）</el-checkbox>
      </div>

      <div class="filter-actions">
        <span class="auto-hint">筛选条件变化后预览自动刷新</span>
      </div>
    </el-card>

    <!-- Step 3: 选择导出格式 -->
    <el-card v-if="selectedType" shadow="hover" class="step-card">
      <div class="step-title">
        <span class="step-num">3</span>
        <span>选择导出格式</span>
      </div>
      <el-checkbox-group v-model="selectedFormats" class="format-group">
        <el-checkbox
          v-for="fmt in availableFormats"
          :key="fmt.value"
          :value="fmt.value"
          border
          class="format-checkbox"
        >
          <el-icon><component :is="fmt.icon" /></el-icon>
          <span class="fmt-label">{{ fmt.label }}</span>
        </el-checkbox>
      </el-checkbox-group>
    </el-card>

    <!-- Step 4: 数据预览 -->
    <el-card v-if="selectedType && previewData.length > 0" shadow="hover" class="step-card">
      <div class="step-title">
        <span class="step-num">4</span>
        <span>数据预览 <small style="color: var(--neutral-400); font-weight: 400">（前 {{ previewData.length }} 条）</small></span>
        <el-button class="refresh-btn" size="small" :loading="previewLoading" @click="loadPreview">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 统计汇总时显示图表 -->
      <div v-if="selectedType === 'statistics'" class="charts-preview">
        <div ref="iucnChartRef" class="chart-item"></div>
        <div ref="familyChartRef" class="chart-item"></div>
        <div ref="trendChartRef" class="chart-item chart-wide"></div>
      </div>

      <!-- 表格预览 -->
      <el-table v-else :data="previewData" stripe border style="width: 100%" max-height="400">
        <el-table-column
          v-for="col in previewColumns"
          :key="col.key"
          :prop="col.key"
          :label="col.label"
          :min-width="col.width || 120"
          show-overflow-tooltip
        />
      </el-table>
    </el-card>

    <!-- 导出按钮 -->
    <div v-if="selectedType" class="export-actions">
      <el-button
        type="primary"
        size="large"
        :loading="exporting"
        :disabled="selectedFormats.length === 0"
        @click="handleExport"
      >
        <el-icon><Download /></el-icon>
        开始导出（{{ selectedFormats.length }} 个格式）
      </el-button>
      <span v-if="selectedFormats.length === 0" class="hint">请至少选择一种导出格式</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Document, Picture, Grid, TrendCharts } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getSpeciesDistribution,
  getSpeciesByIucn,
  getSpeciesByFamily,
  getObservationTrend
} from '@/api/visual'
import { getObservationMapData } from '@/api/observation'
import { exportSpecies, exportObservation, exportStatistics, getObservationPreview } from '@/api/export'
import { downloadFromResponse, exportChartAsPNG, exportChartsAsPDF } from '@/utils/export'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'SUPER_ADMIN' || userStore.role === 'ADMIN')

// ==================== 数据类型 ====================
const dataTypes = [
  { key: 'species', label: '物种分布数据', desc: '物种名称、IUCN 等级、地理分布、栖息地', icon: 'Grid' },
  { key: 'observation', label: '观测地点数据', desc: '观测编号、类型、位置、环境参数', icon: 'TrendCharts' },
  { key: 'statistics', label: '统计汇总数据', desc: 'IUCN 分布、科分布、观测趋势', icon: 'Document' }
]
const selectedType = ref<string>('')

// ==================== 筛选条件 ====================
const filters = ref({
  iucnStatus: [] as string[],
  family: [] as string[],
  observationType: [] as string[],
  period: 'monthly',
  all: false
})
const dateRange = ref<[string, string] | null>(null)

const iucnOptions = ['CR', 'EN', 'VU', 'NT', 'LC', 'DD']
const familyOptions = ref<{ label: string; value: string }[]>([])
const obsTypeOptions = [
  { label: '潜水观测', value: 'DIVE' },
  { label: '调查观测', value: 'SURVEY' },
  { label: '目击观测', value: 'SIGHTING' },
  { label: '追踪观测', value: 'TRACKING' }
]

// ==================== 导出格式 ====================
const allFormats = [
  { value: 'excel', label: 'Excel', icon: 'Document' },
  { value: 'csv', label: 'CSV', icon: 'Document' },
  { value: 'pdf', label: 'PDF 报表', icon: 'Document' },
  { value: 'png', label: '图表 PNG', icon: 'Picture' }
]
const availableFormats = computed(() => {
  // 统计汇总支持所有格式；其他类型不支持 PNG（无图表）
  if (selectedType.value === 'statistics') return allFormats
  return allFormats.filter(f => f.value !== 'png')
})
const selectedFormats = ref<string[]>([])

// 当切换数据类型时重置格式选择
watch(selectedType, () => {
  selectedFormats.value = []
})

// ==================== 数据预览 ====================
const previewData = ref<any[]>([])
const previewLoading = ref(false)
const previewColumns = ref<{ key: string; label: string; width?: number }[]>([])

const speciesColumns = [
  { key: 'chinese_name', label: '中文名', width: 120 },
  { key: 'scientific_name', label: '学名', width: 180 },
  { key: 'family', label: '科', width: 100 },
  { key: 'iucn_status', label: 'IUCN', width: 80 },
  { key: 'country', label: '国家', width: 100 },
  { key: 'province', label: '省份', width: 100 },
  { key: 'latitude', label: '纬度', width: 100 },
  { key: 'longitude', label: '经度', width: 100 },
  { key: 'habitat_type', label: '栖息地', width: 120 }
]
const observationColumns = [
  { key: 'observationCode', label: '编号', width: 140 },
  { key: 'observationType', label: '类型', width: 100 },
  { key: 'observationDate', label: '日期', width: 110 },
  { key: 'latitude', label: '纬度', width: 100 },
  { key: 'longitude', label: '经度', width: 100 },
  { key: 'waterTemperature', label: '水温', width: 80 },
  { key: 'salinity', label: '盐度', width: 80 },
  { key: 'observerName', label: '观测员', width: 100 }
]

// ==================== 图表 ====================
const iucnChartRef = ref<HTMLElement>()
const familyChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

const iucnColors: Record<string, string> = {
  CR: '#c43535', EN: '#d4603a', VU: '#cc8a30',
  NT: '#6a9c42', LC: '#2d8a5e', DD: '#948f86'
}

// ==================== 选择数据类型 ====================
function selectType(key: string) {
  selectedType.value = key
  previewData.value = []
  filters.value = {
    iucnStatus: [], family: [], observationType: [],
    period: 'monthly', all: false
  }
  dateRange.value = null
  nextTick(() => loadPreview())
}

// ==================== 加载预览数据 ====================
let previewTimer: ReturnType<typeof setTimeout> | null = null

/** 带防抖的预览刷新，筛选变化后 600ms 触发 */
function debouncedLoadPreview() {
  if (previewTimer) clearTimeout(previewTimer)
  previewTimer = setTimeout(() => {
    loadPreview()
  }, 600)
}

async function loadPreview() {
  if (!selectedType.value) return
  previewLoading.value = true
  try {
    if (selectedType.value === 'species') {
      // 预览时同样带筛选参数
      const params: Record<string, any> = {}
      if (filters.value.iucnStatus.length > 0) params.iucnStatus = filters.value.iucnStatus.join(',')
      if (filters.value.family.length > 0) params.family = filters.value.family.join(',')
      const res: any = await getSpeciesDistribution(params)
      const list = res.data || []
      previewData.value = list.slice(0, 20)
      previewColumns.value = speciesColumns
    } else if (selectedType.value === 'observation') {
      // 观测预览：调后端预览接口，做 snake_case → camelCase 转换
      const params: Record<string, any> = {}
      if (filters.value.observationType.length > 0) params.observationType = filters.value.observationType.join(',')
      if (dateRange.value && dateRange.value.length === 2) {
        params.startDate = dateRange.value[0]
        params.endDate = dateRange.value[1]
      }
      const res: any = await getObservationPreview(params)
      const list = (res.data || []).map((row: any) => ({
        observationCode: row.observation_code || '',
        observationType: row.observation_type || '',
        observationDate: row.observation_date || '',
        latitude: row.latitude != null ? String(row.latitude) : '',
        longitude: row.longitude != null ? String(row.longitude) : '',
        waterTemperature: row.water_temperature != null ? String(row.water_temperature) : '',
        salinity: row.salinity != null ? String(row.salinity) : '',
        observerName: row.observer_name || ''
      }))
      previewData.value = list
      previewColumns.value = observationColumns
    } else if (selectedType.value === 'statistics') {
      const [iucnRes, familyRes, trendRes]: any[] = await Promise.all([
        getSpeciesByIucn(),
        getSpeciesByFamily(),
        getObservationTrend({ period: filters.value.period })
      ])
      previewData.value = (iucnRes.data || []).concat(familyRes.data || []).concat(trendRes.data || [])
      await nextTick()
      renderCharts(iucnRes.data || [], familyRes.data || [], trendRes.data || [])
    }
  } catch (e) {
    console.error('加载预览失败', e)
  } finally {
    previewLoading.value = false
  }
}

// ==================== 筛选变化自动刷新预览 ====================
// 深度监听 filters 所有字段
watch(filters, () => {
  if (selectedType.value && selectedType.value !== 'statistics') {
    debouncedLoadPreview()
  }
}, { deep: true })

// 统计汇总只需监听 period
watch(() => filters.value.period, () => {
  if (selectedType.value === 'statistics') {
    debouncedLoadPreview()
  }
})

// 日期范围单独监听（el-date-picker 的 v-model 是独立 ref）
watch(dateRange, () => {
  if (selectedType.value === 'observation') {
    debouncedLoadPreview()
  }
})

// ==================== 渲染图表 ====================
function renderCharts(iucnData: any[], familyData: any[], trendData: any[]) {
  // 清理旧图表
  charts.forEach(c => c.dispose())
  charts.length = 0

  // IUCN 柱状图
  if (iucnChartRef.value) {
    const chart = echarts.init(iucnChartRef.value)
    chart.setOption({
      title: { text: 'IUCN 保护等级分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: iucnData.map(d => d.iucn_status) },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: iucnData.map(d => ({
          value: d.count,
          itemStyle: { color: iucnColors[d.iucn_status] || '#5bb5d5' }
        })),
        barWidth: '50%'
      }]
    })
    charts.push(chart)
  }

  // 科分布饼图
  if (familyChartRef.value) {
    const chart = echarts.init(familyChartRef.value)
    chart.setOption({
      title: { text: '物种按科分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: ['35%', '60%'],
        data: familyData.map(d => ({ name: d.family, value: d.count })),
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
      }]
    })
    charts.push(chart)
  }

  // 观测趋势折线图
  if (trendChartRef.value) {
    const chart = echarts.init(trendChartRef.value)
    chart.setOption({
      title: { text: '观测记录趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendData.map(d => d.period), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '观测次数' },
      series: [{
        type: 'line',
        smooth: true,
        data: trendData.map(d => d.count),
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 2, color: '#1a6b8a' },
        itemStyle: { color: '#1a6b8a' }
      }],
      grid: { left: '8%', right: '5%', bottom: '12%', top: '15%' }
    })
    charts.push(chart)
  }
}

// ==================== 执行导出 ====================
const exporting = ref(false)

async function handleExport() {
  if (selectedFormats.value.length === 0) return
  exporting.value = true

  try {
    for (const fmt of selectedFormats.value) {
      if (fmt === 'excel' || fmt === 'csv') {
        await exportFromBackend(fmt)
      } else if (fmt === 'pdf') {
        await exportPDF()
      } else if (fmt === 'png') {
        exportPNG()
      }
    }
    ElMessage.success(`成功导出 ${selectedFormats.value.length} 个文件`)
  } catch (e: any) {
    console.error('导出失败', e)
  } finally {
    exporting.value = false
  }
}

// 后端导出 Excel/CSV
async function exportFromBackend(format: 'excel' | 'csv') {
  if (selectedType.value === 'species') {
    const params: any = {
      format,
      all: filters.value.all,
      iucnStatus: filters.value.iucnStatus.join(','),
      family: filters.value.family.join(',')
    }
    const res = await exportSpecies(params)
    downloadFromResponse(res, `物种分布数据.${format === 'excel' ? 'xlsx' : 'csv'}`)
  } else if (selectedType.value === 'observation') {
    const params: any = {
      format,
      all: filters.value.all,
      observationType: filters.value.observationType.join(','),
      startDate: dateRange.value?.[0] || '',
      endDate: dateRange.value?.[1] || ''
    }
    const res = await exportObservation(params)
    downloadFromResponse(res, `观测记录数据.${format === 'excel' ? 'xlsx' : 'csv'}`)
  } else if (selectedType.value === 'statistics') {
    const res = await exportStatistics({ format, period: filters.value.period as any })
    downloadFromResponse(res, `统计汇总数据.${format === 'excel' ? 'xlsx' : 'csv'}`)
  }
  ElMessage.success(`${format.toUpperCase()} 文件已下载`)
}

// 前端导出 PDF
async function exportPDF() {
  if (charts.length === 0) {
    ElMessage.warning('请先刷新预览加载图表')
    return
  }
  await exportChartsAsPDF(charts, 'OceanVerse 数据导出报告', 'OceanVerse-数据报告')
  ElMessage.success('PDF 报表已下载')
}

// 前端导出 PNG
function exportPNG() {
  if (charts.length === 0) {
    ElMessage.warning('请先刷新预览加载图表')
    return
  }
  const names = ['IUCN保护等级分布', '物种按科分布', '观测记录趋势']
  charts.forEach((chart, i) => {
    exportChartAsPNG(chart, `OceanVerse-${names[i] || '图表' + (i + 1)}`)
  })
  ElMessage.success(`${charts.length} 张图表 PNG 已下载`)
}

// ==================== 生命周期 ====================
onMounted(async () => {
  // 动态加载科列表
  try {
    const res: any = await getSpeciesByFamily()
    const list = res.data || []
    familyOptions.value = list.map((item: any) => ({
      label: `${item.family}（${item.count} 种）`,
      value: item.family
    }))
  } catch (e) {
    console.error('加载科列表失败', e)
  }
  // 默认选中物种分布
  selectType('species')
})

onBeforeUnmount(() => {
  charts.forEach(c => c.dispose())
})

// 监听窗口变化调整图表
function handleResize() {
  charts.forEach(c => c.resize())
}
window.addEventListener('resize', handleResize)
onBeforeUnmount(() => window.removeEventListener('resize', handleResize))
</script>

<style scoped lang="scss">
.data-export {
  animation: fadeIn 0.4s ease;
}

/* 步骤卡片 */
.step-card {
  margin-bottom: 18px;

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.step-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
  margin-bottom: 20px;

  .step-num {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--gradient-ocean);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 700;
  }
}

/* 数据类型卡片 */
.type-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.type-card {
  border: 2px solid var(--neutral-100);
  border-radius: var(--radius-lg);
  padding: 24px 20px;
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  text-align: center;

  &:hover {
    border-color: var(--primary-light);
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }

  &.active {
    border-color: var(--primary-main);
    background: var(--primary-soft);
  }

  .type-icon {
    color: var(--primary-main);
    margin-bottom: 12px;
  }

  .type-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--neutral-800);
    margin-bottom: 6px;
  }

  .type-desc {
    font-size: 12px;
    color: var(--neutral-400);
    line-height: 1.5;
  }
}

/* 筛选条件 */
.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 16px;
}

.filter-item {
  label {
    display: block;
    font-size: 13px;
    color: var(--neutral-600);
    margin-bottom: 8px;
    font-weight: 500;
  }
}

.admin-option {
  padding: 12px 16px;
  background: var(--accent-violet-soft);
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  font-size: 13px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.auto-hint {
  font-size: 12px;
  color: var(--neutral-400);
  margin-right: auto;
}

/* 预览区刷新按钮 */
.refresh-btn {
  margin-left: auto;
}

/* 格式选择 */
.format-group {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.format-checkbox {
  height: auto !important;
  padding: 14px 20px !important;
  border-radius: var(--radius-md) !important;

  .fmt-label {
    margin-left: 6px;
    font-weight: 500;
  }

}

/* 图表预览 */
.charts-preview {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;

  .chart-wide {
    grid-column: span 2;
  }

  .chart-item {
    height: 300px;
    border: 1px solid var(--neutral-100);
    border-radius: var(--radius-md);
  }
}

/* 导出按钮 */
.export-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
  padding: 24px 0;

  .hint {
    font-size: 13px;
    color: var(--neutral-400);
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 900px) {
  .type-cards { grid-template-columns: 1fr; }
  .filter-row { grid-template-columns: 1fr; }
  .charts-preview { grid-template-columns: 1fr; }
  .charts-preview .chart-wide { grid-column: span 1; }
}
</style>
