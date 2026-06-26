import { getToken } from './auth'
import request, { API_BASE_URL } from './request'

const parseStreamError = async (response) => {
  try {
    const payload = await response.json()
    return payload?.message || response.statusText
  } catch {
    return response.statusText
  }
}

export const fetchSessions = (params = {}) => {
  return request.get('/xiaoxiaobai/sessions', { params })
}

export const createSession = (data = {}) => {
  return request.post('/xiaoxiaobai/sessions', data)
}

export const fetchSessionHistory = (sessionId) => {
  return request.get(`/xiaoxiaobai/sessions/${sessionId}/history`)
}

export const deleteSessionById = (sessionId) => {
  return request.delete(`/xiaoxiaobai/sessions/${sessionId}`)
}

export const sendChatMessage = async (sessionId, message) => {
  const response = await fetch(`${API_BASE_URL}/xiaoxiaobai/chat`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${getToken()}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      memoryId: sessionId,
      message,
    }),
  })

  if (response.status === 401) {
    window.dispatchEvent(new Event('auth:expired'))
    throw new Error(await parseStreamError(response))
  }

  if (!response.ok) {
    throw new Error(await parseStreamError(response))
  }

  if (!response.body) {
    return { content: await response.text(), references: [] }
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let content = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    content += decoder.decode(value, { stream: true })
  }

  content += decoder.decode()

  return { content, references: [] }
}
