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
        <el-table-column prop="status" label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            <span v-if="row.status === 2 && row.lockRemainingSeconds > 0" style="font-size: 12px; color: #e6a23c; margin-left: 4px;">
              剩余 {{ row.lockRemainingSeconds }}s
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170">
          <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openRoleDialog(row)">分配角色</el-button>
            <el-button v-if="row.status !== 1" type="success" size="small" link @click="handleStatus(row, 1)">启用</el-button>
            <template v-if="!isSelf(row)">
              <el-button v-if="row.status === 1" type="warning" size="small" link @click="handleStatus(row, 0)">禁用</el-button>
              <el-button v-if="row.status !== 2" type="danger" size="small" link @click="handleStatus(row, 2)">锁定</el-button>
              <el-button type="danger" size="small" link @click="handleForceLogout(row)">下线</el-button>
            </template>
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

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
      <div v-if="currentUser">
        <p>用户: <strong>{{ currentUser.username }}</strong></p>
        <p>当前角色: <el-tag :type="roleTagType(currentUser.role)">{{ roleLabel(currentUser.role) }}</el-tag></p>
      </div>
      <el-divider />
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in allRoles" :key="role.id" :label="role.id">
          {{ role.roleName }} ({{ role.roleCode }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignRoles" :loading="assigning">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { listUsers, updateUserStatus, assignRoles, forceLogout } from '@/api/admin'
import { listRoles } from '@/api/role'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

let pollTimer: ReturnType<typeof setInterval> | null = null

const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

// 角色分配相关
const roleDialogVisible = ref(false)
const currentUser = ref<any>(null)
const allRoles = ref<any[]>([])
const selectedRoleIds = ref<number[]>([])
const assigning = ref(false)

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

function isSelf(row: any) { return row.id === userStore.userId }

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

async function loadRoles() {
  try {
    const res: any = await listRoles({ page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      allRoles.value = res.data.records || []
    }
  } catch (e) {
    console.error('加载角色列表失败', e)
  }
}

async function handleStatus(row: any, status: number) {
  const action = statusLabel(status)
  await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '确认操作', { type: 'warning' })
  await updateUserStatus(row.id, status)
  ElMessage.success(`已${action}`)
  await loadData()
}

function openRoleDialog(row: any) {
  currentUser.value = row
  // 根据当前角色找到对应的roleId
  const currentRole = allRoles.value.find(r => r.roleCode === row.role)
  selectedRoleIds.value = currentRole ? [currentRole.id] : []
  roleDialogVisible.value = true
}

async function handleAssignRoles() {
  if (!currentUser.value || selectedRoleIds.value.length === 0) {
    ElMessage.warning('请选择至少一个角色')
    return
  }
  assigning.value = true
  try {
    await assignRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    await loadData()
  } finally {
    assigning.value = false
  }
}

async function handleForceLogout(row: any) {
  await ElMessageBox.confirm(`确定要强制下线用户 "${row.username}" 吗？`, '确认操作', { type: 'warning' })
  try {
    await forceLogout(row.id)
    ElMessage.success('已强制下线')
  } catch (e) {
    // 错误已由拦截器处理
  }
}

onMounted(() => {
  loadData()
  loadRoles()
  pollTimer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
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
