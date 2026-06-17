<template>
  <div>
    <div class="page-header">
      <div>
        <h2>角色管理</h2>
        <p>管理系统角色和权限分配</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%;">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleCode" label="角色代码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.permissionIds?.length || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openPermDialog(row)">分配权限</el-button>
            <el-button type="warning" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 1" type="danger" size="small" link @click="handleToggleStatus(row, 0)">禁用</el-button>
            <el-button v-else type="success" size="small" link @click="handleToggleStatus(row, 1)">启用</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="角色代码" prop="roleCode">
          <el-input v-model="formData.roleCode" :disabled="isEdit" placeholder="如: EDITOR" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="如: 编辑员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="600px">
      <div v-if="currentRole">
        <p>角色: <strong>{{ currentRole.roleName }}</strong> ({{ currentRole.roleCode }})</p>
      </div>
      <el-divider />
      <div v-loading="loadingPerms">
        <el-tree
          ref="treeRef"
          :data="permissionTree"
          :props="{ label: 'name', children: 'children' }"
          show-checkbox
          node-key="id"
          :default-checked-keys="checkedPermIds"
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignPermissions" :loading="assigningPerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listRoles, createRole, updateRole, deleteRole, toggleRoleStatus, assignPermissions } from '@/api/role'
import { getPermissionTree, getRolePermissions } from '@/api/permission'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<any[]>([])
const loading = ref(false)

// 编辑对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const editRoleId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const formData = ref({
  roleCode: '',
  roleName: '',
  description: ''
})
const formRules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色代码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 权限分配相关
const permDialogVisible = ref(false)
const currentRole = ref<any>(null)
const permissionTree = ref<any[]>([])
const checkedPermIds = ref<number[]>([])
const treeRef = ref<any>(null)
const loadingPerms = ref(false)
const assigningPerms = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res: any = await listRoles({ page: page.value, size: pageSize.value })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEdit.value = false
  editRoleId.value = null
  formData.value = { roleCode: '', roleName: '', description: '' }
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true
  editRoleId.value = row.id
  formData.value = {
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description || ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value && editRoleId.value) {
      await updateRole(editRoleId.value, formData.value)
      ElMessage.success('角色更新成功')
    } else {
      await createRole(formData.value)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row: any, status: number) {
  const action = status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定要${action}角色 "${row.roleName}" 吗？`, '确认操作', { type: 'warning' })
  try {
    await toggleRoleStatus(row.id, status)
    ElMessage.success(`已${action}`)
    await loadData()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定要删除角色 "${row.roleName}" 吗？此操作不可恢复。`, '确认删除', { type: 'error' })
  try {
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    await loadData()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

async function openPermDialog(row: any) {
  currentRole.value = row
  checkedPermIds.value = row.permissionIds || []
  permDialogVisible.value = true
  loadingPerms.value = true
  try {
    const res: any = await getPermissionTree()
    if (res.code === 200) {
      permissionTree.value = res.data || []
    }
  } finally {
    loadingPerms.value = false
  }
}

async function handleAssignPermissions() {
  if (!currentRole.value || !treeRef.value) return
  const checkedKeys = treeRef.value.getCheckedKeys(false)
  const halfCheckedKeys = treeRef.value.getHalfCheckedKeys()
  const allPermIds = [...checkedKeys, ...halfCheckedKeys]
  
  assigningPerms.value = true
  try {
    await assignPermissions(currentRole.value.id, allPermIds)
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
    await loadData()
  } finally {
    assigningPerms.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
