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
            <el-descriptions-item label="邮箱">{{ userStore.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机">{{ userStore.phone || '-' }}</el-descriptions-item>
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
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="50" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="头像URL" prop="avatarUrl">
                  <el-input v-model="profileForm.avatarUrl" placeholder="请输入头像链接地址" />
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
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, updatePassword } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('profile')
const profileFormRef = ref()
const passwordFormRef = ref()
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  realName: '',
  email: '',
  phone: '',
  avatarUrl: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  email: [{ type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }],
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
      profileForm.realName = res.data.realName || ''
      profileForm.email = res.data.email || ''
      profileForm.phone = res.data.phone || ''
      profileForm.avatarUrl = res.data.avatarUrl || ''
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

onMounted(() => {
  loadUserInfo()
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
</style>
