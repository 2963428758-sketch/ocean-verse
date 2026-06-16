<template>
  <div>
    <div class="page-header"><h2>用户管理</h2><p>系统用户列表</p></div>

    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名/姓名/邮箱" clearable style="width: 280px;" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px;">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机" width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170">
          <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" type="success" size="small" link @click="handleStatus(row, 1)">启用</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" link @click="handleStatus(row, 0)">禁用</el-button>
            <el-button v-if="row.status !== 2" type="danger" size="small" link @click="handleStatus(row, 2)">锁定</el-button>
          </template>
        </el-table-column>
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
import { listUsers, updateUserStatus } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

const roleMap: Record<string, string> = {
  SUPER_ADMIN: '超级管理员', ADMIN: '管理员', RESEARCHER: '研究员', OBSERVER: '观测员', VIEWER: '访客'
}

function roleLabel(role: string) { return roleMap[role] || role || '-' }

function roleTagType(role: string) {
  const map: Record<string, string> = { SUPER_ADMIN: 'danger', ADMIN: 'warning', RESEARCHER: '', OBSERVER: 'success', VIEWER: 'info' }
  return (map[role] || 'info') as any
}

function statusLabel(s: number) { return { 0: '禁用', 1: '正常', 2: '锁定' }[s] || '未知' }
function statusTagType(s: number) { return ({ 0: 'danger', 1: 'success', 2: 'warning' }[s] || 'info') as any }

async function loadData() {
  loading.value = true
  try {
    const res: any = await listUsers({ page: page.value, size: pageSize.value, keyword: keyword.value || undefined })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function handleStatus(row: any, status: number) {
  const action = statusLabel(status)
  await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '确认操作', { type: 'warning' })
  await updateUserStatus(row.id, status)
  ElMessage.success(`已${action}`)
  await loadData()
}

onMounted(() => { loadData() })
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
