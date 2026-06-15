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
    .then(response => {
      if (!response.ok) throw new Error('请求失败')
      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      function read() {
        reader?.read().then(({ done, value }) => {
          if (done) { onDone(); return }
          const text = decoder.decode(value)
          const lines = text.split('\n')
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const content = line.slice(6)
              if (content === '[DONE]') { onDone(); return }
              onChunk(content)
            }
          }
          read()
        })
      }
      read()
    })
    .catch(err => onError(err.message))
}

// 识别历史
export const getRecognitionHistory = (params: any) =>
  http.get('/ai/recognition/history', { params })

// 问答历史
export const getChatHistory = (params: any) =>
  http.get('/ai/chat/history', { params })
