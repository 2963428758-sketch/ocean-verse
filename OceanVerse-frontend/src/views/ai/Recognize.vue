<template>
  <div>
    <div class="page-header"><h2>🔍 AI 图像识别</h2><p>上传海洋生物照片，AI 自动识别物种</p></div>
    <el-card>
      <el-upload drag :auto-upload="false" :on-change="handleFileChange" accept="image/*" :limit="1">
        <el-icon :size="48"><UploadFilled /></el-icon>
        <div>拖拽或点击上传图片</div>
        <template #tip><div class="el-upload__tip">支持 jpg/png 格式，最大 20MB</div></template>
      </el-upload>
      <el-button type="primary" style="margin-top:16px" :loading="loading" :disabled="!file" @click="doRecognize">
        开始识别
      </el-button>
      <div v-if="result" style="margin-top:24px">
        <el-result icon="success" :title="result.predictedSpeciesName || '识别完成'">
          <template #sub-title>
            <p>置信度: {{ (result.confidenceScore * 100).toFixed(1) }}%</p>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { recognizeImage } from '@/api/ai'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'

const file = ref<File | null>(null)
const loading = ref(false)
const result = ref<any>(null)

function handleFileChange(uploadFile: UploadFile) {
  file.value = uploadFile.raw || null
}

async function doRecognize() {
  if (!file.value) return
  loading.value = true
  try {
    const res: any = await recognizeImage(file.value)
    result.value = res.data
    ElMessage.success('识别完成')
  } finally {
    loading.value = false
  }
}
</script>
