<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">🌊</div>
        <h1>注册 OceanVerse</h1>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" prefix-icon="User" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" prefix-icon="Message" placeholder="邮箱" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" prefix-icon="Lock" placeholder="密码" type="password" show-password size="large" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" prefix-icon="Lock" placeholder="确认密码" type="password" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" style="width:100%" @click="handleRegister">注 册</el-button>
        </el-form-item>
        <div class="login-footer">
          已有账号？<router-link to="/login">返回登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

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
.login-page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:var(--gradient-ocean); }
.login-card { width:400px; padding:40px; background:white; border-radius:var(--radius-lg); box-shadow:var(--shadow-md); }
.login-header { text-align:center; margin-bottom:32px; .logo{font-size:48px;margin-bottom:8px;} h1{font-size:24px;color:var(--neutral-800);} }
.login-footer { text-align:center; font-size:14px; color:var(--neutral-500); a{color:var(--primary-main);text-decoration:none;} }
</style>
