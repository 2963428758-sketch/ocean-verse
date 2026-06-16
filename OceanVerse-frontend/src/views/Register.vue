<template>
  <div class="auth-page">
    <!-- 左侧：品牌 + 表单 -->
    <div class="auth-left">
      <div class="auth-content">
        <div class="brand">
          <span class="brand-icon">🌊</span>
          <span class="brand-name">OceanVerse</span>
        </div>

        <h1 class="auth-title">创建你的账号</h1>
        <p class="auth-subtitle">加入 OceanVerse，探索海洋生物多样性</p>

        <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" @submit.prevent="handleRegister">
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
              class="auth-btn"
              size="large"
              :loading="loading"
              @click="handleRegister"
            >
              注 册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          已有账号？<router-link to="/login">返回登录</router-link>
        </div>
      </div>
    </div>

    <!-- 右侧：沙滩海浪场景 -->
    <div class="auth-right">
      <div
        class="ocean-panel"
        @mousemove="onPanelMouseMove"
        @mouseleave="onPanelMouseLeave"
        @mousedown="onPanelMouseDown"
      >
        <canvas ref="rippleCanvas" class="ripple-canvas"></canvas>
        <div class="panel-overlay">
          <span class="overlay-tagline">Protect Our Oceans</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
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

/* ========== 沙滩海浪场景渲染系统 ========== */
const rippleCanvas = ref<HTMLCanvasElement>()
let ctx: CanvasRenderingContext2D | null = null
let animId = 0
let time = 0
const mouse = reactive({ x: -999, y: -999, inside: false })

interface WaveRipple { x: number; y: number; radius: number; amplitude: number; birth: number }
interface Foam { x: number; y: number; size: number; life: number; maxLife: number; vx: number; vy: number }

const waveRipples: WaveRipple[] = []
const foamParticles: Foam[] = []

const waveLayers = [
  { baseY: 0.32, amp: 10, freq: 0.005, speed: 0.35, color: '8,56,92',   alpha: 0.45 },
  { baseY: 0.38, amp: 14, freq: 0.007, speed: 0.50, color: '12,64,108',  alpha: 0.55 },
  { baseY: 0.44, amp: 20, freq: 0.006, speed: 0.70, color: '18,88,140',  alpha: 0.65 },
  { baseY: 0.52, amp: 28, freq: 0.008, speed: 0.95, color: '24,110,170', alpha: 0.80 },
  { baseY: 0.60, amp: 35, freq: 0.010, speed: 1.20, color: '40,140,190', alpha: 0.95 },
]

const WATER_LINE = 0.66

function getWaveY(x: number, baseY: number, layerIdx: number): number {
  const t = time
  let y = baseY
  y += Math.sin(x * 0.006 + t * 0.35 + layerIdx * 1.1) * 14
  y += Math.sin(x * 0.011 + t * 0.6 + layerIdx * 0.8) * 9
  y += Math.sin(x * 0.019 + t * 0.9 + layerIdx * 1.5) * 5
  y += Math.sin(x * 0.003 + t * 0.18) * 22
  if (mouse.inside) {
    const dx = x - mouse.x
    const dist = Math.abs(dx)
    const influence = Math.max(0, 1 - dist / 220)
    const dy = mouse.y - baseY
    if (dy > -120 && dy < 120) {
      y += influence * influence * 50 * Math.sign(dy)
      y += Math.sin(dist * 0.04 + t * 3.5) * influence * 18
    }
  }
  for (const r of waveRipples) {
    const dx = x - r.x
    const dist = Math.abs(dx)
    if (dist < r.radius + 100 && dist > Math.max(0, r.radius - 100)) {
      const d = Math.abs(dist - r.radius)
      const env = Math.max(0, 1 - d / 80)
      y += Math.sin(d * 0.07) * env * r.amplitude
    }
  }
  return y
}

function draw() {
  if (!rippleCanvas.value) return
  if (!ctx) {
    ctx = rippleCanvas.value.getContext('2d')
    if (!ctx) return
  }
  const c = rippleCanvas.value
  const p = c.parentElement
  if (p) {
    const r = p.getBoundingClientRect()
    if (r.width > 0 && r.height > 0 && (c.width !== Math.round(r.width) || c.height !== Math.round(r.height))) {
      c.width = Math.round(r.width)
      c.height = Math.round(r.height)
    }
  }
  const w = c.width, h = c.height
  if (w === 0 || h === 0) { animId = requestAnimationFrame(draw); return }
  const waterY = h * WATER_LINE
  ctx.clearRect(0, 0, w, h)

  const sky = ctx.createLinearGradient(0, 0, 0, waterY)
  sky.addColorStop(0, '#071e33')
  sky.addColorStop(0.3, '#0a3555')
  sky.addColorStop(0.65, '#156b9c')
  sky.addColorStop(1, '#4fb8d8')
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, w, waterY + 10)

  ctx.fillStyle = 'rgba(255,255,255,0.4)'
  const starSeed = [0.12, 0.28, 0.45, 0.63, 0.78, 0.91]
  for (let i = 0; i < starSeed.length; i++) {
    const sx = w * starSeed[i]
    const sy = h * 0.04 + (i % 3) * h * 0.06
    const ss = 1 + (i % 2)
    const blink = 0.3 + 0.7 * Math.abs(Math.sin(time * 0.3 + i * 2))
    ctx.globalAlpha = blink * 0.5
    ctx.beginPath()
    ctx.arc(sx, sy, ss, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1

  for (let i = 0; i < waveLayers.length; i++) {
    const L = waveLayers[i]
    const by = h * L.baseY
    ctx.beginPath()
    ctx.moveTo(-1, h)
    for (let x = -1; x <= w + 1; x += 2) ctx.lineTo(x, getWaveY(x, by, i))
    ctx.lineTo(w + 1, h)
    ctx.closePath()
    const wg = ctx.createLinearGradient(0, by - 30, 0, waterY + 30)
    wg.addColorStop(0, `rgba(${L.color},${L.alpha})`)
    wg.addColorStop(1, `rgba(${L.color},${Math.min(1, L.alpha + 0.15)})`)
    ctx.fillStyle = wg
    ctx.fill()
    ctx.beginPath()
    for (let x = 0; x <= w; x += 2) {
      const y = getWaveY(x, by, i)
      if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
    }
    ctx.strokeStyle = `rgba(255,255,255,${0.06 + i * 0.05})`
    ctx.lineWidth = 1.8
    ctx.stroke()
    if (i === waveLayers.length - 1) {
      for (let x = 0; x < w; x += 40) {
        const y = getWaveY(x, by, i)
        const peak = (Math.sin(x * 0.01 + time * 1.2 + i) + 1) * 0.5
        if (peak > 0.82 && Math.random() < 0.04) {
          foamParticles.push({ x, y, size: 1.5 + Math.random() * 3, life: 0, maxLife: 30 + Math.random() * 40, vx: (Math.random() - 0.5) * 0.8, vy: 0.1 + Math.random() * 0.4 })
        }
      }
    }
  }

  ctx.beginPath()
  for (let x = 0; x <= w; x += 2) {
    const y = waterY + Math.sin(x * 0.015 + time * 1.8) * 4 + Math.sin(x * 0.04 + time * 3) * 2
    if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
  }
  ctx.strokeStyle = 'rgba(255,255,255,0.55)'
  ctx.lineWidth = 3
  ctx.stroke()
  ctx.strokeStyle = 'rgba(255,255,255,0.2)'
  ctx.lineWidth = 8
  ctx.stroke()

  const sand = ctx.createLinearGradient(0, waterY - 5, 0, h)
  sand.addColorStop(0, '#c9ab7a')
  sand.addColorStop(0.08, '#d4b68a')
  sand.addColorStop(0.3, '#dcc49e')
  sand.addColorStop(0.6, '#e3ccac')
  sand.addColorStop(1, '#ead6b8')
  ctx.fillStyle = sand
  ctx.fillRect(0, waterY, w, h - waterY)
  const wet = ctx.createLinearGradient(0, waterY, 0, waterY + h * 0.08)
  wet.addColorStop(0, 'rgba(90,68,42,0.35)')
  wet.addColorStop(1, 'rgba(90,68,42,0)')
  ctx.fillStyle = wet
  ctx.fillRect(0, waterY, w, h * 0.08)
  ctx.fillStyle = 'rgba(120,95,60,0.06)'
  for (let i = 0; i < 300; i++) {
    ctx.fillRect(Math.random() * w, waterY + 6 + Math.random() * (h - waterY - 10), 1, 1)
  }

  for (let i = waveRipples.length - 1; i >= 0; i--) {
    const r = waveRipples[i]
    const fade = Math.max(0, 1 - (time - r.birth) / 5)
    if (fade <= 0) { waveRipples.splice(i, 1); continue }
    if (r.y < waterY) {
      ctx.beginPath()
      ctx.arc(r.x, r.y, r.radius, 0, Math.PI * 2)
      ctx.strokeStyle = `rgba(180,230,255,${fade * 0.25})`
      ctx.lineWidth = 1.5 * fade
      ctx.stroke()
    }
  }

  for (let i = foamParticles.length - 1; i >= 0; i--) {
    const f = foamParticles[i]
    f.x += f.vx; f.y += f.vy; f.life++
    const a = 1 - f.life / f.maxLife
    if (a <= 0) { foamParticles.splice(i, 1); continue }
    ctx.beginPath()
    ctx.arc(f.x, f.y, f.size * a, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255,255,255,${a * 0.65})`
    ctx.fill()
  }

  time += 0.018
  animId = requestAnimationFrame(draw)
}

function resizeCanvas() {
  if (!rippleCanvas.value) return
  const p = rippleCanvas.value.parentElement
  if (!p) return
  const r = p.getBoundingClientRect()
  rippleCanvas.value.width = r.width
  rippleCanvas.value.height = r.height
  if (!ctx) {
    ctx = rippleCanvas.value.getContext('2d')
  }
}

let lastSpawn = 0
function onPanelMouseMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  mouse.x = e.clientX - rect.left; mouse.y = e.clientY - rect.top; mouse.inside = true
  const now = performance.now()
  if (now - lastSpawn > 120) {
    waveRipples.push({ x: mouse.x, y: mouse.y, radius: 3, amplitude: 14, birth: time })
    lastSpawn = now
  }
}
function onPanelMouseLeave() { mouse.inside = false }
function onPanelMouseDown(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const x = e.clientX - rect.left, y = e.clientY - rect.top
  waveRipples.push({ x, y, radius: 3, amplitude: 40, birth: time })
  for (let i = 0; i < 10; i++) {
    foamParticles.push({ x, y, size: 2 + Math.random() * 4, life: 0, maxLife: 25 + Math.random() * 35, vx: (Math.random() - 0.5) * 4, vy: -Math.random() * 3 - 1 })
  }
}

onMounted(async () => { await nextTick(); resizeCanvas(); window.addEventListener('resize', resizeCanvas); draw(); setTimeout(resizeCanvas, 100); setTimeout(resizeCanvas, 500) })
onBeforeUnmount(() => { cancelAnimationFrame(animId); window.removeEventListener('resize', resizeCanvas) })
</script>

<style scoped lang="scss">
.auth-page {
  display: flex;
  min-height: 100vh;
  background: #fff;
}

/* ===== 左侧面板 ===== */
.auth-left {
  flex: 0 0 44%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.auth-content {
  width: 100%;
  max-width: 380px;
  animation: fadeSlideUp 0.6s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 40px;

  .brand-icon { font-size: 32px; }
  .brand-name {
    font-size: 18px;
    font-weight: 700;
    color: var(--neutral-800);
    letter-spacing: -0.3px;
  }
}

.auth-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--neutral-800);
  margin: 0 0 8px;
  letter-spacing: -0.3px;
}

.auth-subtitle {
  font-size: 15px;
  color: var(--neutral-500);
  margin: 0 0 36px;
}

.auth-form {
  :deep(.el-input__wrapper) {
    border-radius: var(--radius-md);
    box-shadow: 0 0 0 1px var(--neutral-200);
    transition: box-shadow var(--transition-fast);
    &:hover { box-shadow: 0 0 0 1px var(--neutral-400); }
    &.is-focus { box-shadow: 0 0 0 2px var(--primary-lighter); }
  }

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
}

.auth-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  background: var(--primary-main) !important;
  border: none !important;
  letter-spacing: 2px;
  transition: all var(--transition-smooth) !important;

  &:hover {
    background: var(--primary-light) !important;
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(15, 76, 117, 0.3);
  }

  &:active { transform: translateY(0); }
}

.auth-footer {
  text-align: center;
  font-size: 13px;
  color: var(--neutral-400);
  margin-top: 8px;

  a {
    color: var(--primary-main);
    text-decoration: none;
    font-weight: 500;
    &:hover { text-decoration: underline; }
  }
}

/* ===== 右侧视觉面板 ===== */
.auth-right {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.ocean-panel {
  position: absolute;
  inset: 24px 24px 24px 0;
  border-radius: var(--radius-xl) 0 0 var(--radius-xl);
  overflow: hidden;
  background: #071e33;
}

.ripple-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}

.panel-overlay {
  position: absolute;
  top: 28px;
  left: 36px;
  z-index: 5;
  pointer-events: none;
}

.overlay-tagline {
  font-size: 16px;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .auth-page { flex-direction: column; }

  .auth-left {
    flex: none;
    padding: 32px 24px;
  }

  .auth-right {
    flex: none;
    min-height: 280px;
  }

  .ocean-panel {
    inset: 0;
    border-radius: 0;
  }
}
</style>
