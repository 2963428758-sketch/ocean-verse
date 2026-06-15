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
        background-color="#0f4c75"
        text-color="#b8d4e8"
        active-text-color="#ffffff"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              {{ child.title }}
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
  { path: '/species', title: '物种百科', icon: 'Leaf', children: [
    { path: '/species/list', title: '物种列表' }
  ]},
  { path: '/observation', title: '观测记录', icon: 'Binoculars', children: [
    { path: '/observation/list', title: '观测列表' }
  ]},
  { path: '/visualization', title: '数据可视化', icon: 'DataAnalysis', children: [
    { path: '/visualization/statistics', title: '数据统计' },
    { path: '/visualization/map', title: '分布地图' }
  ]},
  { path: '/ai', title: 'AI 服务', icon: 'MagicStick', children: [
    { path: '/ai/recognize', title: '图像识别' },
    { path: '/ai/chat', title: '智能问答' }
  ]},
  { path: '/community', title: '社区', icon: 'ChatDotRound', children: [
    { path: '/community/feed', title: '动态广场' }
  ]},
  { path: '/admin', title: '系统管理', icon: 'Setting', children: [
    { path: '/admin/users', title: '用户管理' }
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

.sidebar {
  background: #0f4c75;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  .logo-icon { font-size: 28px; }
  .logo-text { color: #fff; font-size: 18px; font-weight: 700; white-space: nowrap; }
}

.sidebar-menu {
  border-right: none;
  :deep(.el-menu-item.is-active) { background: rgba(255,255,255,0.15) !important; }
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: var(--shadow-sm);
  padding: 0 20px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  .collapse-btn { font-size: 20px; cursor: pointer; }
}

.topbar-right {
  .user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
  .username { font-size: 14px; color: var(--neutral-600); }
}

.main-content { background: var(--neutral-50); padding: 24px; }
</style>
