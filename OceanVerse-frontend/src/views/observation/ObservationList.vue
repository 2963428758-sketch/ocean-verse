<template>
  <div>
    <div class="page-header"><h2>观测记录</h2><p>海洋生物观测数据管理</p></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="observationCode" label="观测编号" width="150" />
        <el-table-column prop="observationType" label="类型" width="100" />
        <el-table-column prop="observationDate" label="日期" width="120" />
        <el-table-column prop="observerName" label="观测员" width="120" />
        <el-table-column prop="waterTemperature" label="水温(℃)" width="100" />
        <el-table-column prop="notes" label="备注" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getObservationList } from '@/api/observation'
const list = ref<any[]>([])

onMounted(async () => {
  const res: any = await getObservationList({ page: 1, size: 20 })
  list.value = res.data?.records || []
})
</script>
