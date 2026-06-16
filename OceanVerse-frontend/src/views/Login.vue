<template>
  <div class="auth-page">
    <!-- 左侧：品牌 + 表单 -->
    <div class="auth-left">
      <div class="auth-content">
        <div class="brand">
          <span class="brand-icon">🌊</span>
          <span class="brand-name">OceanVerse</span>
        </div>

        <h1 class="auth-title">欢迎使用 OceanVerse</h1>
        <p class="auth-subtitle">智慧海洋物种观测与管理平台</p>
        <p class="auth-desc">
          汇聚海洋生物多样性数据，集物种检索、分布可视化、AI 识别与社区协作于一体，助力海洋生态保护与研究。
        </p>

        <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" @submit.prevent="handleLogin">
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
          <el-form-item>
            <el-button
              class="auth-btn"
              size="large"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          还没有账号？<router-link to="/register">立即注册</router-link>
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
          <span class="overlay-tagline">Explore the Ocean</span>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

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
    userStore.setLoginInfo(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
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

// 波浪层配置（从远到近，越来越不透明、振幅越来越大）
const waveLayers = [
  { baseY: 0.20, amp: 12, freq: 0.004, speed: 0.30, color: '2,28,55',    alpha: 0.55 },
  { baseY: 0.27, amp: 18, freq: 0.006, speed: 0.45, color: '4,40,75',    alpha: 0.65 },
  { baseY: 0.35, amp: 26, freq: 0.005, speed: 0.65, color: '8,55,100',   alpha: 0.75 },
  { baseY: 0.44, amp: 35, freq: 0.007, speed: 0.90, color: '12,72,125',  alpha: 0.88 },
  { baseY: 0.54, amp: 45, freq: 0.009, speed: 1.15, color: '18,95,155',  alpha: 0.97 },
]

const WATER_LINE = 0.667   // 海水分界线（面板约 2/3 处），上方 2/3 海浪，下方 1/3 沙滩

function hexRgb(hex: string) {
  const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return m ? `${parseInt(m[1],16)},${parseInt(m[2],16)},${parseInt(m[3],16)}` : '255,255,255'
}

function getWaveY(x: number, baseY: number, layerIdx: number): number {
  const t = time
  let y = baseY
  // 自然海浪 = 多个正弦波叠加（增大振幅使波浪更明显）
  y += Math.sin(x * 0.005 + t * 0.30 + layerIdx * 1.1) * 18
  y += Math.sin(x * 0.010 + t * 0.55 + layerIdx * 0.8) * 12
  y += Math.sin(x * 0.018 + t * 0.85 + layerIdx * 1.5) * 7
  y += Math.sin(x * 0.003 + t * 0.15) * 25

  // 鼠标掀浪（增强影响范围和幅度）
  if (mouse.inside) {
    const dx = x - mouse.x
    const dist = Math.abs(dx)
    const influence = Math.max(0, 1 - dist / 280)
    const dy = mouse.y - baseY
    if (dy > -160 && dy < 160) {
      y += influence * influence * 70 * Math.sign(dy)
      y += Math.sin(dist * 0.035 + t * 3.5) * influence * 25
    }
  }

  // 扩散涟漪
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

// ─── 沙滩上的海洋小动物绘制 ───
function drawSeaCreatures(c: CanvasRenderingContext2D, w: number, h: number, waterY: number, t: number) {
  const sandTop = waterY + 8
  const sandH = h - waterY

  // ── 海星 ──
  const starX = w * 0.25
  const starY = sandTop + sandH * 0.35
  const starSize = Math.min(w, sandH) * 0.07
  c.save()
  c.translate(starX, starY)
  c.rotate(0.2 + Math.sin(t * 0.15) * 0.05)
  c.beginPath()
  for (let i = 0; i < 5; i++) {
    const angle = (i * Math.PI * 2) / 5 - Math.PI / 2
    const innerAngle = angle + Math.PI / 5
    c.lineTo(Math.cos(angle) * starSize, Math.sin(angle) * starSize)
    c.lineTo(Math.cos(innerAngle) * starSize * 0.42, Math.sin(innerAngle) * starSize * 0.42)
  }
  c.closePath()
  c.fillStyle = '#e8785a'
  c.fill()
  c.strokeStyle = '#d4623f'
  c.lineWidth = 1.2
  c.stroke()
  // 海星中心小圆点
  c.beginPath()
  c.arc(0, 0, starSize * 0.12, 0, Math.PI * 2)
  c.fillStyle = '#d4623f'
  c.fill()
  c.restore()

  // ── 贝壳 ──
  const shellX = w * 0.6
  const shellY = sandTop + sandH * 0.5
  const shellW = Math.min(w, sandH) * 0.06
  const shellH = shellW * 0.75
  c.save()
  c.translate(shellX, shellY)
  c.rotate(-0.15)
  // 贝壳主体
  c.beginPath()
  c.ellipse(0, 0, shellW, shellH, 0, Math.PI, 0)
  c.closePath()
  c.fillStyle = '#f0dcc0'
  c.fill()
  c.strokeStyle = '#c9a87c'
  c.lineWidth = 1.2
  c.stroke()
  // 贝壳纹路
  for (let i = -3; i <= 3; i++) {
    c.beginPath()
    const rx = (i / 4) * shellW * 0.8
    c.moveTo(rx, 0)
    c.quadraticCurveTo(rx * 0.3, -shellH * 0.9, 0, -shellH)
    c.strokeStyle = 'rgba(180,140,100,0.35)'
    c.lineWidth = 0.8
    c.stroke()
  }
  c.restore()

  // ── 小螃蟹 ──
  const crabX = w * 0.78 + Math.sin(t * 0.6) * 12
  const crabY = sandTop + sandH * 0.28
  const crabSize = Math.min(w, sandH) * 0.045
  c.save()
  c.translate(crabX, crabY)
  // 螃蟹身体
  c.beginPath()
  c.ellipse(0, 0, crabSize * 1.3, crabSize, 0, 0, Math.PI * 2)
  c.fillStyle = '#e05540'
  c.fill()
  c.strokeStyle = '#c43e2a'
  c.lineWidth = 1
  c.stroke()
  // 眼睛
  const eyeOff = crabSize * 0.5
  for (const side of [-1, 1]) {
    c.beginPath()
    c.moveTo(side * eyeOff, -crabSize * 0.6)
    c.lineTo(side * eyeOff * 1.2, -crabSize * 1.4)
    c.strokeStyle = '#c43e2a'
    c.lineWidth = 1.5
    c.stroke()
    c.beginPath()
    c.arc(side * eyeOff * 1.2, -crabSize * 1.4, crabSize * 0.22, 0, Math.PI * 2)
    c.fillStyle = '#222'
    c.fill()
  }
  // 钳子
  const clawSwing = Math.sin(t * 2) * 0.15
  for (const side of [-1, 1]) {
    c.save()
    c.translate(side * crabSize * 1.3, -crabSize * 0.1)
    c.rotate(side * (0.5 + clawSwing))
    c.beginPath()
    c.moveTo(0, 0)
    c.lineTo(side * crabSize * 1.1, -crabSize * 0.5)
    c.strokeStyle = '#c43e2a'
    c.lineWidth = 2
    c.stroke()
    // 钳子头部
    c.beginPath()
    c.arc(side * crabSize * 1.1, -crabSize * 0.5, crabSize * 0.35, 0, Math.PI * 2)
    c.fillStyle = '#e05540'
    c.fill()
    c.stroke()
    c.restore()
  }
  // 小腿
  for (const side of [-1, 1]) {
    for (let i = 0; i < 3; i++) {
      const legX = side * (crabSize * 0.4 + i * crabSize * 0.35)
      const legAngle = side * (0.3 + i * 0.25) + Math.sin(t * 3 + i) * 0.1
      c.save()
      c.translate(legX, crabSize * 0.5)
      c.rotate(legAngle)
      c.beginPath()
      c.moveTo(0, 0)
      c.lineTo(0, crabSize * 0.9)
      c.strokeStyle = '#c43e2a'
      c.lineWidth = 1.2
      c.stroke()
      c.restore()
    }
  }
  c.restore()

  // ── 小海螺 ──
  const snailX = w * 0.42
  const snailY = sandTop + sandH * 0.6
  const snailSize = Math.min(w, sandH) * 0.035
  c.save()
  c.translate(snailX, snailY)
  c.rotate(0.3)
  // 螺旋壳
  c.beginPath()
  c.moveTo(0, 0)
  for (let a = 0; a < Math.PI * 4; a += 0.15) {
    const r = snailSize * (1 - a / (Math.PI * 5))
    c.lineTo(Math.cos(a) * r, Math.sin(a) * r * 0.7)
  }
  c.fillStyle = '#d4a574'
  c.fill()
  c.strokeStyle = '#b8895a'
  c.lineWidth = 1
  c.stroke()
  c.restore()

  // ── 海龟（在沙滩靠近水面的位置）──
  const turtleX = w * 0.13 + Math.sin(t * 0.25) * 8
  const turtleY = sandTop + sandH * 0.15
  const ts = Math.min(w, sandH) * 0.065
  c.save()
  c.translate(turtleX, turtleY)
  c.rotate(-0.1 + Math.sin(t * 0.3) * 0.04)
  // 龟壳
  c.beginPath()
  c.ellipse(0, 0, ts, ts * 0.7, 0, 0, Math.PI * 2)
  c.fillStyle = '#4a8c5c'
  c.fill()
  c.strokeStyle = '#357045'
  c.lineWidth = 1.5
  c.stroke()
  // 龟壳纹路 — 六边形图案
  c.beginPath()
  c.ellipse(0, 0, ts * 0.55, ts * 0.38, 0, 0, Math.PI * 2)
  c.strokeStyle = 'rgba(53,112,69,0.5)'
  c.lineWidth = 1
  c.stroke()
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2
    c.beginPath()
    c.moveTo(Math.cos(a) * ts * 0.55, Math.sin(a) * ts * 0.38)
    c.lineTo(Math.cos(a) * ts, Math.sin(a) * ts * 0.7)
    c.strokeStyle = 'rgba(53,112,69,0.4)'
    c.lineWidth = 0.8
    c.stroke()
  }
  // 头
  c.beginPath()
  c.ellipse(ts * 1.05, -ts * 0.05, ts * 0.3, ts * 0.22, 0, 0, Math.PI * 2)
  c.fillStyle = '#5a9c6c'
  c.fill()
  c.strokeStyle = '#357045'
  c.lineWidth = 1
  c.stroke()
  // 眼睛
  c.beginPath()
  c.arc(ts * 1.18, -ts * 0.14, ts * 0.06, 0, Math.PI * 2)
  c.fillStyle = '#222'
  c.fill()
  // 四肢（小鳍）
  const legSwing = Math.sin(t * 1.5) * 0.12
  for (const [sx, sy, rot] of [[0.6, -0.55, -0.6], [0.6, 0.55, 0.6], [-0.5, -0.5, -0.3], [-0.5, 0.5, 0.3]] as [number, number, number][]) {
    c.save()
    c.translate(ts * sx, ts * sy)
    c.rotate(rot + legSwing * (sy > 0 ? -1 : 1))
    c.beginPath()
    c.ellipse(ts * 0.2, 0, ts * 0.32, ts * 0.12, 0.3, 0, Math.PI * 2)
    c.fillStyle = '#5a9c6c'
    c.fill()
    c.restore()
  }
  c.restore()

  // ── 水母（在水面区域缓慢漂浮）──
  const jellyX = w * 0.5 + Math.sin(t * 0.2) * 30
  const jellyY = waterY * 0.35 + Math.sin(t * 0.35) * 15
  const js = Math.min(w, h) * 0.04
  c.save()
  c.translate(jellyX, jellyY)
  c.globalAlpha = 0.45
  // 水母头部（半球）
  c.beginPath()
  c.arc(0, 0, js, Math.PI, 0)
  c.quadraticCurveTo(js, js * 0.3, 0, js * 0.2)
  c.quadraticCurveTo(-js, js * 0.3, -js, 0)
  c.fillStyle = 'rgba(180,140,220,0.6)'
  c.fill()
  c.strokeStyle = 'rgba(160,120,200,0.5)'
  c.lineWidth = 1
  c.stroke()
  // 触须
  for (let i = -3; i <= 3; i++) {
    c.beginPath()
    const startX = i * js * 0.25
    c.moveTo(startX, js * 0.15)
    const wave1 = Math.sin(t * 1.2 + i * 0.8) * 6
    const wave2 = Math.sin(t * 0.9 + i * 1.2) * 4
    c.bezierCurveTo(
      startX + wave1, js * 0.15 + js * 0.6,
      startX + wave2, js * 0.15 + js * 1.2,
      startX + wave1 * 0.5 + wave2 * 0.5, js * 0.15 + js * 1.8
    )
    c.strokeStyle = `rgba(180,140,220,${0.3 + Math.abs(i) * 0.05})`
    c.lineWidth = 1
    c.stroke()
  }
  c.globalAlpha = 1
  c.restore()
}

function draw() {
  if (!rippleCanvas.value) return
  // 延迟初始化 ctx
  if (!ctx) {
    ctx = rippleCanvas.value.getContext('2d')
    if (!ctx) return
  }
  const c = rippleCanvas.value
  // 自适应尺寸（前几秒每帧检测，防止初始布局未完成）
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

  // ─── 天空 ───
  const sky = ctx.createLinearGradient(0, 0, 0, waterY)
  sky.addColorStop(0, '#020e1a')
  sky.addColorStop(0.25, '#041e38')
  sky.addColorStop(0.55, '#0a3d60')
  sky.addColorStop(0.80, '#156a8a')
  sky.addColorStop(1, '#2a8aaa')
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, w, waterY + 10)

  // 天空中的薄星光
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

  // ─── 海浪层 ───
  for (let i = 0; i < waveLayers.length; i++) {
    const L = waveLayers[i]
    const by = h * L.baseY
    ctx.beginPath()
    ctx.moveTo(-1, h)

    for (let x = -1; x <= w + 1; x += 2) {
      ctx.lineTo(x, getWaveY(x, by, i))
    }
    ctx.lineTo(w + 1, h)
    ctx.closePath()

    const wg = ctx.createLinearGradient(0, by - 30, 0, waterY + 30)
    wg.addColorStop(0, `rgba(${L.color},${L.alpha})`)
    wg.addColorStop(1, `rgba(${L.color},${Math.min(1, L.alpha + 0.15)})`)
    ctx.fillStyle = wg
    ctx.fill()

    // 浪尖白色泡沫线
    ctx.beginPath()
    for (let x = 0; x <= w; x += 2) {
      const y = getWaveY(x, by, i)
      if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
    }
    ctx.strokeStyle = `rgba(255,255,255,${0.12 + i * 0.08})`
    ctx.lineWidth = 2.2
    ctx.stroke()

    // 最前面一层额外生成泡沫粒子
    if (i === waveLayers.length - 1) {
      for (let x = 0; x < w; x += 40) {
        const y = getWaveY(x, by, i)
        const peak = (Math.sin(x * 0.01 + time * 1.2 + i) + 1) * 0.5
        if (peak > 0.82 && Math.random() < 0.04) {
          foamParticles.push({
            x, y,
            size: 1.5 + Math.random() * 3,
            life: 0, maxLife: 30 + Math.random() * 40,
            vx: (Math.random() - 0.5) * 0.8,
            vy: 0.1 + Math.random() * 0.4
          })
        }
      }
    }
  }

  // ─── 海岸过渡带（柔和渐变，取代硬分界线）───
  const shoreH = h * 0.10
  const shore = ctx.createLinearGradient(0, waterY - shoreH * 0.5, 0, waterY + shoreH * 0.5)
  shore.addColorStop(0, 'rgba(38,145,195,0.25)')
  shore.addColorStop(0.4, 'rgba(80,170,200,0.12)')
  shore.addColorStop(1, 'rgba(184,154,106,0)')
  ctx.fillStyle = shore
  ctx.fillRect(0, waterY - shoreH * 0.5, w, shoreH)

  // 轻柔的海岸泡沫线（淡化）
  ctx.beginPath()
  for (let x = 0; x <= w; x += 2) {
    const y = waterY + Math.sin(x * 0.012 + time * 1.5) * 4 + Math.sin(x * 0.035 + time * 2.8) * 2
    if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
  }
  ctx.strokeStyle = 'rgba(255,255,255,0.22)'
  ctx.lineWidth = 2
  ctx.stroke()
  ctx.strokeStyle = 'rgba(255,255,255,0.08)'
  ctx.lineWidth = 6
  ctx.stroke()

  // ─── 沙滩（下方 1/3）───
  const sand = ctx.createLinearGradient(0, waterY - 5, 0, h)
  sand.addColorStop(0, '#b89a6a')
  sand.addColorStop(0.06, '#c4a87c')
  sand.addColorStop(0.2, '#d2b890')
  sand.addColorStop(0.5, '#dcc8a2')
  sand.addColorStop(1, '#e8d4b4')
  ctx.fillStyle = sand
  ctx.fillRect(0, waterY, w, h - waterY)

  // 湿沙区域（柔和过渡）
  const wet = ctx.createLinearGradient(0, waterY, 0, waterY + h * 0.10)
  wet.addColorStop(0, 'rgba(70,100,120,0.18)')
  wet.addColorStop(0.5, 'rgba(90,68,42,0.10)')
  wet.addColorStop(1, 'rgba(90,68,42,0)')
  ctx.fillStyle = wet
  ctx.fillRect(0, waterY, w, h * 0.10)

  // 沙粒纹理
  ctx.fillStyle = 'rgba(120,95,60,0.06)'
  for (let i = 0; i < 300; i++) {
    const sx = Math.random() * w
    const sy = waterY + 6 + Math.random() * (h - waterY - 10)
    ctx.fillRect(sx, sy, 1, 1)
  }

  // ─── 沙滩上的海洋小动物 ───
  drawSeaCreatures(ctx, w, h, waterY, time)

  // ─── 涟漪圆环（水面上） ───
  for (let i = waveRipples.length - 1; i >= 0; i--) {
    const r = waveRipples[i]
    const age = time - r.birth
    const fade = Math.max(0, 1 - age / 5)
    if (fade <= 0) { waveRipples.splice(i, 1); continue }

    // 只在水面区域画圆环
    if (r.y < waterY) {
      ctx.beginPath()
      ctx.arc(r.x, r.y, r.radius, 0, Math.PI * 2)
      ctx.strokeStyle = `rgba(180,230,255,${fade * 0.25})`
      ctx.lineWidth = 1.5 * fade
      ctx.stroke()
    }
  }

  // ─── 泡沫粒子 ───
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
  // 确保 ctx 已初始化
  if (!ctx) {
    ctx = rippleCanvas.value.getContext('2d')
  }
}

let lastSpawn = 0

function onPanelMouseMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  mouse.x = e.clientX - rect.left
  mouse.y = e.clientY - rect.top
  mouse.inside = true
  const now = performance.now()
  if (now - lastSpawn > 80) {
    waveRipples.push({ x: mouse.x, y: mouse.y, radius: 5, amplitude: 20, birth: time })
    lastSpawn = now
  }
}

function onPanelMouseLeave() {
  mouse.inside = false
}

function onPanelMouseDown(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  waveRipples.push({ x, y, radius: 5, amplitude: 55, birth: time })
  // 点击溅起大量泡沫
  for (let i = 0; i < 18; i++) {
    foamParticles.push({
      x, y,
      size: 2.5 + Math.random() * 5,
      life: 0, maxLife: 30 + Math.random() * 40,
      vx: (Math.random() - 0.5) * 5,
      vy: -Math.random() * 4 - 1.5
    })
  }
}

onMounted(async () => {
  await nextTick()
  console.log('[OceanVerse] 海浪场景初始化中...')
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  draw()
  console.log('[OceanVerse] 海浪动画已启动，canvas 尺寸:', rippleCanvas.value?.width, 'x', rippleCanvas.value?.height)
  // 延迟重试，确保布局完成后尺寸正确
  setTimeout(resizeCanvas, 100)
  setTimeout(resizeCanvas, 500)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resizeCanvas)
})
</script>

<style scoped lang="scss">
.auth-page {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #1b6fa8 0%, #3a9ed6 30%, #5fbde4 55%, #8fd8ee 80%, #c4eef7 100%);
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
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-xl);
  padding: 40px 36px;
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
  margin: 0 0 16px;
}

.auth-desc {
  font-size: 13.5px;
  color: var(--neutral-400);
  line-height: 1.7;
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
    margin-bottom: 20px;
  }
}

.auth-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  color: #fff !important;
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
  border-radius: var(--radius-xl);
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
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 2px;
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
