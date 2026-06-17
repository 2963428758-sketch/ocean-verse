import http from '@/utils/http'

// 图像识别
export const recognizeImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/ai/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// AI 智能问答（SSE 流式）
// 后端在流结束后追加一条 {"qaId":N} 元数据事件，onMeta 回调用于接收
export const chatWithAIStream = (
  data: { question: string; sessionId?: string; questionType?: string },
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (err: string) => void,
  onMeta?: (meta: { qaId?: number }) => void
) => {
  const token = localStorage.getItem('token')
  fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(data)
  })
    .then(async response => {
      if (!response.ok) throw new Error('请求失败')
      const reader = response.body?.getReader()
      if (!reader) throw new Error('无法读取响应流')
      const decoder = new TextDecoder()

      let buffer = ''
      let finished = false

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          if (!finished) { finished = true; onDone() }
          return
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (finished) return
          processLine(line)
        }
      }

      function processLine(line: string) {
        const trimmed = line.trim()
        if (!trimmed) return

        let content: string | null = null
        if (trimmed.startsWith('data: ')) {
          content = trimmed.slice(6)
        } else if (trimmed.startsWith('data:')) {
          content = trimmed.slice(5)
        }

        if (content === null) return
        // [DONE] / [ERROR] 是未编码的哨兵值，直接检测
        if (content === '[DONE]' || content === '[ERROR]') {
          finished = true
          onDone()
          return
        }

        // 后端用 JSON 编码每个 chunk（\n → \\n），前端解码还原原始文本
        try {
          const parsed = JSON.parse(content)
          // 元数据事件：后端发送 {"qaId":N}，与文本 chunk（字符串）区分开
          if (parsed && typeof parsed === 'object' && 'qaId' in parsed) {
            if (onMeta) onMeta(parsed)
            return
          }
          // 普通文本 chunk（JSON 编码的字符串）
          if (typeof parsed === 'string' && parsed) onChunk(parsed)
        } catch {
          // 降级：如果 JSON 解析失败，直接使用原始内容
          if (content) onChunk(content)
        }
      }
    })
    .catch(err => onError(err.message))
}

// 识别历史
export const getRecognitionHistory = (params: any) =>
  http.get('/ai/recognition/history', { params })

// 问答历史
export const getChatHistory = (params: any) =>
  http.get('/ai/chat/history', { params })

// 清空会话历史（多轮对话重置）
export const clearSession = (sessionId: string) =>
  http.delete(`/ai/chat/session/${sessionId}`)

// 问答反馈：1=满意，0=不满意
export const submitFeedback = (id: number, feedback: number) =>
  http.post(`/ai/chat/feedback/${id}`, null, { params: { feedback } })

// 重建知识库索引（管理员功能）
export const rebuildKnowledgeBase = () =>
  http.post('/ai/knowledge/rebuild')
