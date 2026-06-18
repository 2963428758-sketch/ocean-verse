<template>
  <div class="login-page">
    <!-- 全屏视频背景 -->
    <div class="video-bg">
      <video autoplay muted loop playsinline class="bg-video">
        <source :src="oceanVideo" type="video/mp4" />
      </video>
    </div>

    <!-- 暗色遮罩 -->
    <div class="overlay"></div>

    <!-- 居中毛玻璃登录框 -->
    <div class="login-card">
      <div class="card-header">
        <span class="brand-icon">&#127754;</span>
        <h1 class="card-title">OceanVerse</h1>
        <p class="card-subtitle">智慧海洋物种观测与管理平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
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
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="rememberMe" label="记住我" />
        </div>

        <el-form-item>
          <el-button
            class="login-btn"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'
import oceanVideo from '@/assets/videos/ocean-bg.mp4'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res: any = await login(form)
    userStore.setLoginInfo({
      accessToken: res.data.accessToken,
      refreshToken: res.data.refreshToken,
      userId: res.data.userId,
      username: res.data.username,
      avatarUrl: res.data.avatarUrl,
      role: res.data.role
    })
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #0a1628;
}

/* ===== 视频背景 ===== */
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

/* ===== 暗色遮罩 ===== */
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

/* ===== 居中毛玻璃登录框 ===== */
.login-card {
  position: relative;
  z-index: 10;
  width: 420px;
  max-width: 92vw;
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 48px 40px 36px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.20),
    inset 0 1px 0 rgba(255, 255, 255, 0.10);
  animation: cardFadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ===== 卡片头部 ===== */
.card-header {
  text-align: center;
  margin-bottom: 36px;
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

/* ===== 表单样式 ===== */
.login-form {
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
    margin-bottom: 20px;
  }
}

/* ===== 记住我 ===== */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  :deep(.el-checkbox__label) {
    color: rgba(255, 255, 255, 0.65) !important;
    font-size: 13px;
  }

  :deep(.el-checkbox__inner) {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.3);
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background: rgba(100, 180, 255, 0.7);
    border-color: rgba(100, 180, 255, 0.8);
  }
}

/* ===== 登录按钮 ===== */
.login-btn {
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

/* ===== 底部链接 ===== */
.card-footer {
  text-align: center;
  margin-top: 12px;
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

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .login-card {
    padding: 36px 24px 28px;
    border-radius: 16px;
  }

  .card-title {
    font-size: 24px;
  }

  .brand-icon {
    font-size: 36px;
  }
}
</style>
