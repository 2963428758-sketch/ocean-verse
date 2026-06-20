<template>
  <div class="notifications-page">
    <!-- 导航 -->
    <div class="nav-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <span class="nav-title">消息通知</span>
      <div class="nav-actions">
        <button v-if="notifications.length > 0" class="mark-all" @click="handleMarkAllRead">全部已读</button>
        <button v-if="notifications.some(n => n.isRead === 1)" class="clear-read" @click="handleClearRead">清空已读</button>
      </div>
    </div>

    <!-- 通知列表 -->
    <div v-loading="loading" class="notification-list">
      <div v-if="notifications.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🔔</div>
        <p>暂无通知</p>
      </div>

      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-card"
        :class="{ unread: item.isRead === 0 }"
        @click="handleRead(item)"
      >
        <div class="noti-icon" :class="typeClass(item.type)">
          <svg v-if="item.type === 'LIKE'" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          <svg v-else-if="item.type === 'COMMENT'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <svg v-else-if="item.type === 'FOLLOW'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
          <svg v-else-if="item.type === 'QUOTA_WARNING'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <svg v-else-if="item.type === 'KNOWLEDGE_REBUILT'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>
          <svg v-else-if="item.type === 'AI_RESULT'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
        </div>
        <div class="noti-body">
          <div class="noti-top">
            <span class="noti-title">{{ item.title }}</span>
            <span class="noti-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <p class="noti-content">{{ item.content }}</p>
        </div>
        <div class="noti-actions">
          <div v-if="item.isRead === 0" class="unread-dot"></div>
          <button class="delete-btn" @click.stop="handleDelete(item)" title="删除">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--; loadNotifications()">上一页</button>
      <span class="page-info">{{ currentPage }} / {{ Math.ceil(total / pageSize) }}</span>
      <button class="page-btn" :disabled="currentPage >= Math.ceil(total / pageSize)" @click="currentPage++; loadNotifications()">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotificationList, markNotificationRead, markAllRead, deleteNotification, deleteAllReadNotifications } from '@/api/community'
import type { CommunityNotification } from '@/types'

const router = useRouter()

const loading = ref(false)
const notifications = ref<CommunityNotification[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

async function loadNotifications() {
  if (loading.value) return
  loading.value = true
  try {
    const res: any = await getNotificationList({ page: currentPage.value, size: pageSize })
    const list = res.data?.records || res.data || []
    // 按 id 去重，防止并发请求导致重复数据
    const map = new Map<number, CommunityNotification>()
    list.forEach((n: CommunityNotification) => { if (!map.has(n.id)) map.set(n.id, n) })
    notifications.value = Array.from(map.values())
    total.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleRead(item: CommunityNotification) {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.id)
      item.isRead = 1
      window.dispatchEvent(new Event('notification-changed'))
    } catch (e) { console.error(e) }
  }
  // AI_RESULT 通知点击跳转到识别结果页
  if (item.type === 'AI_RESULT' && item.relatedId) {
    router.push({ path: '/ai/recognize', query: { id: String(item.relatedId) } })
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    notifications.value.forEach(n => (n.isRead = 1))
    window.dispatchEvent(new Event('notification-changed'))
  } catch (e) { console.error(e) }
}

async function handleDelete(item: CommunityNotification) {
  try {
    await deleteNotification(item.id)
    notifications.value = notifications.value.filter(n => n.id !== item.id)
    total.value = Math.max(0, total.value - 1)
  } catch (e) { console.error(e) }
}

async function handleClearRead() {
  try {
    await deleteAllReadNotifications()
    notifications.value = notifications.value.filter(n => n.isRead === 0)
    total.value = notifications.value.length
  } catch (e) { console.error(e) }
}

function typeClass(type: string): string {
  return { LIKE: 'type-like', COMMENT: 'type-comment', FOLLOW: 'type-follow', QUOTA_WARNING: 'type-warning', KNOWLEDGE_REBUILT: 'type-knowledge', AI_RESULT: 'type-ai', SYSTEM: 'type-system' }[type] || 'type-system'
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped lang="scss">
.notifications-page {
  max-width: 680px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

/* ══════ 导航栏 ══════ */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: var(--surface-card);
  color: var(--neutral-600);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--shadow-xs);
  &:hover { background: var(--neutral-75); transform: translateX(-2px); }
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--neutral-800);
}

.mark-all {
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--primary-main);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  &:hover { background: var(--primary-soft); }
}

.nav-actions {
  display: flex;
  gap: 4px;
}

.clear-read {
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--neutral-400);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  &:hover { color: #e05555; background: #fef4ee; }
}

/* ══════ 通知列表 ══════ */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--neutral-75);
  cursor: pointer;
  transition: all 0.25s var(--ease-out);
  position: relative;

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-1px);
  }

  &.unread {
    background: linear-gradient(135deg, #f0f8ff 0%, #f8fbff 100%);
    border-color: rgba(26, 107, 138, 0.12);
  }
}

.noti-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;

  .notification-card:hover & {
    transform: scale(1.05);
  }

  &.type-like {
    background: #fef4ee;
    color: #e07850;
  }
  &.type-comment {
    background: #e8f4f8;
    color: #1a6b8a;
  }
  &.type-follow {
    background: #f0eefa;
    color: #7c5cbf;
  }
  &.type-warning {
    background: #fff8e6;
    color: #d4a017;
  }
  &.type-knowledge {
    background: #e8f5e9;
    color: #2e7d32;
  }
  &.type-ai {
    background: #eef0fb;
    color: #5c6bc0;
  }
  &.type-system {
    background: var(--neutral-75);
    color: var(--neutral-500);
  }
}

.noti-body {
  flex: 1;
  min-width: 0;
}

.noti-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 3px;
}

.noti-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-700);
}

.noti-time {
  font-size: 12px;
  color: var(--neutral-400);
  flex-shrink: 0;
}

.noti-content {
  font-size: 13px;
  color: var(--neutral-500);
  line-height: 1.5;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4757;
  flex-shrink: 0;
  animation: pulseGlow 2s infinite;
}

.noti-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-top: 4px;
}

.delete-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--neutral-400);
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: #fef4ee; color: #e05555; }

  .notification-card:hover & {
    display: flex;
  }
}

/* ══════ 分页 ══════ */
.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
}

.page-btn {
  padding: 8px 20px;
  border: 1px solid var(--neutral-200);
  border-radius: 20px;
  font-size: 13px;
  color: var(--neutral-600);
  background: var(--surface-card);
  cursor: pointer;
  transition: all 0.2s;
  &:hover:not(:disabled) { border-color: var(--primary-main); color: var(--primary-main); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

.page-info { font-size: 13px; color: var(--neutral-500); }

.empty-state {
  text-align: center;
  padding: 80px 20px;
  .empty-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.4; }
  p { font-size: 14px; color: var(--neutral-400); }
}
</style>
