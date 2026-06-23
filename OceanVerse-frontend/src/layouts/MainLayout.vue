<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '80px' : '220px'" class="sidebar">
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
          <div class="menu-group">
            <!-- 分组标题（仅展开态显示） -->
            <div v-if="item.children && !isCollapse" class="group-header">
              <span>{{ item.title }}</span>
            </div>
            <!-- 无子菜单 -->
            <el-menu-item v-if="!item.children" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span class="menu-item-label">{{ item.title }}</span>
            </el-menu-item>
            <!-- 有子菜单：平铺子项 -->
            <template v-else>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
                <span class="menu-item-label">{{ child.title }}</span>
              </el-menu-item>
            </template>
          </div>
        </template>
      </el-menu>

      <!-- 底部用户模块 -->
      <div class="sidebar-user" :class="{ 'is-collapsed': isCollapse }">
        <!-- 折叠态：手动控制弹出层，避免 el-dropdown 在窄侧边栏中的定位问题 -->
        <el-popover
          v-if="isCollapse"
          :visible="userPopoverVisible"
          placement="right-start"
          :width="130"
          :offset="8"
          :teleported="true"
          trigger="click"
          popper-class="sidebar-user-popover"
        >
          <template #reference>
            <div class="sidebar-user-trigger" @click.stop="userPopoverVisible = !userPopoverVisible">
              <el-avatar :size="32" :src="userStore.avatarUrl || undefined">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
            </div>
          </template>
          <div class="user-popover-menu">
            <div class="user-popover-item" @click="userPopoverVisible = false; router.push('/profile')">个人中心</div>
            <div class="user-popover-divider" />
            <div class="user-popover-item" @click="userPopoverVisible = false; doLogout()">退出登录</div>
          </div>
        </el-popover>
        <!-- 展开态：标准下拉菜单 -->
        <el-dropdown v-else @command="handleCommand" placement="top-end">
          <span class="sidebar-user-trigger">
            <el-avatar :size="36" :src="userStore.avatarUrl || undefined">
              {{ userStore.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <span class="sidebar-user-name">{{ userStore.nickname || userStore.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
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
          <!-- AI 物种识别 -->
          <div class="topbar-icon-group" @click="router.push('/ai/recognize')">
            <el-icon class="topbar-icon"><MagicStick /></el-icon>
            <span class="topbar-icon-label">物种识别</span>
          </div>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
            <el-icon class="notification-bell" @click="router.push('/community/notifications')">
              <Bell />
            </el-icon>
          </el-badge>
          <!-- 系统管理齿轮（仅管理员可见） -->
          <el-dropdown v-if="isAdmin" trigger="click" class="settings-gear-dropdown">
            <span class="settings-gear">
              <el-icon :size="20"><Setting /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/admin/users')">
                  <el-icon><User /></el-icon>用户管理
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/roles')">
                  <el-icon><UserFilled /></el-icon>角色管理
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/login-log')">
                  <el-icon><Document /></el-icon>登录日志
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/operation-log')">
                  <el-icon><Tickets /></el-icon>操作日志
                </el-dropdown-item>
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
        <GlobalChatDrawer />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'
import { getUnreadCount } from '@/api/community'
import { Bell, Setting, User, UserFilled, Document, Tickets, MagicStick } from '@element-plus/icons-vue'
import GlobalChatDrawer from '@/components/GlobalChatDrawer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const unreadCount = ref(0)
const userPopoverVisible = ref(false)
watch(isCollapse, (val) => { if (!val) userPopoverVisible.value = false })

// 点击页面其他区域关闭折叠态 popover
function handleGlobalClick(e: MouseEvent) {
  if (userPopoverVisible.value && isCollapse.value) {
    const target = e.target as HTMLElement
    if (!target.closest('.sidebar-user-popover') && !target.closest('.sidebar-user-trigger')) {
      userPopoverVisible.value = false
    }
  }
}
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
  document.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  ws?.close()
  window.removeEventListener('notification-changed', fetchUnreadCount)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('click', handleGlobalClick)
})

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    fetchUnreadCount()
  }
}

const menuItems = computed(() => {
  const items = [
    { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/community', title: '社区', icon: 'ChatDotRound', children: [
    { path: '/community/feed', title: '动态广场', icon: 'Postcard' },
    { path: '/community/favorites', title: '我的收藏', icon: 'Star' },
    { path: '/community/liked', title: '点赞记录', icon: 'CircleCheck' },
    ...(userStore.role === 'SUPER_ADMIN' || userStore.role === 'ADMIN'
      ? [{ path: '/community/approval', title: '帖子审核', icon: 'Stamp' }]
      : [])
  ]},
    { path: '/species', title: '物种百科', icon: 'Collection', children: [
      { path: '/species/list', title: '物种列表', icon: 'Collection' }
    ]},
    { path: '/observation', title: '观测记录', icon: 'Compass', children: [
      { path: '/observation/list', title: '观测列表', icon: 'Notebook' }
    ]},
    { path: '/visualization', title: '数据可视化', icon: 'DataAnalysis', children: [
      { path: '/visualization/statistics', title: '数据统计', icon: 'TrendCharts' },
      { path: '/visualization/map', title: '分布地图', icon: 'MapLocation' },
      { path: '/visualization/observation-map', title: '观测地图', icon: 'LocationFilled' },
      { path: '/visualization/export', title: '数据导出', icon: 'Download' }
    ]},
  ]

  return items
})

const isAdmin = computed(() =>
  userStore.role === 'SUPER_ADMIN' || userStore.role === 'ADMIN'
)

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
  overflow: hidden;
  border-right: none;
  display: flex;
  flex-direction: column;
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

/* 折叠态：logo 图标居中 */
.sidebar.is-collapsed .logo {
  padding: 0 !important;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;

  :deep(.el-menu-item) {
    margin: 2px 8px;
    border-radius: var(--radius-sm);
    height: 42px;
    line-height: 42px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;

    .el-icon { font-size: 19px; }
  }

  /* 展开态：标签正常内联显示 */
  :deep(.menu-item-label) {
    margin-left: 8px;
    font-size: 14px;
  }

  /* 分组标题 */
  .group-header {
    padding: 18px 20px 6px;
    font-size: 11px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.4);
    letter-spacing: 0.06em;
    text-transform: uppercase;
    user-select: none;
  }

  /* 收起态：图标居中 + 中文标签 */
  &.el-menu--collapse {
    /* 菜单容器撑满侧边栏宽度，去掉内边距 */
    width: 100% !important;
    padding: 0 !important;

    /* 收起态隐藏分组标题 */
    .group-header {
      display: none;
    }

    /* 分组之间加横线分隔 */
    .menu-group + .menu-group {
      border-top: 1px solid rgba(255, 255, 255, 0.15);
      margin-top: 4px;
      padding-top: 4px;
    }

    /* 菜单项：grid 布局保证精确居中 */
    :deep(.el-menu-item) {
      display: grid !important;
      grid-template-rows: auto auto !important;
      justify-items: center !important;
      align-items: center !important;
      justify-content: center !important;
      width: 100% !important;
      padding: 0 !important;
      margin: 0 !important;
      height: auto !important;
      min-height: 56px !important;
      line-height: normal !important;
      text-align: center !important;
      overflow: visible !important;
      box-sizing: border-box !important;
    }

    /* 图标放大、去掉默认 margin */
    :deep(.el-menu-item .el-icon) {
      margin: 0 !important;
      font-size: 24px !important;
      justify-content: center !important;
    }

    /* 强制显示 span 文字（Element Plus 收起态默认隐藏） */
    :deep(.el-menu-item span) {
      display: block !important;
      width: auto !important;
      overflow: visible !important;
      visibility: visible !important;
      opacity: 1 !important;
      height: auto !important;
      font-size: 10px !important;
      line-height: 1.2 !important;
      white-space: nowrap !important;
      text-align: center !important;
      margin: 0 !important;
      padding: 0 0 4px !important;
    }

    /* tooltip 触发器撑满并居中 */
    :deep(.el-tooltip__trigger) {
      display: flex !important;
      flex-direction: column !important;
      align-items: center !important;
      justify-content: center !important;
      width: 100% !important;
      height: 100% !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    background: rgba(255, 255, 255, 0.18) !important;
    font-weight: 500;
  }

  :deep(.el-menu-item:hover) {
    background: rgba(255, 255, 255, 0.10) !important;
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

  .topbar-icon-group {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px 4px 6px;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: var(--primary-soft);
      .topbar-icon { color: var(--primary-main); }
      .topbar-icon-label { color: var(--primary-main); }
    }
  }

  .topbar-icon {
    font-size: 20px;
    color: var(--neutral-500);
    transition: color 0.2s;
  }

  .topbar-icon-label {
    font-size: 13px;
    font-weight: 500;
    color: var(--neutral-500);
    white-space: nowrap;
    transition: color 0.2s;
  }

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

  .settings-gear {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--radius-sm);
    color: var(--neutral-500);
    cursor: pointer;
    transition: all 0.2s;
    outline: none;

    &:hover {
      color: var(--primary-main);
      background: var(--primary-soft);
    }
  }
}

/* ── 侧边栏底部用户模块 ── */
.sidebar-user {
  border-top: 1px solid rgba(255, 255, 255, 0.10);
  padding: 12px 0;
  flex-shrink: 0;

  &.is-collapsed {
    display: flex;
    justify-content: center;
    align-items: center;
  }
}

.sidebar-user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 20px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
  outline: none;
  margin: 0 8px;

  .is-collapsed & {
    margin: 0;
    padding: 0;
    justify-content: center;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.12);
  }
}

.sidebar-user-name {
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
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
}
</style>

<style lang="scss">
/* 隐藏 el-menu 折叠态自动生成的 tooltip 弹出层（现在用内联文字标签替代） */
.el-popper.is-el-menu-tooltip {
  display: none !important;
}

/* Popover 菜单样式（teleported 到 body，需要全局样式） */
.sidebar-user-popover {
  padding: 4px 0 !important;
  border-radius: 8px !important;
  background: #fff !important;
  color: #000 !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12) !important;
}

.user-popover-menu {
  .user-popover-item {
    padding: 8px 16px !important;
    font-size: 14px !important;
    color: #000 !important;
    cursor: pointer;
    transition: background 0.15s;
    white-space: nowrap;
    line-height: 1.5 !important;

    &:hover {
      background: #f5f7fa !important;
      color: #2980b9 !important;
    }
  }

  .user-popover-divider {
    height: 1px;
    background: #e4e7ed;
    margin: 4px 0;
  }
}
</style>
