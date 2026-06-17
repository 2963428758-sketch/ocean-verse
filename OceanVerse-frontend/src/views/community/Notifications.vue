<template>
  <div class="notifications-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">消息通知</span>
      </template>
      <template #extra>
        <el-button v-if="notifications.length > 0" type="primary" link @click="handleMarkAllRead">
          全部已读
        </el-button>
      </template>
    </el-page-header>

    <div v-loading="loading" class="notification-list">
      <div v-if="notifications.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无通知" />
      </div>

      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: item.isRead === 0 }"
        @click="handleRead(item)"
      >
        <div class="notification-icon" :class="typeClass(item.type)">
          <el-icon v-if="item.type === 'LIKE'"><Star /></el-icon>
          <el-icon v-else-if="item.type === 'COMMENT'"><ChatDotRound /></el-icon>
          <el-icon v-else-if="item.type === 'FOLLOW'"><User /></el-icon>
          <el-icon v-else><Bell /></el-icon>
        </div>
        <div class="notification-body">
          <div class="notification-header">
            <span class="notification-title">{{ item.title }}</span>
            <span class="notification-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <p class="notification-content">{{ item.content }}</p>
        </div>
        <div v-if="item.isRead === 0" class="unread-dot"></div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadNotifications"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Star, ChatDotRound, User, Bell } from '@element-plus/icons-vue'
import { getNotificationList, markNotificationRead, markAllRead } from '@/api/community'
import type { CommunityNotification } from '@/types'

const loading = ref(false)
const notifications = ref<CommunityNotification[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

async function loadNotifications() {
  loading.value = true
  try {
    const res: any = await getNotificationList({ page: currentPage.value, size: pageSize })
    notifications.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleRead(item: CommunityNotification) {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.id)
      item.isRead = 1
    } catch (e) {
      console.error(e)
    }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    notifications.value.forEach(n => (n.isRead = 1))
  } catch (e) {
    console.error(e)
  }
}

function typeClass(type: string): string {
  const map: Record<string, string> = {
    LIKE: 'type-like',
    COMMENT: 'type-comment',
    FOLLOW: 'type-follow',
    SYSTEM: 'type-system',
  }
  return map[type] || 'type-system'
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped lang="scss">
.notifications-page {
  max-width: 720px;
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

.page-title {
  font-size: 15px;
  font-weight: 600;
}

:deep(.el-page-header) {
  margin-bottom: 12px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  background: var(--surface-card);
  border-radius: var(--radius-sm);
  border: 1px solid var(--neutral-100);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    box-shadow: var(--shadow-sm);
    transform: translateY(-1px);
  }

  &.unread {
    background: var(--primary-soft);
    border-color: rgba(26, 107, 138, 0.1);
  }
}

.notification-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 15px;

  &.type-like {
    background: #fef4ee;
    color: var(--accent-warm);
  }

  &.type-comment {
    background: var(--primary-soft);
    color: var(--primary-main);
  }

  &.type-follow {
    background: var(--accent-violet-soft);
    color: var(--accent-violet);
  }

  &.type-system {
    background: var(--neutral-75);
    color: var(--neutral-500);
  }
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.notification-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--neutral-700);
}

.notification-time {
  font-size: 11px;
  color: var(--neutral-400);
  flex-shrink: 0;
}

.notification-content {
  font-size: 12px;
  color: var(--neutral-500);
  line-height: 1.45;
}

.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-warm);
  flex-shrink: 0;
  margin-top: 5px;
}

.empty-state {
  padding: 30px 0;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
</style>
