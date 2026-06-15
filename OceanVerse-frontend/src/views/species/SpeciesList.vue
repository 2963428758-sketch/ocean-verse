<template>
  <div>
    <div class="page-header"><h2>物种列表</h2><p>浏览和管理海洋物种信息</p></div>
    <el-card>
      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="8"><el-input v-model="keyword" placeholder="搜索物种名称..." clearable @keyup.enter="search" /></el-col>
        <el-col :span="4"><el-button type="primary" @click="search">搜索</el-button></el-col>
      </el-row>
      <el-table :data="speciesList" stripe>
        <el-table-column prop="chineseName" label="中文名" width="150" />
        <el-table-column prop="scientificName" label="学名" width="200">
          <template #default="{ row }"><em>{{ row.scientificName }}</em></template>
        </el-table-column>
        <el-table-column prop="family" label="科" width="120" />
        <el-table-column prop="iucnStatus" label="IUCN 状态" width="120">
          <template #default="{ row }"><el-tag :type="iucnTagType(row.iucnStatus)">{{ row.iucnStatus || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/species/detail/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :page-size="10" :total="total" layout="total,prev,pager,next" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSpeciesList } from '@/api/species'

const keyword = ref('')
const page = ref(1)
const total = ref(0)
const speciesList = ref<any[]>([])

function iucnTagType(status: string) {
  const map: Record<string, string> = { CR: 'danger', EN: 'warning', VU: '', NT: 'success', LC: 'success' }
  return (map[status] || 'info') as any
}

async function search() {
  const res: any = await getSpeciesList({ keyword: keyword.value, page: page.value, size: 10 })
  speciesList.value = res.data?.records || []
  total.value = res.data?.total || 0
}

onMounted(search)
</script>
