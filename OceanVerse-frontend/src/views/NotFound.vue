<template>
  <div class="not-found">
    <h1>🌊 404</h1>
    <p>页面走丢了...</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found { text-align:center; padding:120px 0; }
.not-found h1 { font-size:72px; color:#0f4c75; }
.not-found p { color:#6c757d; margin:16px 0 32px; }
</style>
