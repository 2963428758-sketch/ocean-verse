<template>
  <div class="profile-page">
    <div class="page-header"><h2>个人中心</h2></div>
    <el-card>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ userStore.userId }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
</script>
