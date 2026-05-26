<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="logo-section">
        <img src="@/assets/logo.png" alt="医疗助手小小白" width="96" height="96" />
        <span class="logo-text">医疗助手小小白</span>
      </div>

      <el-button
        class="new-chat-button"
        type="primary"
        :disabled="isSending || isLoadingHistory"
        @click="newChat"
      >
        <i class="fa-solid fa-plus"></i>
        &nbsp;新会话
      </el-button>

      <div class="session-panel">
        <div class="session-panel-title">会话历史</div>

        <div v-if="isLoadingSessions" class="session-status">正在加载会话...</div>

        <div v-else-if="!sessions.length" class="session-status">
          暂无会话，点击上方按钮开始。
        </div>

        <div v-else class="session-list">
          <button
            v-for="session in sessions"
            :key="session.sessionId"
            type="button"
            class="session-item"
            :class="{ active: session.sessionId === activeSessionId }"
            :disabled="isSending || isLoadingHistory"
            @click="selectSession(session.sessionId)"
          >
            <div class="session-item-top">
              <span class="session-title">{{ session.title || '新会话' }}</span>
              <span class="session-time">{{ formatSessionTime(session.updatedAt) }}</span>
            </div>
            <div class="session-preview">
              {{ session.lastMessage || '暂无消息内容' }}
            </div>
          </button>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <div class="chat-container">
        <div class="chat-header">
          <div class="chat-title">{{ currentSessionTitle }}</div>
          <div class="chat-subtitle">{{ currentSessionSummary }}</div>
        </div>

        <div class="message-list" ref="messageListRef">
          <div v-if="!messages.length && !isLoadingHistory" class="empty-state">
            当前会话还没有内容，发送一条消息开始对话。
          </div>

          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="message.isUser ? 'message user-message' : 'message bot-message'"
          >
            <i
              :class="
                message.isUser
                  ? 'fa-solid fa-user message-icon'
                  : 'fa-solid fa-robot message-icon'
              "
            ></i>
            <span class="message-body">
              <span v-html="message.html"></span>
              <span
                v-if="message.isThinking || message.isTyping"
                class="loading-dots"
              >
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </span>
          </div>
        </div>

        <div class="input-container">
          <el-input
            v-model="inputMessage"
            placeholder="请输入消息"
            :disabled="isSending || isLoadingHistory || !activeSessionId"
            @keyup.enter="sendMessage"
          ></el-input>
          <el-button
            type="primary"
            :disabled="isSending || isLoadingHistory || !activeSessionId"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import MarkdownIt from 'markdown-it'

const ACTIVE_SESSION_KEY = 'active_session_id'
const GREETING_MESSAGE = '你好'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const defaultLinkRenderer =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const hrefIndex = tokens[idx].attrIndex('href')
  const href = hrefIndex >= 0 ? tokens[idx].attrs[hrefIndex][1] : ''
  if (!/^(https?:|mailto:)/i.test(href)) {
    if (hrefIndex >= 0) {
      tokens[idx].attrs[hrefIndex][1] = '#'
    } else {
      tokens[idx].attrPush(['href', '#'])
    }
  }
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return defaultLinkRenderer(tokens, idx, options, env, self)
}

const messageListRef = ref()
const isSending = ref(false)
const isLoadingSessions = ref(false)
const isLoadingHistory = ref(false)
const inputMessage = ref('')
const messages = ref([])
const sessions = ref([])
const activeSessionId = ref(null)

const currentSession = computed(() => {
  return sessions.value.find((session) => session.sessionId === activeSessionId.value) || null
})

const currentSessionTitle = computed(() => {
  return currentSession.value?.title || '新会话'
})

const currentSessionSummary = computed(() => {
  if (isLoadingHistory.value) {
    return '正在加载历史记录...'
  }
  return currentSession.value?.lastMessage || '支持按会话查看历史记录'
})

watch(
  messages,
  () => {
    scrollToBottom()
  },
  { deep: true }
)

onMounted(() => {
  initializeChat()
})

const initializeChat = async () => {
  await fetchSessions()

  const storedSessionId = readStoredSessionId()
  const matchedSession = storedSessionId
    ? sessions.value.find((session) => session.sessionId === storedSessionId)
    : null
  const initialSessionId = matchedSession?.sessionId || sessions.value[0]?.sessionId

  if (initialSessionId != null) {
    await selectSession(initialSessionId, { force: true })
    return
  }

  await createSession({ sendGreeting: true })
}

const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const readStoredSessionId = () => {
  const value = localStorage.getItem(ACTIVE_SESSION_KEY)
  if (!value) {
    return null
  }

  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}

const setActiveSessionId = (sessionId) => {
  activeSessionId.value = sessionId
  if (sessionId == null) {
    localStorage.removeItem(ACTIVE_SESSION_KEY)
    return
  }
  localStorage.setItem(ACTIVE_SESSION_KEY, String(sessionId))
}

const fetchSessions = async () => {
  isLoadingSessions.value = true
  try {
    const response = await axios.get('/api/xiaoxiaobai/sessions')
    sessions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.error('加载会话列表失败:', error)
    sessions.value = []
  } finally {
    isLoadingSessions.value = false
  }
}

const createSession = async ({ sendGreeting = false } = {}) => {
  const response = await axios.post('/api/xiaoxiaobai/sessions', {})
  const createdSessionId = response.data?.sessionId

  await fetchSessions()
  messages.value = []

  if (createdSessionId != null) {
    setActiveSessionId(createdSessionId)
  }

  if (sendGreeting && createdSessionId != null) {
    await sendRequest(GREETING_MESSAGE, { displayUserMessage: false })
  }

  return createdSessionId
}

const selectSession = async (sessionId, { force = false } = {}) => {
  if (sessionId == null || isSending.value) {
    return
  }

  if (!force && sessionId === activeSessionId.value) {
    return
  }

  isLoadingHistory.value = true
  try {
    setActiveSessionId(sessionId)
    const response = await axios.get(`/api/xiaoxiaobai/sessions/${sessionId}/history`)
    messages.value = mapHistoryMessages(response.data?.messages)
  } catch (error) {
    console.error('加载会话历史失败:', error)
    messages.value = [
      createMessage({
        content: '加载会话历史失败，请稍后重试。',
        isUser: false,
      }),
    ]
  } finally {
    isLoadingHistory.value = false
  }
}

const ensureActiveSession = async () => {
  if (activeSessionId.value != null) {
    return activeSessionId.value
  }
  return createSession()
}

const newChat = async () => {
  if (isSending.value || isLoadingHistory.value) {
    return
  }

  try {
    await createSession({ sendGreeting: true })
  } catch (error) {
    console.error('创建新会话失败:', error)
  }
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || isSending.value) {
    return
  }

  inputMessage.value = ''
  await sendRequest(message)
}

const sendRequest = async (
  message,
  { displayUserMessage = true } = {}
) => {
  const sessionId = await ensureActiveSession()
  if (sessionId == null) {
    return
  }

  isSending.value = true

  if (displayUserMessage) {
    messages.value.push(
      createMessage({
        content: message,
        isUser: true,
      })
    )
  }

  const botMessage = createMessage({
    content: '',
    isUser: false,
    isTyping: true,
  })
  messages.value.push(botMessage)

  try {
    await axios.post(
      '/api/xiaoxiaobai/chat',
      { memoryId: sessionId, message },
      {
        responseType: 'stream',
        onDownloadProgress: (event) => {
          const fullText = event.event.target.responseText || ''
          const newText = fullText.substring(botMessage.content.length)
          botMessage.content += newText
          botMessage.html = renderMessageContent(botMessage.content, false)
        },
      }
    )
  } catch (error) {
    console.error('流式对话失败:', error)
    botMessage.content = '请求失败，请稍后重试。'
    botMessage.html = renderMessageContent(botMessage.content, false)
  } finally {
    botMessage.isTyping = false
    isSending.value = false
    await fetchSessions()
  }
}

const createMessage = ({
  content,
  isUser,
  isTyping = false,
  isThinking = false,
}) => {
  return {
    isUser,
    content,
    html: renderMessageContent(content, isUser),
    isTyping,
    isThinking,
  }
}

const mapHistoryMessages = (historyMessages) => {
  if (!Array.isArray(historyMessages)) {
    return []
  }

  return historyMessages
    .map((message) => {
      const role = String(message?.role || message?.type || '').toLowerCase()
      if (role.includes('system')) {
        return null
      }

      const content = extractHistoryMessageText(message)
      if (!content) {
        return null
      }

      return createMessage({
        content,
        isUser: role.includes('user'),
      })
    })
    .filter(Boolean)
}

const extractHistoryMessageText = (message) => {
  if (!message || typeof message !== 'object') {
    return ''
  }

  if (typeof message.content === 'string' && message.content.trim()) {
    return message.content
  }

  if (typeof message.text === 'string' && message.text.trim()) {
    return message.text
  }

  if (Array.isArray(message.contents)) {
    const parts = message.contents
      .map((item) => {
        if (typeof item?.text === 'string') {
          return item.text
        }
        return ''
      })
      .filter(Boolean)
    return parts.join('\n').trim()
  }

  return ''
}

const formatSessionTime = (value) => {
  if (!value) {
    return ''
  }

  const normalizedValue = String(value).replace(' ', 'T')
  const date = new Date(normalizedValue)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const escapeHtml = (text) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const renderMessageContent = (text, isUser) => {
  if (isUser) {
    return escapeHtml(text).replace(/\n/g, '<br>')
  }
  return md.render(text || '')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.sidebar {
  width: 300px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #f8fbf8 0%, #edf3ec 100%);
  border-right: 1px solid #dfe9dd;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #2b4634;
}

.new-chat-button {
  width: 100%;
}

.session-panel {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #4a6151;
}

.session-status {
  padding: 14px;
  border-radius: 12px;
  background-color: rgba(255, 255, 255, 0.75);
  color: #6b7f70;
  font-size: 13px;
  line-height: 1.6;
}

.session-list {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  padding-right: 4px;
}

.session-item {
  width: 100%;
  padding: 14px 12px;
  border: 1px solid #d8e6d5;
  border-radius: 14px;
  background-color: rgba(255, 255, 255, 0.9);
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-item:hover:not(:disabled) {
  border-color: #8cb890;
  box-shadow: 0 8px 18px rgba(86, 126, 94, 0.1);
  transform: translateY(-1px);
}

.session-item:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.session-item.active {
  border-color: #6fa173;
  background: linear-gradient(180deg, #f3fbf1 0%, #e5f2e4 100%);
  box-shadow: 0 10px 24px rgba(92, 145, 102, 0.16);
}

.session-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.session-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 700;
  color: #284333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #78907d;
}

.session-preview {
  font-size: 13px;
  line-height: 1.5;
  color: #617366;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.main-content {
  min-width: 0;
  flex: 1;
  padding: 20px;
}

.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-header {
  padding: 18px 20px;
  border: 1px solid #dbe7d8;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 242, 0.94));
}

.chat-title {
  font-size: 20px;
  font-weight: 700;
  color: #284333;
}

.chat-subtitle {
  margin-top: 6px;
  color: #6a7e6f;
  font-size: 13px;
  line-height: 1.6;
}

.message-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #dce8d9;
  border-radius: 18px;
  background-color: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
}

.empty-state {
  margin: auto;
  max-width: 320px;
  text-align: center;
  color: #7d8f81;
  line-height: 1.7;
}

.message {
  max-width: 100%;
  margin-bottom: 12px;
  padding: 14px;
  border-radius: 16px;
  display: flex;
}

.user-message {
  max-width: 72%;
  align-self: flex-end;
  flex-direction: row-reverse;
  background: linear-gradient(180deg, #dff3ff 0%, #d0ebfb 100%);
}

.bot-message {
  max-width: 100%;
  align-self: flex-start;
  background: linear-gradient(180deg, #f2f8ef 0%, #e8f2e2 100%);
}

.message-icon {
  margin: 0 10px;
  font-size: 1.2em;
  color: #4a6151;
}

.message-body {
  flex: 1;
  min-width: 0;
  line-height: 1.7;
  word-break: break-word;
  color: #273229;
}

.message-body :deep(p) {
  margin: 0 0 10px;
}

.message-body :deep(h1),
.message-body :deep(h2),
.message-body :deep(h3),
.message-body :deep(h4),
.message-body :deep(h5),
.message-body :deep(h6) {
  margin: 0 0 12px;
  line-height: 1.4;
}

.message-body :deep(ul),
.message-body :deep(ol) {
  margin: 0 0 10px;
  padding-left: 22px;
}

.message-body :deep(li + li) {
  margin-top: 4px;
}

.message-body :deep(code) {
  padding: 2px 6px;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.06);
  font-family: Consolas, 'Courier New', monospace;
}

.message-body :deep(pre) {
  margin: 0 0 12px;
  padding: 12px;
  overflow-x: auto;
  border-radius: 8px;
  background-color: #1f2937;
  color: #f9fafb;
}

.message-body :deep(pre code) {
  padding: 0;
  background-color: transparent;
  color: inherit;
}

.message-body :deep(blockquote) {
  margin: 0 0 12px;
  padding-left: 12px;
  border-left: 4px solid #8eb894;
  color: #5f6b5f;
}

.message-body :deep(table) {
  width: 100%;
  margin: 0 0 12px;
  border-collapse: collapse;
  background-color: #ffffff;
}

.message-body :deep(th),
.message-body :deep(td) {
  padding: 8px 10px;
  border: 1px solid #d9e2d0;
  text-align: left;
  vertical-align: top;
}

.message-body :deep(th) {
  background-color: #eef6e7;
}

.message-body :deep(a) {
  color: #2f6fed;
  text-decoration: none;
}

.message-body :deep(a:hover) {
  text-decoration: underline;
}

.loading-dots {
  padding-left: 5px;
}

.dot {
  display: inline-block;
  margin-left: 5px;
  width: 8px;
  height: 8px;
  background-color: #516452;
  border-radius: 50%;
  animation: pulse 1.2s infinite ease-in-out both;
}

.dot:nth-child(2) {
  animation-delay: -0.6s;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(0.6);
    opacity: 0.4;
  }

  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.input-container {
  display: flex;
  gap: 10px;
}

.input-container .el-input {
  flex: 1;
}

@media (max-width: 900px) {
  .app-layout {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }

  .sidebar {
    width: auto;
    border-right: none;
    border-bottom: 1px solid #dfe9dd;
  }

  .session-list {
    max-height: 220px;
  }

  .main-content {
    padding: 16px;
  }

  .user-message {
    max-width: 100%;
  }
}
</style>
