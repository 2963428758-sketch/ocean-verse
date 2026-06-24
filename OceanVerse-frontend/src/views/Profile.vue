<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
      <p class="page-subtitle">管理您的账户信息和密码</p>
    </div>

    <div class="profile-layout">
      <!-- 左侧信息卡 -->
      <aside class="profile-sidebar">
        <div class="user-card">
          <div class="card-banner" :class="roleColorClass" />
          <div class="card-body">
            <div class="avatar-wrapper" @click="triggerAvatarUpload">
              <el-avatar :size="80" :src="userStore.avatarUrl || undefined" class="user-avatar">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="avatar-ring" :class="roleColorClass" />
              <div class="avatar-overlay">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
              </div>
              <input ref="avatarInputRef" type="file" accept="image/*" style="display:none" @change="handleAvatarFileChange" />
            </div>
            <h3 class="user-name">{{ userStore.nickname || userStore.username }}</h3>
            <el-tag :type="roleTagType" size="small" effect="dark" round>{{ roleLabel }}</el-tag>
          </div>
          <div class="card-info">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ userStore.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">真实姓名</span>
              <span class="info-value">{{ userStore.realName || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDate(userStore.createTime) }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧功能 -->
      <main class="profile-main">
        <div class="content-card">
          <!-- 自定义下划线 Tab -->
          <nav class="custom-tabs">
            <button :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">
              编辑资料
            </button>
            <button :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">
              修改密码
            </button>
          </nav>

          <!-- 编辑资料 -->
          <section v-show="activeTab === 'profile'" class="tab-panel">
            <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top" class="profile-form">
              <el-form-item label="昵称" prop="nickname">
                <div class="input-with-counter">
                  <el-input v-model="profileForm.nickname" placeholder="给自己起一个好听的名字" maxlength="30" />
                  <span class="counter">{{ profileForm.nickname.length }}/30</span>
                </div>
              </el-form-item>
              <el-form-item label="真实姓名" prop="realName">
                <div class="input-with-counter">
                  <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="50" />
                  <span class="counter">{{ profileForm.realName.length }}/50</span>
                </div>
              </el-form-item>
              <el-form-item class="btn-row">
                <el-button type="primary" :loading="profileLoading" :class="{ saved: showSaved }" @click="handleUpdateProfile">
                  {{ showSaved ? '✓ 已保存' : '保存修改' }}
                </el-button>
              </el-form-item>
            </el-form>
          </section>

          <!-- 修改密码 -->
          <section v-show="activeTab === 'password'" class="tab-panel">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top" class="password-form">
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                  placeholder="请输入新密码"
                  :class="['strength-input', strengthClass]"
                />
                <div v-if="passwordForm.newPassword" class="pwd-strength">
                  <div class="strength-bar">
                    <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent }" />
                  </div>
                  <span class="strength-label" :class="strengthClass">{{ strengthLabel }}</span>
                  <div class="strength-checks" v-show="showAllChecks">
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
                  <button type="button" class="toggle-checks" @click="showAllChecks = !showAllChecks">
                    {{ showAllChecks ? '收起规则 ▲' : '查看全部规则 ▼' }}
                  </button>
                </div>
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                <div v-if="passwordForm.confirmPassword && passwordForm.confirmPassword === passwordForm.newPassword" class="match-hint success">✓ 两次密码一致</div>
                <div v-else-if="passwordForm.confirmPassword && passwordForm.confirmPassword !== passwordForm.newPassword" class="match-hint error">✗ 两次密码不一致</div>
              </el-form-item>
              <el-form-item class="btn-row">
                <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">修改密码</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </section>
        </div>

        <!-- 危险操作 -->
        <div class="danger-zone">
          <div class="danger-bar" />
          <div class="danger-content">
            <div class="danger-text">
              <strong>注销账号</strong>
              <p>注销后账号将被禁用，无法登录。数据保留30天后彻底删除。</p>
            </div>
            <el-popconfirm
              title="确定要注销账号吗？"
              confirm-button-text="确认注销"
              cancel-button-text="取消"
              @confirm="handleDeleteAccount"
            >
              <template #reference>
                <el-button type="danger" :loading="deleteLoading">注销账号</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </main>
    </div>
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
const showSaved = ref(false)
const showAllChecks = ref(false)

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
  newPassword: [{ required: true, validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const roleMap: Record<string, string> = {
  SUPER_ADMIN: '超级管理员', ADMIN: '管理员', RESEARCHER: '研究员', OBSERVER: '观测员', VIEWER: '访客'
}

const roleLabel = computed(() => roleMap[userStore.role] || userStore.role || '访客')

const roleTagType = computed(() => {
  const map: Record<string, string> = { SUPER_ADMIN: 'danger', ADMIN: 'warning', RESEARCHER: '', OBSERVER: 'success', VIEWER: 'info' }
  return (map[userStore.role] || 'info') as any
})

const roleColorClass = computed(() => {
  const map: Record<string, string> = { SUPER_ADMIN: 'role-danger', ADMIN: 'role-warning', RESEARCHER: 'role-primary', OBSERVER: 'role-success', VIEWER: 'role-info' }
  return map[userStore.role] || 'role-info'
})

function formatDate(t?: string) {
  if (!t) return '-'
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function loadUserInfo() {
  try {
    const res: any = await getProfile()
    if (res.code === 200 && res.data) {
      userStore.setUserInfo(res.data)
      profileForm.nickname = res.data.nickname || ''
      profileForm.realName = res.data.realName || ''
    }
  } catch { /* ignore */ }
}

async function handleUpdateProfile() {
  await profileFormRef.value?.validate()
  profileLoading.value = true
  try {
    await updateProfile(profileForm)
    showSaved.value = true
    setTimeout(() => { showSaved.value = false }, 2000)
    await loadUserInfo()
  } finally { profileLoading.value = false }
}

async function handleUpdatePassword() {
  await passwordFormRef.value?.validate()
  passwordLoading.value = true
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } finally { passwordLoading.value = false }
}

function resetPasswordForm() {
  passwordFormRef.value?.resetFields()
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showAllChecks.value = false
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function handleAvatarFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.error('仅支持图片格式'); return }
  if (file.size > 2 * 1024 * 1024) { ElMessage.error('头像大小不能超过2MB'); return }

  avatarUploading.value = true
  try {
    await uploadAvatar(file)
    ElMessage.success('头像上传成功')
    await loadUserInfo()
  } catch { /* error handled by http interceptor */ }
  finally { avatarUploading.value = false; input.value = '' }
}

async function handleDeleteAccount() {
  deleteLoading.value = true
  try {
    await deleteAccount()
    ElMessage.success('账号已注销')
    userStore.logout()
    router.push('/login')
  } finally { deleteLoading.value = false }
}

onMounted(() => { loadUserInfo() })
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 1080px;
  animation: fadeIn 0.4s ease;
}

.page-header {
  margin-bottom: 28px;
  h2 { font-size: 24px; color: var(--neutral-800); margin: 0 0 6px; }
  .page-subtitle { font-size: 14px; color: var(--neutral-400); margin: 0; }
}

/* ══════ 双栏布局 ══════ */
.profile-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.profile-sidebar {
  width: 300px;
  flex-shrink: 0;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

/* ══════ 用户卡片 ══════ */
.user-card {
  background: var(--surface-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--neutral-75);
  transition: box-shadow 0.3s ease;
  &:hover { box-shadow: var(--shadow-md); }
}

.card-banner {
  height: 64px;
  background: var(--gradient-ocean);
  &.role-danger { background: linear-gradient(135deg, #e74c3c, #c0392b); }
  &.role-warning { background: linear-gradient(135deg, #f39c12, #e67e22); }
  &.role-primary { background: linear-gradient(135deg, #3498db, #2980b9); }
  &.role-success { background: linear-gradient(135deg, #27ae60, #2ecc71); }
  &.role-info { background: linear-gradient(135deg, #95a5a6, #7f8c8d); }
}

.card-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 20px 16px;
  margin-top: -40px;
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;

  &:hover .avatar-overlay { opacity: 1; }
  &:hover .user-avatar { transform: scale(1.05); }
  &:hover .avatar-ring { opacity: 0.3; }
}

.user-avatar {
  transition: transform 0.2s ease;
  box-shadow: 0 0 0 4px var(--surface-card);
}

.avatar-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid var(--primary-main);
  opacity: 1;
  transition: opacity 0.2s ease;
  &.role-danger { border-color: #e74c3c; }
  &.role-warning { border-color: #f39c12; }
  &.role-primary { border-color: #3498db; }
  &.role-success { border-color: #27ae60; }
  &.role-info { border-color: #95a5a6; }
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.user-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--neutral-800);
  margin: 12px 0 6px;
}

.card-info {
  padding: 16px 20px 0;
  border-top: 1px solid var(--neutral-75);
  margin: 0 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  & + .info-item { border-top: 1px solid var(--neutral-50); }
}

.info-label {
  font-size: 13px;
  color: var(--neutral-400);
}

.info-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--neutral-700);
  text-align: right;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ══════ 内容卡片 ══════ */
.content-card {
  background: var(--surface-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--neutral-75);
  overflow: hidden;
}

/* ══════ 自定义下划线 Tab ══════ */
.custom-tabs {
  display: flex;
  border-bottom: 1px solid var(--neutral-75);
  padding: 0 24px;

  button {
    padding: 16px 20px;
    border: none;
    background: none;
    font-size: 14px;
    font-weight: 500;
    color: var(--neutral-400);
    cursor: pointer;
    position: relative;
    transition: color 0.2s;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 20px;
      right: 20px;
      height: 2px;
      background: var(--primary-main);
      border-radius: 1px;
      transform: scaleX(0);
      transition: transform 0.2s ease;
    }

    &:hover { color: var(--neutral-600); }
    &.active {
      color: var(--primary-main);
      font-weight: 600;
      &::after { transform: scaleX(1); }
    }
  }
}

.tab-panel {
  padding: 28px 24px;
}

/* ══════ 表单 ══════ */
.profile-form, .password-form {
  max-width: 440px;
}

.input-with-counter {
  position: relative;

  .counter {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 11px;
    color: var(--neutral-350);
    pointer-events: none;
    z-index: 1;
  }

  :deep(.el-input__inner) { padding-right: 42px; }
}

.btn-row {
  margin-top: 8px;
}

/* ══════ 保存按钮动画 ══════ */
:deep(.saved) {
  background: var(--el-color-success) !important;
  border-color: var(--el-color-success) !important;
  transition: all 0.3s ease;
}

/* ══════ 密码强度 ══════ */
.strength-input {
  :deep(.el-input__wrapper) {
    transition: box-shadow 0.3s ease;
  }
  &.weak :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #f56c6c inset !important; }
  &.medium :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #e6a23c inset !important; }
  &.strong :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #67c23a inset !important; }
}

.pwd-strength { margin-top: 8px; }

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
  margin-bottom: 4px;
  span { font-size: 11px; }
  .pass { color: #67c23a; }
  .fail { color: var(--neutral-400); }
}

.toggle-checks {
  border: none;
  background: none;
  font-size: 11px;
  color: var(--primary-main);
  cursor: pointer;
  padding: 0;
  margin-top: 2px;
  &:hover { text-decoration: underline; }
}

.match-hint {
  font-size: 12px;
  margin-top: 6px;
  &.success { color: var(--el-color-success); }
  &.error { color: var(--el-color-danger); }
}

/* ══════ 危险操作 ══════ */
.danger-zone {
  margin-top: 16px;
  background: var(--surface-card);
  border-radius: var(--radius-xl);
  border: 1px solid var(--neutral-75);
  overflow: hidden;
  display: flex;
}

.danger-bar {
  width: 4px;
  background: var(--el-color-danger);
  flex-shrink: 0;
}

.danger-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  gap: 16px;

  p { font-size: 13px; color: var(--neutral-400); margin: 4px 0 0; font-weight: 400; }
  strong { font-size: 14px; color: var(--neutral-700); }
}

/* ══════ 响应式 ══════ */
@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }
  .profile-sidebar { width: 100%; }
  .danger-content {
    flex-direction: column;
    align-items: flex-start;
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
