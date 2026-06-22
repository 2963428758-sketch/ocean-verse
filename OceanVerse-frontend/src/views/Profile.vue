<template>
  <div class="profile-page">
    <div class="page-header"><h2>个人中心</h2><p>管理您的账户信息</p></div>

    <el-row :gutter="24">
      <el-col :span="8">
        <el-card class="info-card">
          <div class="avatar-section">
            <el-avatar :size="80" :src="userStore.avatarUrl || undefined">
              {{ userStore.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <h3>{{ userStore.nickname || userStore.username }}</h3>
            <el-tag :type="roleTagType" size="small">{{ roleLabel }}</el-tag>
          </div>
          <el-descriptions :column="1" class="mt-16">
            <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userStore.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="真实姓名">{{ userStore.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ userStore.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="编辑资料" name="profile">
              <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px" style="max-width: 500px; margin-top: 16px;">
                <el-form-item label="头像">
                  <div class="avatar-upload-row">
                    <el-avatar :size="60" :src="userStore.avatarUrl || undefined">
                      {{ userStore.username?.charAt(0)?.toUpperCase() }}
                    </el-avatar>
                    <el-button size="small" type="primary" :loading="avatarUploading" @click="triggerAvatarUpload">
                      上传头像
                    </el-button>
                    <input ref="avatarInputRef" type="file" accept="image/*" style="display: none" @change="handleAvatarFileChange" />
                  </div>
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="30" />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="50" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" style="max-width: 500px; margin-top: 16px;">
                <el-form-item label="旧密码" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码 (6-50位)" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="登录历史" name="loginHistory">
              <el-table :data="loginHistory" stripe style="margin-top: 16px;" v-loading="loginHistoryLoading">
                <el-table-column prop="loginTime" label="登录时间" width="180" />
                <el-table-column prop="ipAddress" label="IP地址" width="150" />
                <el-table-column prop="userAgent" label="设备/浏览器" min-width="200" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                      {{ row.status === 1 ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="message" label="备注" min-width="120" show-overflow-tooltip />
              </el-table>
              <div class="pagination-wrap">
                <el-pagination
                  v-model:current-page="loginHistoryPage"
                  :page-size="loginHistorySize"
                  :total="loginHistoryTotal"
                  layout="prev, pager, next"
                  @current-change="loadLoginHistory"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 24px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span class="danger-title">危险操作</span>
          </template>
          <div class="danger-zone">
            <div>
              <p><strong>注销账号</strong></p>
              <p class="danger-desc">注销后账号将被禁用，无法登录。数据保留30天后彻底删除。</p>
            </div>
            <el-popconfirm
              title="确定要注销账号吗？注销后无法登录。"
              confirm-button-text="确认注销"
              cancel-button-text="取消"
              @confirm="handleDeleteAccount"
            >
              <template #reference>
                <el-button type="danger" :loading="deleteLoading">注销账号</el-button>
              </template>
            </el-popconfirm>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, updatePassword, uploadAvatar, deleteAccount, getLoginHistory } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('profile')
const profileFormRef = ref()
const passwordFormRef = ref()
const avatarInputRef = ref<HTMLInputElement>()
const profileLoading = ref(false)
const passwordLoading = ref(false)
const avatarUploading = ref(false)
const deleteLoading = ref(false)

const profileForm = reactive({
  nickname: '',
  realName: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  nickname: [{ max: 30, message: '昵称长度不超过30', trigger: 'blur' }],
  realName: [{ max: 50, message: '真实姓名长度不超过50', trigger: 'blur' }]
}

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度6-50位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// -- login history --
const loginHistory = ref<any[]>([])
const loginHistoryLoading = ref(false)
const loginHistoryPage = ref(1)
const loginHistorySize = ref(10)
const loginHistoryTotal = ref(0)

// -- role display --
const roleMap: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  ADMIN: '管理员',
  RESEARCHER: '研究员',
  OBSERVER: '观测员',
  VIEWER: '访客'
}

const roleLabel = computed(() => roleMap[userStore.role] || userStore.role || '访客')

const roleTagType = computed(() => {
  const map: Record<string, string> = { SUPER_ADMIN: 'danger', ADMIN: 'warning', RESEARCHER: '', OBSERVER: 'success', VIEWER: 'info' }
  return (map[userStore.role] || 'info') as any
})

async function loadUserInfo() {
  try {
    const res: any = await getProfile()
    if (res.code === 200 && res.data) {
      userStore.setUserInfo(res.data)
      profileForm.nickname = res.data.nickname || ''
      profileForm.realName = res.data.realName || ''
    }
  } catch {
    // ignore
  }
}

async function handleUpdateProfile() {
  await profileFormRef.value?.validate()
  profileLoading.value = true
  try {
    await updateProfile(profileForm)
    ElMessage.success('资料更新成功')
    await loadUserInfo()
  } finally {
    profileLoading.value = false
  }
}

async function handleUpdatePassword() {
  await passwordFormRef.value?.validate()
  passwordLoading.value = true
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } finally {
    passwordLoading.value = false
  }
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function handleAvatarFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('仅支持图片格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过2MB')
    return
  }

  avatarUploading.value = true
  try {
    const res: any = await uploadAvatar(file)
    const url = res.data?.url || res.data
    ElMessage.success('头像上传成功')
    await loadUserInfo()
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

async function loadLoginHistory() {
  loginHistoryLoading.value = true
  try {
    const res: any = await getLoginHistory(loginHistoryPage.value, loginHistorySize.value)
    loginHistory.value = res.data?.records || []
    loginHistoryTotal.value = res.data?.total || 0
  } finally {
    loginHistoryLoading.value = false
  }
}

async function handleDeleteAccount() {
  deleteLoading.value = true
  try {
    await deleteAccount()
    ElMessage.success('账号已注销')
    userStore.logout()
    router.push('/login')
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})

watch(activeTab, (tab) => {
  if (tab === 'loginHistory') {
    loadLoginHistory()
  }
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 1200px;
}

.info-card {
  text-align: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--neutral-100);

  h3 {
    font-size: 18px;
    color: var(--neutral-800);
    margin: 0;
  }
}

.mt-16 {
  margin-top: 16px;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.danger-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-color-danger);
}

.danger-zone {
  display: flex;
  justify-content: space-between;
  align-items: center;

  p {
    margin: 0;
    line-height: 1.6;
  }

  .danger-desc {
    font-size: 13px;
    color: var(--neutral-500);
    margin-top: 4px;
  }
}
</style>
