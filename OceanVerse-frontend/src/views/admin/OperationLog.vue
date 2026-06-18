<template>
  <div>
    <div class="page-header"><h2>操作日志</h2><p>管理员操作行为记录</p></div>

    <el-card>
      <div class="toolbar">
        <el-input v-model="searchOperator" placeholder="搜索操作人" clearable style="width: 200px;" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchModule" placeholder="搜索模块" clearable style="width: 180px;" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Folder /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px;">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.operationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="ipAddress" label="操作IP" width="140" />
        <el-table-column prop="executionTime" label="耗时(ms)" width="100" />
        <el-table-column prop="operationResult" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.operationResult === 1 ? 'success' : 'danger'" size="small">
              {{ row.operationResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listOperationLogs } from '@/api/admin'
import { Search, Folder } from '@element-plus/icons-vue'

const searchOperator = ref('')
const searchModule = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res: any = await listOperationLogs({
      page: page.value,
      size: pageSize.value,
      module: searchModule.value || undefined,
      operatorName: searchOperator.value || undefined
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
