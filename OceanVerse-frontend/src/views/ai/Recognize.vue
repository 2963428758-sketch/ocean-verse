<template>
  <div class="recognize-page">
    <div class="page-header">
      <div>
        <h2>🔍 AI 图像识别</h2>
        <p>上传海洋生物照片，AI 自动识别物种并给出详细分析</p>
      </div>
    </div>

    <div class="recognize-layout">
      <!-- 左栏：图片区域 -->
      <div class="image-panel">
        <div v-if="!previewUrl && !result" class="upload-zone" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleInputChange" />
          <div class="upload-content">
            <svg class="upload-icon" viewBox="0 0 48 48" fill="none">
              <rect x="6" y="10" width="36" height="28" rx="4" stroke="currentColor" stroke-width="2"/>
              <circle cx="18" cy="22" r="4" stroke="currentColor" stroke-width="2"/>
              <path d="M6 32l10-8 6 5 8-10 12 13" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
            </svg>
            <span class="upload-text">拖拽图片到此处，或点击选择</span>
            <span class="upload-hint">支持 JPG / PNG 格式，最大 20MB</span>
          </div>
        </div>

        <div v-else class="image-preview" :class="{ scanning: loading }">
          <img :src="previewUrl || result?.imageUrl" alt="uploaded" class="preview-img" />
          <div v-if="loading" class="scan-overlay">
            <div class="scan-line"></div>
            <span class="scan-text">AI 正在分析...</span>
          </div>
          <button v-if="!loading" class="change-btn" @click="triggerUpload">更换图片</button>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleInputChange" />
        </div>

        <!-- 观测建议横幅（任务 3.5：高置信度识别建议创建观测记录） -->
        <div v-if="result && hasObservationSuggestion && observationData" class="observation-banner">
          <div class="observation-info">
            <span class="observation-icon">📍</span>
            <span>高置信度识别 — 可记录为生态观测</span>
          </div>
          <button class="observation-btn" @click="showObservationPrompt">记录观测</button>
        </div>

        <el-button v-if="file && !loading && !result" type="primary" class="recognize-btn" @click="doRecognize">
          开始识别
        </el-button>
      </div>

      <!-- 右栏：结果区域 -->
      <div class="result-panel">
        <!-- 空状态 -->
        <div v-if="!result && !loading" class="empty-state">
          <div class="empty-illustration">
            <svg viewBox="0 0 120 120" fill="none">
              <circle cx="60" cy="60" r="50" stroke="var(--neutral-200)" stroke-width="2" stroke-dasharray="8 4"/>
              <path d="M40 55c0-11 9-20 20-20s20 9 20 20" stroke="var(--neutral-300)" stroke-width="2" stroke-linecap="round"/>
              <circle cx="48" cy="65" r="3" fill="var(--neutral-300)"/>
              <circle cx="72" cy="65" r="3" fill="var(--neutral-300)"/>
              <path d="M50 78c4 4 16 4 20 0" stroke="var(--neutral-300)" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <p class="empty-title">等待识别</p>
          <p class="empty-desc">上传一张海洋生物照片，AI 将自动识别物种并提供详细分析</p>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="pulse-ring">
            <div class="pulse-dot"></div>
          </div>
          <p>正在调用视觉模型分析图像...</p>
        </div>

        <!-- 识别结果 -->
        <div v-if="result" class="result-content">
          <!-- 物种标题 -->
          <div class="species-header">
            <div class="species-names">
              <h3 class="species-name">{{ result.predictedSpeciesName || parsedResult.speciesName || '未识别' }}</h3>
              <span v-if="parsedResult.scientificName" class="scientific-name">{{ parsedResult.scientificName }}</span>
            </div>
            <div class="verify-badge" :class="result.verificationStatus?.toLowerCase()">
              {{ result.verificationStatus === 'VERIFIED' ? '✓ 已验证' : '⏳ 待验证' }}
            </div>
          </div>

          <!-- 置信度 + 保护等级 -->
          <div class="metrics-row">
            <div class="metric-card confidence">
              <svg class="gauge" viewBox="0 0 80 80">
                <circle cx="40" cy="40" r="34" fill="none" stroke="var(--neutral-100)" stroke-width="6"/>
                <circle cx="40" cy="40" r="34" fill="none"
                  :stroke="confidenceColor"
                  stroke-width="6"
                  stroke-linecap="round"
                  :stroke-dasharray="gaugeArc"
                  :stroke-dashoffset="gaugeArc"
                  transform="rotate(-90 40 40)"
                  class="gauge-fill"/>
              </svg>
              <div class="gauge-label">
                <span class="gauge-value">{{ confidencePercent }}%</span>
                <span class="gauge-desc">置信度</span>
              </div>
            </div>
            <div v-if="parsedResult.conservationStatus" class="metric-card status">
              <div class="status-icon">🛡️</div>
              <div class="status-info">
                <span class="status-label">保护等级</span>
                <span class="status-value">{{ parsedResult.conservationStatus }}</span>
              </div>
            </div>
          </div>

          <!-- 描述 -->
          <div v-if="parsedResult.description" class="detail-section">
            <h4>📋 物种描述</h4>
            <p class="detail-text">{{ parsedResult.description }}</p>
          </div>

          <!-- 栖息地 -->
          <div v-if="parsedResult.habitat" class="detail-section">
            <h4>🌊 栖息环境</h4>
            <p class="detail-text">{{ parsedResult.habitat }}</p>
          </div>

          <!-- 物种知识卡片（任务 3.4：识别结果关联 species 表） -->
          <div v-if="speciesCard" class="species-card">
            <h4 class="species-card-title">📖 物种知识</h4>
            <div class="species-card-grid">
              <div v-if="speciesCard.iucnStatus" class="species-card-item">
                <span class="species-card-label">IUCN 状态</span>
                <span class="species-card-value">{{ speciesCard.iucnStatus }}</span>
              </div>
              <div v-if="speciesCard.protectionLevel" class="species-card-item">
                <span class="species-card-label">保护等级</span>
                <span class="species-card-value">{{ speciesCard.protectionLevel }}</span>
              </div>
            </div>
            <p v-if="speciesCard.morphology" class="species-card-text">
              <span class="species-card-label">形态特征：</span>{{ speciesCard.morphology }}
            </p>
            <p v-if="speciesCard.ecology" class="species-card-text">
              <span class="species-card-label">生态习性：</span>{{ speciesCard.ecology }}
            </p>
            <p v-if="speciesCard.description" class="species-card-text">
              <span class="species-card-label">简介：</span>{{ speciesCard.description }}
            </p>
          </div>

          <!-- 技术元数据 -->
          <div class="meta-footer">
            <span class="meta-item">
              <span class="meta-dot"></span>
              模型：{{ result.aiModelVersion || 'qwen-vl-max' }}
            </span>
            <span class="meta-item">
              <span class="meta-dot"></span>
              耗时：{{ result.processingTimeMs || 0 }}ms
            </span>
            <span v-if="result.recognitionCode" class="meta-item">
              <span class="meta-dot"></span>
              {{ result.recognitionCode }}
            </span>
          </div>
        </div>

        <!-- 识别失败 -->
        <div v-if="result && !result.predictedSpeciesName && !parsedResult.speciesName" class="error-state">
          <p>识别失败，请尝试上传更清晰的图片</p>
          <p v-if="result.errorMessage" class="error-detail">{{ result.errorMessage }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AiRecognize' })
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { recognizeImage } from '@/api/ai'
import { ElMessage } from 'element-plus'

const router = useRouter()

const fileInput = ref<HTMLInputElement>()
const file = ref<File | null>(null)
const loading = ref(false)
const result = ref<any>(null)
const previewUrl = ref<string | null>(null)
const speciesCard = ref<any>(null)
const observationData = ref<any>(null)
const hasObservationSuggestion = ref(false)

// 解析 recognitionResult（后端存储的 AI 原始 JSON 响应）
const parsedResult = computed(() => {
  if (!result.value?.recognitionResult) return {}
  try {
    const raw = result.value.recognitionResult.trim()
    // 兼容 markdown code block 包裹
    let json = raw
    if (json.startsWith('```')) {
      const firstNl = json.indexOf('\n')
      const lastFence = json.lastIndexOf('```')
      if (firstNl > 0 && lastFence > firstNl) json = json.substring(firstNl + 1, lastFence).trim()
    }
    const first = json.indexOf('{')
    const last = json.lastIndexOf('}')
    if (first >= 0 && last > first) json = json.substring(first, last + 1)
    return JSON.parse(json)
  } catch {
    return {}
  }
})

const confidencePercent = computed(() => {
  const score = result.value?.confidenceScore ?? parsedResult.value?.confidence ?? 0
  return (Number(score) * 100).toFixed(1)
})

const confidenceColor = computed(() => {
  const pct = Number(confidencePercent.value)
  if (pct >= 80) return 'var(--el-color-success)'
  if (pct >= 50) return 'var(--el-color-warning)'
  return 'var(--el-color-danger)'
})

// SVG 圆弧：半径 34，周长 ≈ 213.6，只取 75% 弧（约 160）
const gaugeArc = computed(() => {
  const circumference = 2 * Math.PI * 34
  const arc = circumference * 0.75
  const offset = arc - (arc * Number(confidencePercent.value)) / 100
  return `${arc} ${circumference}`
})

function triggerUpload() {
  fileInput.value?.click()
}

function handleInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0]
  if (f) selectFile(f)
  input.value = '' // reset so same file can be re-selected
}

function handleDrop(e: DragEvent) {
  const f = e.dataTransfer?.files?.[0]
  if (f && f.type.startsWith('image/')) selectFile(f)
}

function selectFile(f: File) {
  if (f.size > 20 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 20MB')
    return
  }
  file.value = f
  result.value = null
  speciesCard.value = null
  observationData.value = null
  hasObservationSuggestion.value = false
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(f)
}

function resetAll() {
  file.value = null
  result.value = null
  speciesCard.value = null
  observationData.value = null
  hasObservationSuggestion.value = false
  if (previewUrl.value) { URL.revokeObjectURL(previewUrl.value); previewUrl.value = null }
}

async function doRecognize() {
  if (!file.value) return
  loading.value = true
  try {
    const res: any = await recognizeImage(file.value)
    const data = res.data
    // 第三层增强响应格式：{ recognition, speciesCard?, suggestObservation?, observationData? }
    if (data && data.recognition) {
      result.value = data.recognition
      speciesCard.value = data.speciesCard || null
      hasObservationSuggestion.value = !!data.suggestObservation
      observationData.value = data.observationData || null
    } else {
      // 兼容旧格式：直接返回识别记录
      result.value = data
      speciesCard.value = null
      hasObservationSuggestion.value = false
      observationData.value = null
    }
    ElMessage.success('识别完成')
    // 高置信度识别时显示观测建议横幅，用户主动点击后才弹窗
  } catch {
    ElMessage.error('识别失败，请重试')
  } finally {
    loading.value = false
  }
}

function showObservationPrompt() {
  const od = observationData.value
  if (!od) return

  const confidence = (Number(od.confidence) * 100).toFixed(0)
  const now = new Date()

  router.push({
    path: '/observation/list',
    state: {
      aiPrefill: {
        observationType: 'SIGHTING',
        observationDate: now.toISOString().slice(0, 10),
        observationTime: now.toTimeString().slice(0, 5),
        latitude: od.latitude || undefined,
        longitude: od.longitude || undefined,
        notes: `【AI 识别生成】识别编号：${result.value?.recognitionCode || ''}，物种：${od.speciesName}，置信度：${confidence}%。`
      }
    }
  })
}
</script>

<style scoped lang="scss">
.recognize-page {
  width: 100%;
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.page-header {
  flex-shrink: 0;
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 20px; }
  p { margin: 4px 0 0; color: var(--neutral-500); font-size: 14px; }
}

/* ── 两栏布局 ── */
.recognize-layout {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) 2fr;
  gap: 20px;
  flex: 1;
  min-height: 0;
}

/* ── 左栏：图片区 ── */
.image-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
  align-items: stretch;
}

.upload-zone {
  border: 2px dashed var(--neutral-200);
  border-radius: 12px;
  padding: 48px 24px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  &:hover { border-color: var(--primary-main); background: color-mix(in srgb, var(--primary-main) 4%, transparent); }
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  width: 48px;
  height: 48px;
  color: var(--neutral-300);
}

.upload-text {
  font-size: 14px;
  color: var(--neutral-600);
}

.upload-hint {
  font-size: 12px;
  color: var(--neutral-400);
}

.image-preview {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: var(--neutral-75);

  .preview-img {
    width: 100%;
    display: block;
    border-radius: 12px;
    max-height: calc(100vh - 280px);
    object-fit: contain;
  }

  &.scanning .preview-img {
    filter: brightness(0.7);
  }
}

.scan-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 12px;
}

.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--primary-main), transparent);
  box-shadow: 0 0 12px var(--primary-main);
  animation: scan 2s ease-in-out infinite;
}

@keyframes scan {
  0% { top: 0; }
  50% { top: calc(100% - 3px); }
  100% { top: 0; }
}

.scan-text {
  color: white;
  font-size: 14px;
  font-weight: 500;
  text-shadow: 0 1px 4px rgba(0,0,0,0.5);
  z-index: 1;
}

.change-btn {
  position: absolute;
  bottom: 10px;
  right: 10px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: rgba(0,0,0,0.55);
  color: white;
  font-size: 12px;
  cursor: pointer;
  backdrop-filter: blur(4px);
  transition: background 0.2s;
  &:hover { background: rgba(0,0,0,0.7); }
}

.recognize-btn {
  width: 100%;
}

/* ── 右栏：结果区 ── */
.result-panel {
  background: white;
  border: 1px solid var(--neutral-100);
  border-radius: 12px;
  padding: 24px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  flex: 1;
}

.empty-illustration {
  svg { width: 100px; height: 100px; }
  margin-bottom: 16px;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-700);
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 13px;
  color: var(--neutral-400);
  margin: 0;
  max-width: 260px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex: 1;
  p { color: var(--neutral-500); font-size: 14px; margin: 0; }
}

.pulse-ring {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--primary-main) 12%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 1.5s ease-in-out infinite;
}

.pulse-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--primary-main);
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.15); opacity: 0.7; }
}

/* ── 识别结果 ── */
.result-content {
  animation: fadeUp 0.4s ease;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.species-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.species-names {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.species-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--neutral-900);
  margin: 0;
  line-height: 1.3;
}

.scientific-name {
  font-size: 14px;
  color: var(--neutral-400);
  font-style: italic;
}

.verify-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  white-space: nowrap;
  flex-shrink: 0;
  &.verified {
    background: color-mix(in srgb, var(--el-color-success) 10%, transparent);
    color: var(--el-color-success);
  }
  &.pending {
    background: color-mix(in srgb, var(--el-color-warning) 10%, transparent);
    color: var(--el-color-warning);
  }
}

/* 置信度 + 保护等级 */
.metrics-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.metric-card {
  border: 1px solid var(--neutral-100);
  border-radius: 10px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;

  &.confidence {
    .gauge {
      width: 60px;
      height: 60px;
      flex-shrink: 0;
      .gauge-fill {
        transition: stroke-dashoffset 0.8s ease;
      }
    }
  }

  &.status {
    .status-icon { font-size: 28px; flex-shrink: 0; }
  }
}

.gauge-label {
  display: flex;
  flex-direction: column;
}

.gauge-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
  line-height: 1.2;
}

.gauge-desc {
  font-size: 12px;
  color: var(--neutral-400);
}

.status-label {
  font-size: 12px;
  color: var(--neutral-400);
  display: block;
}

.status-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-800);
  display: block;
  margin-top: 2px;
}

/* 详情区块 */
.detail-section {
  margin-bottom: 16px;
  h4 {
    font-size: 13px;
    font-weight: 600;
    color: var(--neutral-600);
    margin: 0 0 6px;
  }
}

.detail-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--neutral-700);
  margin: 0;
}

/* 底部元数据 */
.meta-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--neutral-100);
}

.meta-item {
  font-size: 12px;
  color: var(--neutral-400);
  display: flex;
  align-items: center;
  gap: 5px;
}

.meta-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--neutral-300);
}

/* 失败状态 */
.error-state {
  text-align: center;
  padding: 60px 0;
  p { color: var(--neutral-500); margin: 0; }
  .error-detail { font-size: 12px; color: var(--neutral-400); margin-top: 8px; }
}

/* ── 物种知识卡片（任务 3.4） ── */
.species-card {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--neutral-150, #e8e8e8);
  border-radius: 10px;
  background: color-mix(in srgb, var(--primary-main) 3%, white);
}

.species-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);
  margin: 0 0 12px;
}

.species-card-grid {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
}

.species-card-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.species-card-label {
  font-size: 12px;
  color: var(--neutral-400);
  font-weight: 500;
}

.species-card-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-800);
}

.species-card-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--neutral-600);
  margin: 6px 0 0;
}

/* ── 观测建议横幅（任务 3.5） ── */
.observation-banner {
  margin-top: 16px;
  padding: 12px 16px;
  border: 1px solid color-mix(in srgb, var(--el-color-success) 30%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--el-color-success) 6%, white);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.observation-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--neutral-700);
}

.observation-icon {
  font-size: 18px;
}

.observation-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: var(--el-color-success, #67c23a);
  color: white;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  white-space: nowrap;
  &:hover { opacity: 0.85; }
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .recognize-layout {
    grid-template-columns: 1fr;
  }
  .metrics-row {
    grid-template-columns: 1fr;
  }
}
</style>
