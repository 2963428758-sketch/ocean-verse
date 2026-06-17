<template>
  <div class="register-page">
    <!-- 全屏视频背景 -->
    <div class="video-bg">
      <video autoplay muted loop playsinline class="bg-video">
        <source :src="oceanVideo" type="video/mp4" />
      </video>
    </div>

    <!-- 暗色遮罩 -->
    <div class="overlay"></div>

    <!-- 居中毛玻璃注册框 -->
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

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            prefix-icon="Message"
            placeholder="邮箱"
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import oceanVideo from '@/assets/videos/ocean-bg.mp4'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }]
}

async function handleRegister() {
  await formRef.value?.validate()
  if (form.password !== form.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await register({ username: form.username, password: form.password, email: form.email })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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

/* ===== 居中毛玻璃注册框 ===== */
.register-card {
  position: relative;
  z-index: 10;
  width: 420px;
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

/* ===== 表单样式 ===== */
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

/* ===== 注册按钮 ===== */
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

/* ===== 底部链接 ===== */
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

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .register-card {
    padding: 32px 24px 24px;
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
