<template>
  <div>
    <div class="page-header"><h2>💬 AI 智能问答</h2><p>向 AI 助手咨询海洋生物知识</p></div>
    <el-card class="chat-card">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome">
          <h3>🌊 海洋 AI 助手</h3>
          <p>试试问我：</p>
          <div class="suggestions">
            <el-tag v-for="q in suggestions" :key="q" class="suggestion" @click="sendQuestion(q)">{{ q }}</el-tag>
          </div>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
          <div class="bubble">{{ msg.content }}</div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="input" placeholder="输入你的问题..." @keyup.enter="sendMessage" :disabled="loading" />
        <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { chatWithAIStream } from '@/api/ai'

const messagesRef = ref<HTMLElement>()
const input = ref('')
const loading = ref(false)
const messages = reactive<{ role: string; content: string }[]>([])
const suggestions = ['绿海龟的保护现状', '珊瑚礁为什么会白化？', '深海有哪些奇特生物？', '海洋酸化的影响']

function sendQuestion(q: string) { input.value = q; sendMessage() }

function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.push({ role: 'user', content: text })
  messages.push({ role: 'assistant', content: '' })
  input.value = ''
  loading.value = true
  const idx = messages.length - 1

  chatWithAIStream(
    { question: text },
    (chunk) => { messages[idx].content += chunk; scrollToBottom() },
    () => { loading.value = false },
    () => { messages[idx].content = messages[idx].content || '暂无法回答'; loading.value = false }
  )
}

function scrollToBottom() {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}
</script>

<style scoped lang="scss">
.chat-card { :deep(.el-card__body) { padding:0; display:flex; flex-direction:column; height:calc(100vh - 250px); } }
.chat-messages { flex:1; overflow-y:auto; padding:20px; }
.welcome { text-align:center; padding:60px 0; h3{font-size:20px;margin-bottom:8px;} p{color:var(--neutral-500);margin-bottom:16px;} }
.suggestions { display:flex; flex-wrap:wrap; justify-content:center; gap:8px; }
.suggestion { cursor:pointer; }
.message { margin-bottom:16px; display:flex; &.user{justify-content:flex-end;} &.assistant{justify-content:flex-start;} }
.bubble { max-width:70%; padding:10px 16px; border-radius:12px; font-size:14px; line-height:1.6; }
.message.user .bubble { background:var(--primary-main); color:white; }
.message.assistant .bubble { background:var(--neutral-100); color:var(--neutral-800); }
.chat-input { display:flex; gap:8px; padding:12px 20px; border-top:1px solid var(--neutral-100); }
</style>
