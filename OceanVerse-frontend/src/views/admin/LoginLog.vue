<template>
  <div>
    <div class="page-header"><h2>登录日志</h2><p>用户登录行为记录</p></div>

    <el-card>
      <div class="toolbar">
        <el-input v-model="searchUsername" placeholder="搜索用户名" clearable style="width: 200px;" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchStatus" placeholder="登录状态" clearable style="width: 140px;" @change="loadData">
          <el-option label="登录成功" :value="1" />
          <el-option label="登录失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px;">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ipAddress" label="登录IP" width="140" />
        <el-table-column prop="userAgent" label="User-Agent" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
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
import { listLoginLogs } from '@/api/admin'
import { Search } from '@element-plus/icons-vue'

const searchUsername = ref('')
const searchStatus = ref<number | undefined>(undefined)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res: any = await listLoginLogs({
      page: page.value,
      size: pageSize.value,
      username: searchUsername.value || undefined,
      status: searchStatus.value
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
