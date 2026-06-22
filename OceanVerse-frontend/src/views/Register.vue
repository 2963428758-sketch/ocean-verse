<template>
  <div class="register-page">
    <div class="video-bg">
      <video autoplay muted loop playsinline class="bg-video">
        <source :src="oceanVideo" type="video/mp4" />
      </video>
    </div>

    <div class="overlay"></div>

    <div class="register-card">
      <div class="card-header">
        <span class="brand-icon">&#127754;</span>
        <h1 class="card-title">OceanVerse</h1>
        <p class="card-subtitle">创建你的账号，加入海洋探索社区</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="register-form" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="User"
            placeholder="用户名"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            prefix-icon="Lock"
            placeholder="密码"
            type="password"
            show-password
            size="large"
          />
          <div v-if="form.password" class="pwd-strength">
            <div class="strength-bar">
              <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent }"></div>
            </div>
            <span class="strength-label" :class="strengthClass">{{ strengthLabel }}</span>
            <div class="strength-checks">
              <span :class="checks.minLength ? 'pass' : 'fail'">{{ checks.minLength ? '✓' : '✗' }} 至少8位字符</span>
              <span :class="checks.hasUpper ? 'pass' : 'fail'">{{ checks.hasUpper ? '✓' : '✗' }} 包含大写字母</span>
              <span :class="checks.hasLower ? 'pass' : 'fail'">{{ checks.hasLower ? '✓' : '✗' }} 包含小写字母</span>
              <span :class="checks.hasDigit ? 'pass' : 'fail'">{{ checks.hasDigit ? '✓' : '✗' }} 包含数字</span>
              <span :class="checks.hasSpecial ? 'pass' : 'fail'">{{ checks.hasSpecial ? '✓' : '✗' }} 包含特殊字符</span>
              <span :class="checks.enoughTypes ? 'pass' : 'fail'">{{ checks.enoughTypes ? '✓' : '✗' }} 至少满足以上3种</span>
              <span v-if="!checks.noWeakDict" class="fail">✗ 常见弱密码</span>
              <span v-if="!checks.noUsername" class="fail">✗ 不能包含用户名</span>
              <span v-if="!checks.noConsecutive" class="fail">✗ 不能含连续字符</span>
              <span v-if="!checks.noRepeated" class="fail">✗ 不能含重复字符</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            prefix-icon="Lock"
            placeholder="确认密码"
            type="password"
            show-password
            size="large"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              prefix-icon="Key"
              placeholder="验证码"
              size="large"
              class="captcha-input"
              @keyup.enter="handleRegister"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              alt="验证码"
              class="captcha-img"
              @click="refreshCaptcha"
              title="点击刷新"
            />
            <el-button class="captcha-refresh" size="large" @click="refreshCaptcha" :loading="captchaLoading">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            class="register-btn"
            size="large"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        已有账号？<router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getCaptcha } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import oceanVideo from '@/assets/videos/ocean-bg.mp4'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const captchaLoading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '', captchaKey: '', captchaCode: '' })
const captchaImage = ref('')

// -- real-time password strength checks --
const UPPER = /[A-Z]/
const LOWER = /[a-z]/
const DIGIT = /[0-9]/
const SPECIAL = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/
const CONSECUTIVE = /(?:abcdef|bcdefg|cdefgh|defghi|efghij|fghijk|ghijkl|hijklm|ijklmn|jklmno|klmnop|lmnopq|mnopqr|nopqrs|opqrst|pqrstu|qrstuv|rstuvw|stuvwx|tuvwxy|uvwxyz|123456|234567|345678|456789|567890|654321|765432|876543|987654)/i
const REPEATED = /(.)\1{3,}/
const WEAK_DICT = new Set([
  '123456','password','123456789','12345678','12345','1234567890','1234567',
  'qwerty','abc123','111111','123123','admin','password1','iloveyou','welcome',
  'monkey','dragon','master','football','baseball','sunshine','princess',
  '1234','123','000000','666666','888888','qwerty123','1q2w3e4r','passw0rd'
])

const checks = computed(() => {
  const pwd = form.password
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
  const username = form.username
  const noUsername = !username || !pwd.toLowerCase().includes(username.toLowerCase())

  return { minLength, hasUpper, hasLower, hasDigit, hasSpecial, enoughTypes, noWeakDict, noConsecutive, noRepeated, noUsername }
})

const strengthLabel = computed(() => {
  const { minLength, enoughTypes } = checks.value
  const pwd = form.password
  if (!pwd || pwd.length < 3) return ''
  const typeCount = [checks.value.hasUpper, checks.value.hasLower, checks.value.hasDigit, checks.value.hasSpecial].filter(Boolean).length
  if (!minLength || typeCount < 2) return '弱'
  if (minLength && typeCount >= 4 && pwd.length >= 12) return '强'
  if (enoughTypes) return '中'
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

// -- form validation rules --
const validatePassword = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8) {
    callback(new Error('密码长度至少8位'))
    return
  }
  const chk = checks.value
  const digitsOnly = /^\d+$/.test(value)
  const lettersOnly = /^[a-zA-Z]+$/.test(value)
  if (digitsOnly) {
    callback(new Error('密码不能为纯数字'))
    return
  }
  if (lettersOnly) {
    callback(new Error('密码不能为纯字母'))
    return
  }
  if (!chk.noWeakDict) {
    callback(new Error('该密码为常见弱密码，请更换'))
    return
  }
  if (!chk.noUsername) {
    callback(new Error('密码不能包含用户名'))
    return
  }
  if (!chk.noConsecutive) {
    callback(new Error('密码包含连续字符（如123456、abcdef）'))
    return
  }
  if (!chk.noRepeated) {
    callback(new Error('密码包含过多重复字符'))
    return
  }
  if (!chk.enoughTypes) {
    callback(new Error('需包含大写、小写、数字、特殊字符中至少3种'))
    return
  }
  callback()
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const res: any = await getCaptcha()
    form.captchaKey = res.data.captchaKey
    captchaImage.value = res.data.imageBase64
    form.captchaCode = ''
  } finally {
    captchaLoading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})

async function handleRegister() {
  if (form.password !== form.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await formRef.value?.validate()
    await register({ username: form.username, password: form.password, captchaKey: form.captchaKey, captchaCode: form.captchaCode })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #0a1628;
}

.video-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

.bg-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(
    135deg,
    rgba(5, 15, 35, 0.40) 0%,
    rgba(10, 30, 60, 0.30) 40%,
    rgba(8, 20, 50, 0.35) 100%
  );
}

.register-card {
  position: relative;
  z-index: 10;
  width: 460px;
  max-width: 92vw;
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 44px 40px 32px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.20),
    inset 0 1px 0 rgba(255, 255, 255, 0.10);
  animation: cardFadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(24px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  font-size: 42px;
  display: block;
  margin-bottom: 12px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.2));
}

.card-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.card-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  letter-spacing: 0.5px;
}

// ========== 密码强度指示 ==========
.pwd-strength {
  margin-top: 8px;
}

.strength-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
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

  span {
    font-size: 11px;
  }

  .pass { color: #67c23a; }
  .fail { color: rgba(255, 255, 255, 0.35); }
}

// ========== 表单 ==========
.register-form {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.06) !important;
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 12px;
    box-shadow: none !important;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.10) !important;
      border-color: rgba(255, 255, 255, 0.28);
    }

    &.is-focus {
      background: rgba(255, 255, 255, 0.12) !important;
      border-color: rgba(100, 180, 255, 0.6);
      box-shadow: 0 0 0 3px rgba(100, 180, 255, 0.15) !important;
    }
  }

  :deep(.el-input__inner) {
    color: #fff !important;

    &::placeholder {
      color: rgba(255, 255, 255, 0.45) !important;
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: rgba(255, 255, 255, 0.5) !important;
  }

  :deep(.el-input__suffix .el-icon) {
    color: rgba(255, 255, 255, 0.5) !important;
  }

  :deep(.el-form-item__error) {
    color: #ffb4b4;
  }

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
}

// ========== 验证码 ==========
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;

  .captcha-input {
    flex: 1;
  }

  .captcha-img {
    height: 44px;
    width: 130px;
    border-radius: 8px;
    cursor: pointer;
    border: 1px solid rgba(255, 255, 255, 0.15);
    flex-shrink: 0;
  }

  .captcha-refresh {
    width: 44px;
    height: 44px;
    padding: 0;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: rgba(255, 255, 255, 0.7);

    &:hover {
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
      border-color: rgba(255, 255, 255, 0.3);
    }
  }
}

// ========== 按钮 ==========
.register-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  color: #fff !important;
  border-radius: 12px !important;
  border: none !important;
  letter-spacing: 4px;
  background: linear-gradient(135deg, rgba(60, 140, 220, 0.75) 0%, rgba(40, 100, 180, 0.85) 100%) !important;
  backdrop-filter: blur(8px);
  transition: all 0.3s ease !important;

  &:hover {
    background: linear-gradient(135deg, rgba(70, 155, 240, 0.85) 0%, rgba(50, 120, 200, 0.95) 100%) !important;
    transform: translateY(-1px);
    box-shadow: 0 6px 24px rgba(40, 100, 200, 0.35);
  }

  &:active {
    transform: translateY(0);
  }
}

.card-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);

  a {
    color: rgba(150, 210, 255, 0.85);
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;

    &:hover {
      color: #fff;
      text-decoration: underline;
    }
  }
}

@media (max-width: 480px) {
  .register-card {
    padding: 32px 24px 24px;
    border-radius: 16px;
  }

  .card-title { font-size: 24px; }
  .brand-icon { font-size: 36px; }
}
</style>
