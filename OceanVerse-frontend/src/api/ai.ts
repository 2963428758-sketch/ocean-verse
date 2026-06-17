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
export const chatWithAIStream = (
  data: { question: string; sessionId?: string },
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (err: string) => void
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
          const text = JSON.parse(content)
          if (text) onChunk(text)
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
