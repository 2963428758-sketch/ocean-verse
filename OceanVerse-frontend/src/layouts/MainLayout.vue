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
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const menuItems = [
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
    { path: '/community/feed', title: '动态广场', icon: 'Postcard' }
  ]},
  { path: '/admin', title: '系统管理', icon: 'Setting', children: [
    { path: '/admin/users', title: '用户管理', icon: 'User' },
    { path: '/admin/roles', title: '角色管理', icon: 'UserFilled' }
  ]}
]

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
