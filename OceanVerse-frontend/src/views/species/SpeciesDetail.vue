<template>
  <div>
    <div class="page-header"><h2>物种详情</h2></div>
    <el-card v-if="species">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="中文名">{{ species.chineseName }}</el-descriptions-item>
        <el-descriptions-item label="学名"><em>{{ species.scientificName }}</em></el-descriptions-item>
        <el-descriptions-item label="科">{{ species.family }}</el-descriptions-item>
        <el-descriptions-item label="属">{{ species.genus }}</el-descriptions-item>
        <el-descriptions-item label="IUCN状态"><el-tag>{{ species.iucnStatus || '-' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="保护等级">{{ species.protectionLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ species.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="形态特征" :span="2">{{ species.morphology || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生态习性" :span="2">{{ species.ecology || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSpeciesDetail } from '@/api/species'

const route = useRoute()
const species = ref<any>(null)

onMounted(async () => {
  const res: any = await getSpeciesDetail(Number(route.params.id))
  species.value = res.data
})
</script>
