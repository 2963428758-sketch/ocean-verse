import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { requiresAuth: false } },
  {
    path: '/', component: MainLayout, redirect: '/dashboard', meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '仪表盘', icon: 'Odometer' } },
      { path: 'species', name: 'Species', redirect: '/species/list', meta: { title: '物种百科', icon: 'Collection' }, children: [
        { path: 'list', name: 'SpeciesList', component: () => import('@/views/species/SpeciesList.vue'), meta: { title: '物种列表' } },
        { path: 'detail/:id', name: 'SpeciesDetail', component: () => import('@/views/species/SpeciesDetail.vue'), meta: { title: '物种详情', hidden: true } }
      ]},
      { path: 'observation', name: 'Observation', redirect: '/observation/list', meta: { title: '观测记录', icon: 'Compass' }, children: [
        { path: 'list', name: 'ObservationList', component: () => import('@/views/observation/ObservationList.vue'), meta: { title: '观测列表' } },
        { path: 'detail/:id', name: 'ObservationDetail', component: () => import('@/views/observation/ObservationDetail.vue'), meta: { title: '观测详情', hidden: true } }
      ]},
      { path: 'visualization', name: 'Visualization', redirect: '/visualization/statistics', meta: { title: '数据可视化', icon: 'DataAnalysis' }, children: [
        { path: 'statistics', name: 'Statistics', component: () => import('@/views/visualization/Statistics.vue'), meta: { title: '数据统计' } },
        { path: 'map', name: 'DistributionMap', component: () => import('@/views/visualization/DistributionMap.vue'), meta: { title: '分布地图' } }
      ]},
      { path: 'ai', name: 'AI', redirect: '/ai/recognize', meta: { title: 'AI 服务', icon: 'MagicStick' }, children: [
        { path: 'recognize', name: 'AIRecognize', component: () => import('@/views/ai/Recognize.vue'), meta: { title: '图像识别' } },
        { path: 'chat', name: 'AIChat', component: () => import('@/views/ai/Chat.vue'), meta: { title: '智能问答' } }
      ]},
      { path: 'community', name: 'Community', redirect: '/community/feed', meta: { title: '社区', icon: 'ChatDotRound' }, children: [
        { path: 'feed', name: 'CommunityFeed', component: () => import('@/views/community/Feed.vue'), meta: { title: '动态广场' } }
      ]},
      { path: 'admin', name: 'Admin', redirect: '/admin/users', meta: { title: '系统管理', icon: 'Setting' }, children: [
        { path: 'users', name: 'UserManagement', component: () => import('@/views/admin/UserManagement.vue'), meta: { title: '用户管理' } }
      ]},
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心', hidden: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, _from, next) => {
  document.title = `${(to.meta as any).title || ''} - OceanVerse 智慧海洋探索平台`
  const token = localStorage.getItem('token')
  const requiresAuth = to.matched.some(r => r.meta.requiresAuth !== false)
  if (requiresAuth && !token) next('/login')
  else if (to.path === '/login' && token) next('/dashboard')
  else next()
})

export default router
