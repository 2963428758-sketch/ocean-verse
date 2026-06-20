<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo" @click="router.push('/')">
        <span class="logo-icon">🌊</span>
        <span v-if="!isCollapse" class="logo-text">OceanVerse</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="var(--sidebar-text)"
        active-text-color="var(--sidebar-text-active)"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧内容 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="topbar">
        <div class="topbar-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
            <el-icon class="notification-bell" @click="router.push('/community/notifications')">
              <Bell />
            </el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.avatarUrl || undefined">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['AiRecognize']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'
import { getUnreadCount } from '@/api/community'
import { Bell } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const unreadCount = ref(0)
let pollTimer: ReturnType<typeof setInterval> | null = null
let ws: WebSocket | null = null
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null
let wsReconnectDelay = 1000

async function fetchUnreadCount() {
  try {
    const res: any = await getUnreadCount()
    unreadCount.value = res.data ?? res ?? 0
  } catch {}
}

function connectWebSocket() {
  if (!userStore.isLoggedIn || !userStore.userId) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const url = `${protocol}//${window.location.host}/ws/notification/${userStore.userId}`

  try {
    ws = new WebSocket(url)

    ws.onopen = () => {
      wsReconnectDelay = 1000 // 连接成功后重置重连间隔
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (typeof data.unreadCount === 'number') {
          unreadCount.value = data.unreadCount
        } else {
          fetchUnreadCount() // 兜底：消息格式不含未读数时走 HTTP
        }
      } catch {
        fetchUnreadCount()
      }
    }

    ws.onclose = () => {
      ws = null
      // 指数退避重连，最大 30 秒
      if (userStore.isLoggedIn) {
        wsReconnectTimer = setTimeout(() => {
          connectWebSocket()
        }, wsReconnectDelay)
        wsReconnectDelay = Math.min(wsReconnectDelay * 2, 30000)
      }
    }

    ws.onerror = () => {
      ws?.close()
    }
  } catch {
    // WebSocket 创建失败，降级为纯轮询
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30000) // 轮询作为兜底
    connectWebSocket()
    window.addEventListener('notification-changed', fetchUnreadCount)
    document.addEventListener('visibilitychange', onVisibilityChange)
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  ws?.close()
  window.removeEventListener('notification-changed', fetchUnreadCount)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    fetchUnreadCount()
  }
}

const menuItems = computed(() => {
  const items = [
    { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
    { path: '/species', title: '物种百科', icon: 'Collection', children: [
      { path: '/species/list', title: '物种列表', icon: 'Collection' }
    ]},
    { path: '/observation', title: '观测记录', icon: 'Compass', children: [
      { path: '/observation/list', title: '观测列表', icon: 'Notebook' }
    ]},
    { path: '/visualization', title: '数据可视化', icon: 'DataAnalysis', children: [
      { path: '/visualization/statistics', title: '数据统计', icon: 'TrendCharts' },
      { path: '/visualization/map', title: '分布地图', icon: 'MapLocation' }
    ]},
    { path: '/ai', title: 'AI 服务', icon: 'MagicStick', children: [
      { path: '/ai/recognize', title: '图像识别', icon: 'Camera' },
      { path: '/ai/chat', title: '智能问答', icon: 'ChatLineSquare' }
    ]},
  { path: '/community', title: '社区', icon: 'ChatDotRound', children: [
    { path: '/community/feed', title: '动态广场', icon: 'Postcard' },
    { path: '/community/favorites', title: '我的收藏', icon: 'Star' },
    { path: '/community/liked', title: '点赞记录', icon: 'CircleCheck' },
    ...(userStore.role === 'SUPER_ADMIN' || userStore.role === 'ADMIN'
      ? [{ path: '/community/approval', title: '帖子审核', icon: 'Stamp' }]
      : [])
  ]},
  ]

  // 仅管理员可见系统管理菜单
  if (userStore.role === 'SUPER_ADMIN' || userStore.role === 'ADMIN') {
    items.push({
      path: '/admin', title: '系统管理', icon: 'Setting', children: [
        { path: '/admin/users', title: '用户管理', icon: 'User' },
        { path: '/admin/roles', title: '角色管理', icon: 'UserFilled' },
        { path: '/admin/login-log', title: '登录日志', icon: 'Document' },
        { path: '/admin/operation-log', title: '操作日志', icon: 'Tickets' }
      ]
    })
  }

  return items
})

const breadcrumbs = computed(() => {
  return route.matched.filter(r => r.meta?.title).map(r => r.meta.title as string)
})

function handleCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') doLogout()
}

async function doLogout() {
  try { await logoutApi() } catch {}
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.main-layout { height: 100vh; }

/* ── 侧边栏 ── */
.sidebar {
  background: linear-gradient(180deg, #1b5a80 0%, #2980b9 50%, #3da8d8 100%);
  transition: width 0.3s var(--ease-out);
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  .logo-icon { font-size: 26px; }
  .logo-text {
    color: var(--sidebar-text-active);
    font-size: 17px;
    font-weight: 600;
    white-space: nowrap;
    letter-spacing: -0.02em;
  }
}

.sidebar-menu {
  border-right: none;

  :deep(.el-menu-item) {
    margin: 2px 8px;
    border-radius: var(--radius-sm);
    height: 42px;
    line-height: 42px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;
  }

  :deep(.el-menu-item.is-active) {
    background: rgba(255, 255, 255, 0.18) !important;
    font-weight: 500;
  }

  :deep(.el-menu-item:hover) {
    background: rgba(255, 255, 255, 0.10) !important;
  }

  :deep(.el-sub-menu__title) {
    margin: 2px 8px;
    border-radius: var(--radius-sm);
    height: 42px;
    line-height: 42px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.10) !important;
    }
  }
}

/* ── 顶栏 ── */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--neutral-100);
  padding: 0 24px;
  height: 56px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;

  .collapse-btn {
    font-size: 18px;
    cursor: pointer;
    color: var(--neutral-400);
    transition: color 0.15s;

    &:hover {
      color: var(--neutral-700);
    }
  }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;

  .notification-badge {
    display: inline-flex;
    align-items: center;
    :deep(.el-badge__content) {
      top: 6px;
      right: 14px;
    }
  }

  .notification-bell {
    font-size: 20px;
    cursor: pointer;
    color: var(--neutral-500);
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-sm);
    transition: all 0.2s;
    vertical-align: middle;

    &:hover {
      color: var(--primary-main);
      background: var(--primary-soft);
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: var(--radius-sm);
    transition: background 0.15s;

    &:hover {
      background: var(--neutral-75);
    }
  }

  .username {
    font-size: 13px;
    color: var(--neutral-600);
    font-weight: 500;
  }
}

/* ── 主内容 ── */
.main-content {
  background: var(--neutral-50);
  padding: 28px 32px;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .main-content {
    padding: 20px 16px;
  }
  .topbar {
    padding: 0 16px;
    height: 52px;
  }
  .topbar-left {
    gap: 10px;
  }
  .username {
    display: none;
  }
}
</style>
