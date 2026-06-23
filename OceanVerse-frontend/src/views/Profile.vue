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
              <div class="password-section" style="max-width: 500px; margin-top: 16px;">
                <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
                  <el-form-item label="旧密码" prop="oldPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                    <div v-if="passwordForm.newPassword" class="pwd-strength">
                      <div class="strength-bar">
                        <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent }" />
                      </div>
                      <span class="strength-label" :class="strengthClass">{{ strengthLabel }}</span>
                      <div class="strength-checks">
                        <span :class="pwChecks.minLength ? 'pass' : 'fail'">{{ pwChecks.minLength ? '✓' : '✗' }} 至少8位字符</span>
                        <span :class="pwChecks.hasUpper ? 'pass' : 'fail'">{{ pwChecks.hasUpper ? '✓' : '✗' }} 包含大写字母</span>
                        <span :class="pwChecks.hasLower ? 'pass' : 'fail'">{{ pwChecks.hasLower ? '✓' : '✗' }} 包含小写字母</span>
                        <span :class="pwChecks.hasDigit ? 'pass' : 'fail'">{{ pwChecks.hasDigit ? '✓' : '✗' }} 包含数字</span>
                        <span :class="pwChecks.hasSpecial ? 'pass' : 'fail'">{{ pwChecks.hasSpecial ? '✓' : '✗' }} 包含特殊字符</span>
                        <span :class="pwChecks.enoughTypes ? 'pass' : 'fail'">{{ pwChecks.enoughTypes ? '✓' : '✗' }} 至少满足以上3种</span>
                        <span v-if="!pwChecks.noWeakDict" class="fail">✗ 常见弱密码</span>
                        <span v-if="!pwChecks.noUsername" class="fail">✗ 不能包含用户名</span>
                        <span v-if="!pwChecks.noConsecutive" class="fail">✗ 不能含连续字符</span>
                        <span v-if="!pwChecks.noRepeated" class="fail">✗ 不能含重复字符</span>
                      </div>
                    </div>
                  </el-form-item>
                  <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                    <div v-if="passwordForm.confirmPassword && passwordForm.confirmPassword === passwordForm.newPassword" class="match-hint success mt-8">✓ 两次密码一致</div>
                    <div v-else-if="passwordForm.confirmPassword && passwordForm.confirmPassword !== passwordForm.newPassword" class="match-hint error mt-8">✗ 两次密码不一致</div>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">修改密码</el-button>
                    <el-button @click="resetPasswordForm">重置</el-button>
                  </el-form-item>
                </el-form>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, updatePassword, uploadAvatar, deleteAccount } from '@/api/user'
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

// -- real-time password strength (与注册页一致) --
const UPPER = /[A-Z]/
const LOWER = /[a-z]/
const DIGIT = /[0-9]/
const SPECIAL = /[!@#\$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/
const CONSECUTIVE = /(?:abcdef|bcdefg|cdefgh|defghi|efghij|fghijk|ghijkl|hijklm|ijklmn|jklmno|klmnop|lmnopq|mnopqr|nopqrs|opqrst|pqrstu|qrstuv|rstuvw|stuvwx|tuvwxy|uvwxyz|123456|234567|345678|456789|567890|654321|765432|876543|987654)/i
const REPEATED = /(.)\1{3,}/
const WEAK_DICT = new Set([
  '123456','password','123456789','12345678','12345','1234567890','1234567',
  'qwerty','abc123','111111','123123','admin','password1','iloveyou','welcome',
  'monkey','dragon','master','football','baseball','sunshine','princess',
  '1234','123','000000','666666','888888','qwerty123','1q2w3e4r','passw0rd'
])

const pwChecks = computed(() => {
  const pwd = passwordForm.newPassword
  if (!pwd) return { minLength: false, hasUpper: false, hasLower: false, hasDigit: false, hasSpecial: false, enoughTypes: false, noWeakDict: true, noConsecutive: true, noRepeated: true, noUsername: true }
  const hasUpper = UPPER.test(pwd)
  const hasLower = LOWER.test(pwd)
  const hasDigit = DIGIT.test(pwd)
  const hasSpecial = SPECIAL.test(pwd)
  const typeCount = [hasUpper, hasLower, hasDigit, hasSpecial].filter(Boolean).length
  const minLength = pwd.length >= 8
  const isDigitOnly = /^\d+$/.test(pwd)
  const isLetterOnly = /^[a-zA-Z]+$/.test(pwd)
  const enoughTypes = typeCount >= 3 && !isDigitOnly && !isLetterOnly
  const noWeakDict = !WEAK_DICT.has(pwd.toLowerCase())
  const noConsecutive = !CONSECUTIVE.test(pwd)
  const noRepeated = !REPEATED.test(pwd)
  const username = userStore.username
  const noUsername = !username || !pwd.toLowerCase().includes(username.toLowerCase())
  return { minLength, hasUpper, hasLower, hasDigit, hasSpecial, enoughTypes, noWeakDict, noConsecutive, noRepeated, noUsername }
})

const strengthLabel = computed(() => {
  const { minLength } = pwChecks.value
  const pwd = passwordForm.newPassword
  if (!pwd || pwd.length < 3) return ''
  const typeCount = [pwChecks.value.hasUpper, pwChecks.value.hasLower, pwChecks.value.hasDigit, pwChecks.value.hasSpecial].filter(Boolean).length
  if (!minLength || typeCount < 2) return '弱'
  if (minLength && typeCount >= 4 && pwd.length >= 12) return '强'
  if (pwChecks.value.enoughTypes) return '中'
  return '弱'
})

const strengthClass = computed(() => {
  const label = strengthLabel.value
  if (label === '强') return 'strong'
  if (label === '中') return 'medium'
  if (label === '弱') return 'weak'
  return ''
})

const strengthPercent = computed(() => {
  const label = strengthLabel.value
  if (label === '强') return '100%'
  if (label === '中') return '66%'
  if (label === '弱') return '33%'
  return '0%'
})

const validateNewPassword = (_rule: any, value: string, callback: any) => {
  if (!value) { callback(new Error('请输入新密码')); return }
  if (value.length < 8) { callback(new Error('密码长度至少8位')); return }
  const chk = pwChecks.value
  if (/^\d+$/.test(value)) { callback(new Error('密码不能为纯数字')); return }
  if (/^[a-zA-Z]+$/.test(value)) { callback(new Error('密码不能为纯字母')); return }
  if (!chk.noWeakDict) { callback(new Error('该密码为常见弱密码，请更换')); return }
  if (!chk.noUsername) { callback(new Error('密码不能包含用户名')); return }
  if (!chk.noConsecutive) { callback(new Error('密码包含连续字符（如123456、abcdef）')); return }
  if (!chk.noRepeated) { callback(new Error('密码包含过多重复字符')); return }
  if (!chk.enoughTypes) { callback(new Error('需包含大写、小写、数字、特殊字符中至少3种')); return }
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

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
    resetPasswordForm()
  } finally {
    passwordLoading.value = false
  }
}

function resetPasswordForm() {
  passwordFormRef.value?.resetFields()
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
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
    await uploadAvatar(file)
    ElMessage.success('头像上传成功')
    await loadUserInfo()
  } catch {
    // error handled by http interceptor
  } finally {
    avatarUploading.value = false
    input.value = ''
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

.password-section {
  .mt-8 { margin-top: 8px; }
}

// 密码强度指示（与注册页一致）
.pwd-strength {
  margin-top: 8px;
}

.strength-bar {
  height: 4px;
  background: var(--neutral-100);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease, background 0.3s ease;
  &.weak { background: #f56c6c; }
  &.medium { background: #e6a23c; }
  &.strong { background: #67c23a; }
}

.strength-label {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  display: inline-block;
  &.weak { color: #f56c6c; }
  &.medium { color: #e6a23c; }
  &.strong { color: #67c23a; }
}

.strength-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  span { font-size: 11px; }
  .pass { color: #67c23a; }
  .fail { color: var(--neutral-400); }
}

.match-hint {
  font-size: 12px;
  &.success { color: var(--el-color-success); }
  &.error { color: var(--el-color-danger); }
}
</style>
