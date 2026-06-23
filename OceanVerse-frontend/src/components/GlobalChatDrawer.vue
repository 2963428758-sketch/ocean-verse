<template>
  <Teleport to="body">
    <!-- 悬浮按钮 -->
    <div
      class="chat-fab"
      :class="{ hidden: drawerVisible }"
      @click="drawerVisible = true"
    >
      <svg viewBox="0 0 24 24" fill="none" width="26" height="26">
        <path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div>

    <!-- 抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      :size="drawerSize"
      :with-header="false"
      :z-index="2000"
      :append-to-body="true"
      class="global-chat-drawer"
    >
      <div class="chat-drawer-content">
        <!-- 顶部栏 -->
        <div class="chat-header">
          <div class="chat-header-left">
            <div class="chat-title-icon">
              <svg viewBox="0 0 24 24" fill="none" width="22" height="22">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" fill="var(--primary-main)" opacity="0.15"/>
                <path d="M8 14s1.5 2 4 2 4-2 4-2" stroke="var(--primary-main)" stroke-width="1.5" stroke-linecap="round"/>
                <circle cx="9" cy="10" r="1.2" fill="var(--primary-main)"/>
                <circle cx="15" cy="10" r="1.2" fill="var(--primary-main)"/>
              </svg>
            </div>
            <div>
              <h2 class="chat-title">海洋 AI 助手</h2>
              <span class="chat-subtitle">基于 RAG 知识库 + 多轮对话</span>
            </div>
          </div>
          <div class="chat-header-actions">
            <button class="icon-btn" @click="handleNewConversation" :disabled="loading" title="新对话">
              <svg viewBox="0 0 20 20" fill="none" width="16" height="16">
                <path d="M10 4v12M4 10h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
              <span>新对话</span>
            </button>
            <button class="icon-btn close-btn" @click="drawerVisible = false" title="关闭">
              <svg viewBox="0 0 20 20" fill="none" width="16" height="16">
                <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 问题类型选择 -->
        <div class="type-bar">
          <button
            v-for="t in questionTypes" :key="t.value"
            class="type-chip" :class="{ active: questionType === t.value }"
            @click="questionType = t.value">
            <span class="type-emoji">{{ t.emoji }}</span>
            {{ t.label }}
          </button>
        </div>

        <!-- 消息区 -->
        <div class="chat-messages" ref="messagesRef">
          <!-- 欢迎页 -->
          <div v-if="messages.length === 0" class="welcome">
            <div class="welcome-icon">
              <svg viewBox="0 0 80 80" fill="none" width="64" height="64">
                <circle cx="40" cy="40" r="36" fill="var(--primary-main)" opacity="0.08"/>
                <path d="M25 45c3-6 8-10 15-10s12 4 15 10" stroke="var(--primary-main)" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
                <circle cx="40" cy="32" r="8" stroke="var(--primary-main)" stroke-width="2"/>
                <path d="M20 50c5-3 12-5 20-5s15 2 20 5" stroke="var(--primary-main)" stroke-width="2" stroke-linecap="round"/>
                <path d="M28 55c3 3 8 5 12 5s9-2 12-5" stroke="var(--primary-main)" stroke-width="1.5" stroke-linecap="round" opacity="0.4"/>
              </svg>
            </div>
            <h3 class="welcome-title">你好，我是海洋 AI 助手</h3>
            <p class="welcome-desc">我可以帮你识别海洋物种、解答生态问题、提供保护建议</p>
            <div class="welcome-cards">
              <div v-for="s in suggestionCards" :key="s.text" class="welcome-card" @click="sendQuestion(s.text)">
                <span class="card-emoji">{{ s.emoji }}</span>
                <div class="card-body">
                  <span class="card-label">{{ s.label }}</span>
                  <span class="card-text">{{ s.text }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 消息列表 -->
          <template v-for="(msg, i) in messages" :key="i">
            <div class="message-row" :class="msg.role">
              <!-- 头像 -->
              <div class="avatar" :class="msg.role">
                <svg v-if="msg.role === 'user'" viewBox="0 0 24 24" fill="none" width="18" height="18">
                  <circle cx="12" cy="8" r="4" stroke="white" stroke-width="1.5"/>
                  <path d="M4 20c0-4 4-7 8-7s8 3 8 7" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" width="18" height="18">
                  <path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9-4.03-9-9-9z" fill="var(--primary-main)" opacity="0.15"/>
                  <path d="M8 14s1.5 2 4 2 4-2 4-2" stroke="var(--primary-main)" stroke-width="1.5" stroke-linecap="round"/>
                  <circle cx="9" cy="10" r="1" fill="var(--primary-main)"/>
                  <circle cx="15" cy="10" r="1" fill="var(--primary-main)"/>
                </svg>
              </div>
              <!-- 消息体 -->
              <div class="message-body">
                <div class="bubble" :class="msg.role">
                  <div v-if="msg.role === 'assistant' && msg.content" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                  <div v-else-if="msg.role === 'assistant' && !msg.content && loading && i === messages.length - 1" class="typing-indicator">
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                  </div>
                  <div v-else>{{ msg.content }}</div>
                </div>
                <!-- 底部：时间 + 反馈 -->
                <div class="message-footer" v-if="msg.role === 'assistant' && msg.content && !loading">
                  <span class="msg-time">{{ msg.time }}</span>
                  <div class="feedback-bar">
                    <button class="fb-btn" :class="{ active: msg.feedback === 1 }" :disabled="msg.feedback != null && msg.feedback !== undefined" @click="handleFeedback(i, 1)" title="满意">
                      <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <path d="M5 14V7l3-5a1 1 0 011-.7h.5a1 1 0 011 1V6h3a1 1 0 011 1.1l-1 5A1 1 0 0112.5 13H5z" stroke="currentColor" stroke-width="1.2"/>
                        <path d="M2 8h2.5v6H2a.5.5 0 01-.5-.5v-5A.5.5 0 012 8z" stroke="currentColor" stroke-width="1.2"/>
                      </svg>
                    </button>
                    <button class="fb-btn" :class="{ active: msg.feedback === 0 }" :disabled="msg.feedback != null && msg.feedback !== undefined" @click="handleFeedback(i, 0)" title="不满意">
                      <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <path d="M11 2v7l-3 5a1 1 0 01-1 .7H6.5a1 1 0 01-1-1V10h-3a1 1 0 01-1-1.1l1-5A1 1 0 013.5 3H11z" stroke="currentColor" stroke-width="1.2"/>
                        <path d="M14 8h-2.5V2H14a.5.5 0 01.5.5v5a.5.5 0 01-.5.5z" stroke="currentColor" stroke-width="1.2"/>
                      </svg>
                    </button>
                    <span v-if="msg.feedback === 1" class="fb-label ok">感谢反馈</span>
                    <span v-if="msg.feedback === 0" class="fb-label bad">已记录</span>
                  </div>
                </div>
                <div class="message-footer" v-if="msg.role === 'user'">
                  <span class="msg-time">{{ msg.time }}</span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 输入区 -->
        <div class="chat-input-bar">
          <div class="input-wrapper">
            <input
              v-model="input"
              placeholder="输入你的问题..."
              @keyup.enter="sendMessage"
              :disabled="loading"
              class="chat-input"
            />
            <span class="char-count" v-if="input.length > 0">{{ input.length }}</span>
            <button class="send-btn" :class="{ active: input.trim() && !loading }" :disabled="!input.trim() || loading" @click="sendMessage">
              <svg viewBox="0 0 20 20" fill="none" width="18" height="18">
                <path d="M3 10l14-7-4 16-4-7-6-2z" fill="currentColor" opacity="0.9"/>
              </svg>
            </button>
          </div>
          <div class="input-hint">Enter 发送 · 当前模式：{{ questionTypes.find(t => t.value === questionType)?.label }}</div>
        </div>
      </div>
    </el-drawer>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, computed, onMounted } from 'vue'
import { marked } from 'marked'
import { chatWithAIStream, clearSession, submitFeedback } from '@/api/ai'
import { ElMessage } from 'element-plus'

marked.setOptions({ breaks: true, gfm: true })

const renderMarkdown = (text: string): string => {
  if (!text) return ''
  return marked.parse(text) as string
}

const drawerVisible = ref(false)
const drawerSize = computed(() => window.innerWidth < 500 ? '100%' : 450)

const messagesRef = ref<HTMLElement>()
const input = ref('')
const loading = ref(false)
const questionType = ref('GENERAL')
const sessionId = ref(generateSessionId())
const messages = reactive<{ role: string; content: string; qaId?: number; feedback?: number | null; time?: string }[]>([])

const questionTypes = [
  { value: 'GENERAL', label: '通用', emoji: '💬' },
  { value: 'SPECIES', label: '物种', emoji: '🐟' },
  { value: 'ECOLOGY', label: '生态', emoji: '🌿' },
  { value: 'CONSERVATION', label: '保护', emoji: '🛡️' },
]

const suggestionPool = [
  { emoji: '🐢', label: '物种知识', text: '绿海龟的保护现状' },
  { emoji: '🪸', label: '生态现象', text: '珊瑚礁为什么会白化？' },
  { emoji: '🐙', label: '深海探索', text: '深海有哪些奇特生物？' },
  { emoji: '🌊', label: '环境问题', text: '海洋酸化有什么影响？' },
  { emoji: '🦈', label: '物种知识', text: '大白鲨为什么需要不停游动？' },
  { emoji: '🐋', label: '物种知识', text: '蓝鲸为什么能长到这么大？' },
  { emoji: '🦑', label: '深海探索', text: '巨型乌贼有哪些特征？' },
  { emoji: '🐡', label: '物种知识', text: '河豚为什么会膨胀？' },
  { emoji: '🪸', label: '生态现象', text: '红树林对海岸有什么作用？' },
  { emoji: '🐠', label: '生态现象', text: '小丑鱼和海葵是什么关系？' },
  { emoji: '🐧', label: '物种知识', text: '企鹅为什么不会飞？' },
  { emoji: '🦭', label: '物种知识', text: '海豹和海狮怎么区分？' },
  { emoji: '🐚', label: '物种知识', text: '珍珠是怎么形成的？' },
  { emoji: '🌡️', label: '环境问题', text: '海水温度升高有什么影响？' },
  { emoji: '🗑️', label: '环境问题', text: '海洋塑料污染的现状如何？' },
  { emoji: '🦀', label: '物种知识', text: '帝王蟹生活在多深的海域？' },
  { emoji: '🐬', label: '物种知识', text: '海豚为什么被称为高智商动物？' },
  { emoji: '🦑', label: '深海探索', text: '深海热液喷口有什么生物？' },
  { emoji: '🌿', label: '生态现象', text: '海草床有什么生态价值？' },
  { emoji: '🛡️', label: '保护', text: '如何保护濒危海洋物种？' },
]

function shuffleArray<T>(arr: T[]): T[] {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

function refreshSuggestions() {
  suggestionCards.value = shuffleArray(suggestionPool).slice(0, 4)
}

const suggestionCards = ref<typeof suggestionPool>([])

onMounted(() => {
  refreshSuggestions()
})

function generateSessionId(): string {
  return 'session-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9)
}

function nowTime(): string {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function sendQuestion(q: string) { input.value = q; sendMessage() }

async function handleNewConversation() {
  if (loading.value) return
  try { await clearSession(sessionId.value) } catch {}
  messages.splice(0, messages.length)
  sessionId.value = generateSessionId()
  refreshSuggestions()
  ElMessage.success('已开始新对话')
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
  messages.push({ role: 'user', content: text, time: nowTime() })
  messages.push({ role: 'assistant', content: '', qaId: undefined, feedback: undefined, time: undefined })
  input.value = ''
  loading.value = true
  const idx = messages.length - 1

  chatWithAIStream(
    { question: text, sessionId: sessionId.value, questionType: questionType.value },
    (chunk) => { messages[idx].content += chunk; scrollToBottom() },
    () => { messages[idx].time = nowTime(); loading.value = false },
    () => { messages[idx].content = messages[idx].content || '暂无法回答'; messages[idx].time = nowTime(); loading.value = false },
    (meta) => { if (meta.qaId && meta.qaId > 0) messages[idx].qaId = meta.qaId }
  )
}

function scrollToBottom() {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}
</script>

<style scoped lang="scss">
/* ── 悬浮按钮 ── */
.chat-fab {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2980b9, #3da8d8);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1999;
  transition: transform 0.25s var(--ease-out, ease), opacity 0.25s, box-shadow 0.25s;
  box-shadow: 0 4px 16px rgba(41, 128, 185, 0.4);

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 24px rgba(41, 128, 185, 0.5);
  }

  &:active {
    transform: scale(0.95);
  }

  &.hidden {
    opacity: 0;
    pointer-events: none;
    transform: scale(0.6);
  }
}

/* ── 抽屉内容容器 ── */
.chat-drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--neutral-50, #f8f9fb);
  overflow: hidden;
}

/* ── 顶部栏 ── */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: white;
  border-bottom: 1px solid var(--neutral-100);
  flex-shrink: 0;
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.chat-title-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--primary-main) 8%, white);
  display: flex; align-items: center; justify-content: center;
}
.chat-title { margin: 0; font-size: 16px; font-weight: 600; color: var(--neutral-900); }
.chat-subtitle { font-size: 12px; color: var(--neutral-400); }
.chat-header-actions {
  display: flex;
  gap: 6px;
}
.icon-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--neutral-200);
  border-radius: 8px;
  background: white;
  font-size: 12px;
  color: var(--neutral-600);
  cursor: pointer;
  transition: all 0.15s;
  &:hover:not(:disabled) { border-color: var(--primary-main); color: var(--primary-main); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.close-btn {
  padding: 6px;
  &:hover { border-color: var(--neutral-400); color: var(--neutral-700); }
}

/* ── 问题类型 ── */
.type-bar {
  display: flex;
  gap: 6px;
  padding: 10px 20px;
  background: white;
  border-bottom: 1px solid var(--neutral-100);
  flex-shrink: 0;
  flex-wrap: wrap;
}
.type-chip {
  padding: 5px 14px;
  border-radius: 20px;
  border: 1px solid var(--neutral-200);
  background: white;
  font-size: 13px;
  color: var(--neutral-500);
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 4px;
  &:hover { border-color: var(--primary-main); color: var(--primary-main); }
  &.active {
    background: var(--primary-main);
    border-color: var(--primary-main);
    color: white;
    .type-emoji { filter: grayscale(1) brightness(10); }
  }
}
.type-emoji { font-size: 13px; }

/* ── 消息区 ── */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

/* 欢迎页 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 16px;
}
.welcome-icon { margin-bottom: 12px; }
.welcome-title { font-size: 17px; font-weight: 600; color: var(--neutral-800); margin: 0 0 6px; }
.welcome-desc { font-size: 13px; color: var(--neutral-400); margin: 0 0 22px; text-align: center; padding: 0 12px; }
.welcome-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  width: 100%;
}
.welcome-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border: 1px solid var(--neutral-150, #e8e8e8);
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.15s;
  &:hover { border-color: var(--primary-main); box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
}
.card-emoji { font-size: 20px; flex-shrink: 0; }
.card-body { display: flex; flex-direction: column; }
.card-label { font-size: 11px; color: var(--neutral-400); }
.card-text { font-size: 13px; color: var(--neutral-700); font-weight: 500; }

/* ── 消息行 ── */
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  animation: msgIn 0.3s ease;
  &.user { flex-direction: row-reverse; }
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 头像 */
.avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  &.user { background: var(--primary-main); }
  &.assistant { background: color-mix(in srgb, var(--primary-main) 10%, white); border: 1px solid var(--neutral-150, #e8e8e8); }
}

/* 消息体 */
.message-body {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}
.bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.65;
  &.user {
    background: var(--primary-main);
    color: white;
    border-bottom-right-radius: 4px;
  }
  &.assistant {
    background: white;
    color: var(--neutral-800);
    border: 1px solid var(--neutral-100);
    border-bottom-left-radius: 4px;
  }
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--neutral-300);
  animation: bounce 1.2s infinite;
  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 底部：时间 + 反馈 */
.message-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  padding: 0 2px;
}
.msg-time { font-size: 11px; color: var(--neutral-350, #bbb); }
.message-row.user .message-footer { justify-content: flex-end; }

.feedback-bar {
  display: flex;
  align-items: center;
  gap: 2px;
}
.fb-btn {
  width: 26px; height: 26px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--neutral-350, #bbb);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  &:hover:not(:disabled) { background: var(--neutral-100); color: var(--neutral-600); }
  &.active { color: var(--primary-main); }
  &:disabled { cursor: default; opacity: 0.6; }
}
.fb-label {
  font-size: 11px;
  margin-left: 2px;
  &.ok { color: var(--el-color-success, #67c23a); }
  &.bad { color: var(--el-color-danger, #f56c6c); }
}

/* ── 输入区 ── */
.chat-input-bar {
  padding: 12px 20px 14px;
  background: white;
  border-top: 1px solid var(--neutral-100);
  flex-shrink: 0;
}
.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--neutral-200);
  border-radius: 12px;
  padding: 4px 6px 4px 14px;
  transition: border-color 0.15s;
  &:focus-within { border-color: var(--primary-main); }
}
.chat-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 8px 0;
  background: transparent;
  color: var(--neutral-800);
  &::placeholder { color: var(--neutral-350, #bbb); }
  &:disabled { opacity: 0.5; }
}
.char-count {
  font-size: 11px;
  color: var(--neutral-350, #bbb);
  flex-shrink: 0;
}
.send-btn {
  width: 36px; height: 36px;
  border-radius: 10px;
  border: none;
  background: var(--neutral-150, #e8e8e8);
  color: var(--neutral-400);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
  &.active { background: var(--primary-main); color: white; }
  &:disabled { cursor: not-allowed; }
}
.input-hint {
  font-size: 11px;
  color: var(--neutral-350, #bbb);
  margin-top: 6px;
  padding-left: 2px;
}

/* ── Markdown 排版 ── */
.markdown-body {
  :deep(p) { margin: 0 0 6px; &:last-child { margin-bottom: 0; } }
  :deep(ul), :deep(ol) { margin: 6px 0; padding-left: 18px; }
  :deep(li) { margin-bottom: 3px; line-height: 1.65; }
  :deep(strong) { font-weight: 600; color: var(--neutral-900); }
  :deep(em) { font-style: italic; color: var(--neutral-600); }
  :deep(code) {
    background: var(--neutral-75, #f3f4f6);
    padding: 1px 5px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  }
  :deep(pre) {
    background: var(--neutral-75, #f3f4f6);
    padding: 10px 14px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 6px 0;
    code { background: none; padding: 0; }
  }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 10px 0 4px;
    font-weight: 600;
    &:first-child { margin-top: 0; }
  }
  :deep(h3) { font-size: 15px; }
  :deep(h4) { font-size: 14px; }
  :deep(blockquote) {
    border-left: 3px solid var(--primary-main);
    padding-left: 10px;
    margin: 6px 0;
    color: var(--neutral-500);
    opacity: 0.9;
  }
  :deep(a) { color: var(--primary-main); text-decoration: underline; }
  :deep(table) { border-collapse: collapse; margin: 6px 0; width: 100%; font-size: 13px; }
  :deep(th), :deep(td) { border: 1px solid var(--neutral-200); padding: 5px 8px; text-align: left; }
  :deep(th) { background: var(--neutral-75, #f3f4f6); font-weight: 600; }
  :deep(hr) { border: none; border-top: 1px solid var(--neutral-200); margin: 10px 0; }
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .chat-fab {
    bottom: 20px;
    right: 20px;
    width: 50px;
    height: 50px;
  }
  .welcome-cards { grid-template-columns: 1fr; }
  .message-body { max-width: 85%; }
}
</style>

<style lang="scss">
/* 抽屉 body 样式（非 scoped，覆盖 Element Plus） */
.global-chat-drawer {
  .el-drawer__body {
    padding: 0 !important;
    overflow: hidden;
  }
}
</style>
