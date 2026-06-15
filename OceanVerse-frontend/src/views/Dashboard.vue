<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>🌊 OceanVerse 仪表盘</h2>
      <p>智慧海洋探索平台概览</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" style="margin-top:24px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>🔬 快速操作</span></template>
          <el-space wrap>
            <el-button type="primary" @click="$router.push('/ai/recognize')">📷 AI 识别</el-button>
            <el-button type="success" @click="$router.push('/ai/chat')">💬 AI 问答</el-button>
            <el-button type="warning" @click="$router.push('/community/feed')">👥 社区动态</el-button>
            <el-button type="info" @click="$router.push('/visualization/map')">🗺️ 分布地图</el-button>
          </el-space>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>📊 系统信息</span></template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="平台版本">v1.0.0</el-descriptions-item>
            <el-descriptions-item label="技术栈">Vue3 + Spring Boot + MySQL + Redis</el-descriptions-item>
            <el-descriptions-item label="AI 引擎">DeepSeek / 通义千问</el-descriptions-item>
            <el-descriptions-item label="当前用户">{{ userStore.username }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()

const statCards = [
  { title: '物种总数', value: '128', icon: 'Leaf', color: 'linear-gradient(135deg,#67c23a,#409eff)' },
  { title: '观测记录', value: '56', icon: 'Binoculars', color: 'linear-gradient(135deg,#409eff,#0f4c75)' },
  { title: 'AI 识别次数', value: '23', icon: 'MagicStick', color: 'linear-gradient(135deg,#e6a23c,#f56c6c)' },
  { title: '社区动态', value: '89', icon: 'ChatDotRound', color: 'linear-gradient(135deg,#f56c6c,#909399)' }
]
</script>

<style scoped lang="scss">
.stat-cards {
  .stat-card {
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
    }
  }
  .stat-icon {
    width: 56px; height: 56px;
    border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    color: white;
    flex-shrink: 0;
  }
  .stat-value { font-size: 28px; font-weight: 700; color: var(--neutral-800); }
  .stat-title { font-size: 13px; color: var(--neutral-500); margin-top: 2px; }
}
</style>
