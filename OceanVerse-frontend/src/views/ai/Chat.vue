<template>
  <div>
    <div class="page-header">
      <div>
        <h2>💬 AI 智能问答</h2>
        <p>向 AI 助手咨询海洋生物知识</p>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="handleNewConversation" :disabled="loading">
          新对话
        </el-button>
        <el-button size="small" @click="handleRebuildKnowledge" :loading="rebuilding">
          重建知识库
        </el-button>
      </div>
    </div>
    <el-card class="chat-card">
      <div class="chat-toolbar">
        <el-select v-model="questionType" size="small" style="width: 160px">
          <el-option label="通用问题" value="GENERAL" />
          <el-option label="物种识别" value="SPECIES" />
          <el-option label="生态关系" value="ECOLOGY" />
          <el-option label="保护相关" value="CONSERVATION" />
        </el-select>
      </div>
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome">
          <h3>🌊 海洋 AI 助手</h3>
          <p>试试问我：</p>
          <div class="suggestions">
            <el-tag v-for="q in suggestions" :key="q" class="suggestion" @click="sendQuestion(q)">{{ q }}</el-tag>
          </div>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
          <div class="message-wrapper">
            <div v-if="msg.role === 'assistant'" class="bubble markdown-body" v-html="renderMarkdown(msg.content)"></div>
            <div v-else class="bubble">{{ msg.content }}</div>
            <div v-if="msg.role === 'assistant' && msg.content && !loading" class="feedback-bar">
              <el-button
                size="small" text
                :type="msg.feedback === 1 ? 'success' : undefined"
                :disabled="msg.feedback !== undefined && msg.feedback !== null"
                @click="handleFeedback(i, 1)"
                title="满意">👍</el-button>
              <el-button
                size="small" text
                :type="msg.feedback === 0 ? 'danger' : undefined"
                :disabled="msg.feedback !== undefined && msg.feedback !== null"
                @click="handleFeedback(i, 0)"
                title="不满意">👎</el-button>
              <span v-if="msg.feedback === 1" class="feedback-label success">已评价：满意</span>
              <span v-if="msg.feedback === 0" class="feedback-label fail">已评价：不满意</span>
            </div>
          </div>
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
import { marked } from 'marked'
import { chatWithAIStream, clearSession, submitFeedback, rebuildKnowledgeBase } from '@/api/ai'
import { ElMessage } from 'element-plus'

// 配置 marked：同步解析，GFM 支持
marked.setOptions({
  breaks: true,
  gfm: true
})

const renderMarkdown = (text: string): string => {
  if (!text) return ''
  return marked.parse(text) as string
}

const messagesRef = ref<HTMLElement>()
const input = ref('')
const loading = ref(false)
const rebuilding = ref(false)
const questionType = ref('GENERAL')
const sessionId = ref(generateSessionId())
const messages = reactive<{ role: string; content: string; qaId?: number; feedback?: number | null }[]>([])
const suggestions = ['绿海龟的保护现状', '珊瑚礁为什么会白化？', '深海有哪些奇特生物？', '海洋酸化的影响']

function generateSessionId(): string {
  return 'session-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9)
}

function sendQuestion(q: string) { input.value = q; sendMessage() }

async function handleNewConversation() {
  if (loading.value) return
  try {
    await clearSession(sessionId.value)
  } catch {
    // 清空失败不影响前端操作（可能 session 已过期）
  }
  messages.splice(0, messages.length)
  sessionId.value = generateSessionId()
  ElMessage.success('已开始新对话')
}

async function handleRebuildKnowledge() {
  rebuilding.value = true
  try {
    await rebuildKnowledgeBase()
    ElMessage.success('知识库重建成功')
  } catch {
    ElMessage.error('知识库重建失败')
  } finally {
    rebuilding.value = false
  }
}

async function handleFeedback(index: number, value: number) {
  const msg = messages[index]
  if (!msg.qaId || msg.qaId <= 0 || msg.feedback !== undefined && msg.feedback !== null) return
  try {
    await submitFeedback(msg.qaId, value)
    msg.feedback = value
    ElMessage.success(value === 1 ? '感谢反馈！' : '已记录，我们会持续改进')
  } catch {
    ElMessage.error('反馈提交失败')
  }
}

function sendMessage() {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.push({ role: 'user', content: text })
  messages.push({ role: 'assistant', content: '', qaId: undefined, feedback: undefined })
  input.value = ''
  loading.value = true
  const idx = messages.length - 1

  chatWithAIStream(
    { question: text, sessionId: sessionId.value, questionType: questionType.value },
    (chunk) => { messages[idx].content += chunk; scrollToBottom() },
    () => { loading.value = false },
    () => { messages[idx].content = messages[idx].content || '暂无法回答'; loading.value = false },
    (meta) => {
      // 后端流结束后发送 {"qaId":N}，用于反馈功能
      if (meta.qaId && meta.qaId > 0) {
        messages[idx].qaId = meta.qaId
      }
    }
  )
}

function scrollToBottom() {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  h2 { margin: 0; }
  p { margin: 4px 0 0; }
}
.header-actions {
  display: flex;
  gap: 8px;
  padding-top: 4px;
}
.chat-card { :deep(.el-card__body) { padding:0; display:flex; flex-direction:column; height:calc(100vh - 250px); } }
.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  border-bottom: 1px solid var(--neutral-100);
  font-size: 13px;
  color: var(--neutral-500);
}
.chat-messages { flex:1; overflow-y:auto; padding:20px; }
.welcome { text-align:center; padding:60px 0; h3{font-size:20px;margin-bottom:8px;} p{color:var(--neutral-500);margin-bottom:16px;} }
.suggestions { display:flex; flex-wrap:wrap; justify-content:center; gap:8px; }
.suggestion { cursor:pointer; }
.message { margin-bottom:16px; display:flex; &.user{justify-content:flex-end;} &.assistant{justify-content:flex-start;} }
.message-wrapper { max-width:80%; }
.bubble { max-width:100%; padding:10px 16px; border-radius:12px; font-size:14px; line-height:1.6; }
.message.user .bubble { background:var(--primary-main); color:white; }
.message.assistant .bubble { background:var(--neutral-100); color:var(--neutral-800); }
.feedback-bar {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
  padding-left: 4px;
  .el-button { font-size: 14px; padding: 2px 6px; }
}
.feedback-label {
  font-size: 12px;
  margin-left: 4px;
  &.success { color: var(--el-color-success); }
  &.fail { color: var(--el-color-danger); }
}
.chat-input { display:flex; gap:8px; padding:12px 20px; border-top:1px solid var(--neutral-100); }

/* ── Markdown 排版（v-html 内容需要 :deep 穿透）── */
.markdown-body {
  :deep(p) {
    margin-bottom: 8px;
    &:last-child { margin-bottom: 0; }
  }
  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 20px;
  }
  :deep(li) {
    margin-bottom: 4px;
    line-height: 1.7;
  }
  :deep(strong) {
    font-weight: 600;
    color: var(--neutral-900);
  }
  :deep(em) {
    font-style: italic;
    color: var(--neutral-600);
  }
  :deep(code) {
    background: var(--neutral-75);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  }
  :deep(pre) {
    background: var(--neutral-75);
    padding: 12px 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;
    code {
      background: none;
      padding: 0;
    }
  }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 12px 0 6px;
    font-weight: 600;
    &:first-child { margin-top: 0; }
  }
  :deep(h3) { font-size: 15px; }
  :deep(h4) { font-size: 14px; }
  :deep(blockquote) {
    border-left: 3px solid var(--primary-lighter);
    padding-left: 12px;
    margin: 8px 0;
    color: var(--neutral-500);
  }
  :deep(a) {
    color: var(--primary-main);
    text-decoration: underline;
  }
  :deep(table) {
    border-collapse: collapse;
    margin: 8px 0;
    width: 100%;
    font-size: 13px;
  }
  :deep(th), :deep(td) {
    border: 1px solid var(--neutral-200);
    padding: 6px 10px;
    text-align: left;
  }
  :deep(th) {
    background: var(--neutral-75);
    font-weight: 600;
  }
  :deep(hr) {
    border: none;
    border-top: 1px solid var(--neutral-200);
    margin: 12px 0;
  }
}
</style>
