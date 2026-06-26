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
        :disabled="isBusy"
        @click="newChat"
      >
        <i class="fa-solid fa-plus"></i>
        &nbsp;新会话
      </el-button>

      <div class="session-panel">
        <div class="session-panel-header">
          <div class="session-panel-title">会话历史</div>
          <button
            type="button"
            class="session-search-trigger"
            :disabled="isBusy"
            aria-label="搜索会话"
            @click="openSessionSearchDialog"
          >
            <el-icon><Search /></el-icon>
          </button>
        </div>

        <div v-if="isLoadingSessions" class="session-status">正在加载会话...</div>

        <div v-else-if="!sessions.length" class="session-status">
          暂无会话，点击上方按钮开始。
        </div>

        <div v-else class="session-list">
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: session.sessionId === activeSessionId }"
          >
            <button
              type="button"
              class="session-delete-button"
              :disabled="isBusy"
              aria-label="删除会话"
              @click.stop="openDeleteDialog(session)"
            >
              ×
            </button>

            <button
              type="button"
              class="session-select-button"
              :disabled="isBusy"
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
      </div>
    </aside>

    <main class="main-content">
      <div class="chat-container">
        <div class="chat-header">
          <div class="chat-title">{{ currentSessionTitle }}</div>
          <div class="chat-subtitle">{{ currentSessionSummary }}</div>
        </div>

        <div ref="messageListRef" class="message-list">
          <div v-if="showWelcomePanel" class="welcome-panel">
            <div class="welcome-media">
              <img
                :src="hospitalImageUrl"
                alt="北京协和医院建筑外观"
                loading="lazy"
              />
            </div>
            <div class="welcome-content">
              <span class="welcome-eyebrow">医疗助手小小白</span>
              <h1>智能预约与就诊咨询助手</h1>
              <p>
                本项目可以帮助用户通过自然语言完成预约咨询、预约挂号信息确认，并在个人中心查看历史预约记录。
              </p>
              <div class="hospital-info-grid">
                <div class="hospital-info-item">
                  <span>医院位置</span>
                  <strong>东单院区：北京市东城区帅府园一号。</strong>
                  <strong>西单院区：北京市西城区大木仓胡同 41 号。</strong>
                </div>
                <div class="hospital-info-item">
                  <span>开放时间</span>
                  <strong>门诊工作日 8:00 - 17:30，急诊 24 小时开放。</strong>
                </div>
                <div class="hospital-info-item">
                  <span>可用能力</span>
                  <strong>点击左侧“新会话”开始预约；点击会话历史可查看对应对话详情。</strong>
                </div>
                <div class="hospital-info-item">
                  <span>图片来源</span>
                  <strong>北京协和医院航拍图，来源：新华社 / 凤凰网图片。</strong>
                </div>
              </div>

              <div class="duty-panel">
                <div class="duty-panel-header">
                  <div>
                    <div class="duty-panel-title">当前值班医生</div>
                    <div class="duty-panel-subtitle">默认按当前时间展示 {{ currentDutySlot }} 值班医生</div>
                  </div>
                  <div class="duty-slot-switch">
                    <button
                      type="button"
                      class="duty-slot-button"
                      :class="{ active: currentDutySlot === '上午' }"
                      @click="loadCurrentDutyDoctors('上午')"
                    >
                      上午
                    </button>
                    <button
                      type="button"
                      class="duty-slot-button"
                      :class="{ active: currentDutySlot === '下午' }"
                      @click="loadCurrentDutyDoctors('下午')"
                    >
                      下午
                    </button>
                  </div>
                </div>

                <div v-if="currentDutyLoading" class="duty-loading">正在加载值班医生...</div>
                <div v-else-if="!currentDutyDoctors.length" class="duty-empty">
                  当前时段暂无可展示的值班医生
                </div>
                <div v-else class="duty-grid">
                  <article v-for="doctor in currentDutyDoctors" :key="doctor.id" class="duty-card">
                    <div class="duty-card-head">
                      <span class="duty-name">{{ doctor.doctorName }}</span>
                      <span class="duty-tag">{{ currentDutySlot }}</span>
                    </div>
                    <div class="duty-meta">{{ doctor.department }} · {{ doctor.title }}</div>
                    <div class="duty-specialty">{{ doctor.specialty }}</div>
                  </article>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="!messages.length && !isLoadingHistory" class="empty-state">
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

            <div class="message-content">
              <span class="message-body">
                <span v-html="message.html"></span>
                <span v-if="message.isThinking || message.isTyping" class="loading-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                </span>
              </span>

              <div
                v-if="!message.isUser && message.references.length"
                class="message-references"
              >
                <div class="message-references-title">来源</div>
                <div class="reference-list">
                  <span
                    v-for="reference in message.references"
                    :key="reference"
                    class="reference-tag"
                  >
                    {{ reference }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="input-container">
          <el-input
            v-model="inputMessage"
            placeholder="请输入消息"
            :disabled="isBusy || !activeSessionId"
            @keyup.enter="sendMessage"
          ></el-input>
          <el-button
            type="primary"
            :disabled="isBusy || !activeSessionId"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </div>
    </main>

    <div v-if="isDeleteDialogVisible" class="dialog-mask" @click="closeDeleteDialog">
      <div class="delete-dialog" @click.stop>
        <div class="delete-dialog-title">删除后，该对话将不可恢复</div>
        <div class="delete-dialog-description">
          由该对话生成的分享链接也将失效。
        </div>
        <div class="delete-dialog-actions">
          <button
            type="button"
            class="dialog-button dialog-button-cancel"
            :disabled="isDeletingSession"
            @click="closeDeleteDialog"
          >
            取消
          </button>
          <button
            type="button"
            class="dialog-button dialog-button-danger"
            :disabled="isDeletingSession"
            @click="deleteSession"
          >
            {{ isDeletingSession ? '删除中...' : '删除该对话' }}
          </button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="isSessionSearchDialogVisible"
      title="搜索对话"
      width="520px"
      destroy-on-close
      @opened="focusSessionSearchInput"
    >
      <div class="session-search-dialog">
        <el-input
          ref="sessionSearchInputRef"
          v-model="sessionKeyword"
          clearable
          placeholder="输入用户消息关键词"
          :disabled="isBusy"
          @clear="handleSessionSearchClear"
          @keyup.enter="handleSessionSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button :loading="isSearchingSessions" @click="handleSessionSearch">
              搜索
            </el-button>
          </template>
        </el-input>

        <div v-if="isSearchingSessions" class="session-search-empty">
          正在搜索会话...
        </div>

        <div v-else-if="sessionKeyword.trim() && !searchSessions.length" class="session-search-empty">
          没有匹配的对话记录
        </div>

        <div v-else-if="searchSessions.length" class="session-search-results">
          <button
            v-for="session in searchSessions"
            :key="session.sessionId"
            type="button"
            class="session-search-result"
            @click="selectSearchedSession(session)"
          >
            <div class="session-search-result-top">
              <span class="session-search-result-title">
                {{ session.title || '新会话' }}
              </span>
              <span class="session-search-result-time">
                {{ formatSessionTime(session.updatedAt) }}
              </span>
            </div>
            <div class="session-search-result-preview">
              {{ session.lastMessage || '暂无消息内容' }}
            </div>
          </button>
        </div>

        <div v-else class="session-search-empty">
          输入关键词后，可按用户消息内容搜索历史对话
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import { Search } from '@element-plus/icons-vue'
import {
  createSession as createSessionRequest,
  deleteSessionById,
  fetchSessionHistory,
  fetchSessions as fetchSessionsRequest,
  sendChatMessage,
} from '@/api/chat'
import { fetchCurrentDoctorDuties } from '@/api/doctorDuty'

const ACTIVE_SESSION_KEY = 'active_session_id'
const hospitalImageUrl = 'https://x0.ifengimg.com/ucms/2025_09/EE88D126E48D07032864996BFDE02C2002D36AF1_size198_w1920_h1080.jpg'

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
const sessionSearchInputRef = ref(null)
const isSending = ref(false)
const isLoadingSessions = ref(false)
const isLoadingHistory = ref(false)
const isDeletingSession = ref(false)
const isDeleteDialogVisible = ref(false)
const isSessionSearchDialogVisible = ref(false)
const isSearchingSessions = ref(false)
const pendingDeleteSession = ref(null)
const inputMessage = ref('')
const sessionKeyword = ref('')
const currentDutySlot = ref(new Date().getHours() < 12 ? '上午' : '下午')
const currentDutyDoctors = ref([])
const currentDutyLoading = ref(false)
const messages = ref([])
const sessions = ref([])
const searchSessions = ref([])
const activeSessionId = ref(null)

const isBusy = computed(() => {
  return isSending.value || isLoadingHistory.value || isDeletingSession.value
})

const showWelcomePanel = computed(() => {
  return activeSessionId.value == null && !isLoadingHistory.value
})

const currentSession = computed(() => {
  return sessions.value.find((session) => session.sessionId === activeSessionId.value) || null
})

const currentSessionTitle = computed(() => {
  if (activeSessionId.value == null) {
    return '医疗预约助手'
  }

  return currentSession.value?.title || '新会话'
})

const currentSessionSummary = computed(() => {
  if (isLoadingHistory.value) {
    return '正在加载历史记录...'
  }

  if (activeSessionId.value == null) {
    return '点击左侧“新会话”开始预约，或点击历史会话查看详情'
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
  loadCurrentDutyDoctors(currentDutySlot.value)
})

const initializeChat = async () => {
  await loadSessions()
  setActiveSessionId(null)
  messages.value = []
}

const normalizeDutyDoctor = (doctor) => {
  return {
    id: doctor?.id,
    doctorName: doctor?.doctorName || '',
    department: doctor?.department || '',
    title: doctor?.title || '',
    specialty: doctor?.specialty || '',
  }
}

const loadCurrentDutyDoctors = async (slot = currentDutySlot.value) => {
  currentDutySlot.value = slot === '下午' ? '下午' : '上午'
  currentDutyLoading.value = true

  try {
    const data = await fetchCurrentDoctorDuties({ slot: currentDutySlot.value })
    currentDutyDoctors.value = Array.isArray(data) ? data.map(normalizeDutyDoctor) : []
  } catch (error) {
    console.error('加载当前值班医生失败:', error)
    currentDutyDoctors.value = []
  } finally {
    currentDutyLoading.value = false
  }
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

const loadSessions = async () => {
  isLoadingSessions.value = true
  try {
    sessions.value = await fetchSessionsRequest()
  } catch (error) {
    console.error('加载会话列表失败:', error)
    sessions.value = []
  } finally {
    isLoadingSessions.value = false
  }
}

const createSession = async () => {
  const response = await createSessionRequest({})
  const createdSessionId = response?.sessionId

  sessionKeyword.value = ''
  searchSessions.value = []
  await loadSessions()
  messages.value = []

  if (createdSessionId != null) {
    setActiveSessionId(createdSessionId)
  }

  return createdSessionId
}

const selectSession = async (sessionId, { force = false } = {}) => {
  if (sessionId == null || isSending.value || isDeletingSession.value) {
    return
  }

  if (!force && sessionId === activeSessionId.value) {
    return
  }

  isLoadingHistory.value = true
  try {
    setActiveSessionId(sessionId)
    const response = await fetchSessionHistory(sessionId)
    messages.value = mapHistoryMessages(response?.messages)
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

  return null
}

const newChat = async () => {
  if (isBusy.value) {
    return
  }

  try {
    await createSession()
  } catch (error) {
    console.error('创建新会话失败:', error)
  }
}

const openDeleteDialog = (session) => {
  if (isBusy.value) {
    return
  }

  pendingDeleteSession.value = session
  isDeleteDialogVisible.value = true
}

const closeDeleteDialog = ({ force = false } = {}) => {
  if (isDeletingSession.value && !force) {
    return
  }

  pendingDeleteSession.value = null
  isDeleteDialogVisible.value = false
}

const deleteSession = async () => {
  const session = pendingDeleteSession.value
  if (!session?.sessionId) {
    return
  }

  isDeletingSession.value = true

  try {
    await deleteSessionById(session.sessionId)

    const deletedSessionId = session.sessionId
    const deletedActiveSession = activeSessionId.value === deletedSessionId
    const remainingSessions = sessions.value.filter(
      (item) => item.sessionId !== deletedSessionId
    )

    sessions.value = remainingSessions
    closeDeleteDialog({ force: true })

    if (!remainingSessions.length) {
      setActiveSessionId(null)
      messages.value = []
      await loadSessions()
      return
    }

    if (deletedActiveSession) {
      setActiveSessionId(null)
      messages.value = []
    }

    await loadSessions()
  } catch (error) {
    console.error('删除会话失败:', error)
  } finally {
    isDeletingSession.value = false
  }
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || isBusy.value) {
    return
  }

  inputMessage.value = ''
  await sendRequest(message)
}

const sendRequest = async (message) => {
  const sessionId = await ensureActiveSession()
  if (sessionId == null) {
    return
  }

  isSending.value = true

  messages.value.push(
    createMessage({
      content: message,
      isUser: true,
    })
  )

  const botMessage = createMessage({
    content: '',
    isUser: false,
    isTyping: true,
    references: [],
  })
  messages.value.push(botMessage)

  try {
    await streamChatResponse(sessionId, message, botMessage)
    await refreshCurrentSessionMessages(sessionId)
  } catch (error) {
    console.error('流式对话失败:', error)
    botMessage.content = '请求失败，请稍后重试。'
    botMessage.html = renderMessageContent(botMessage.content, false)
    botMessage.references = []
  } finally {
    botMessage.isTyping = false
    isSending.value = false
    await loadSessions()
  }
}

const streamChatResponse = async (sessionId, message, botMessage) => {
  const response = await sendChatMessage(sessionId, message)
  botMessage.content = response?.content || ''
  botMessage.references = response?.references || []
  botMessage.html = renderMessageContent(botMessage.content, false)
}

const refreshCurrentSessionMessages = async (sessionId) => {
  try {
    const response = await fetchSessionHistory(sessionId)
    const latestMessages = mapHistoryMessages(response?.messages)
    if (latestMessages.length) {
      messages.value = latestMessages
    }
  } catch (error) {
    console.error('刷新消息显示失败:', error)
  }
}

const createMessage = ({
  content,
  isUser,
  isTyping = false,
  isThinking = false,
  references = [],
}) => {
  return {
    isUser,
    content,
    html: renderMessageContent(content, isUser),
    isTyping,
    isThinking,
    references: Array.isArray(references) ? references : [],
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
        references: message?.references,
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

const handleSessionSearch = async () => {
  if (isBusy.value) {
    return
  }

  const keyword = sessionKeyword.value.trim()
  if (!keyword) {
    searchSessions.value = []
    return
  }

  isSearchingSessions.value = true

  try {
    searchSessions.value = await fetchSessionsRequest({ keyword })
  } catch (error) {
    console.error('搜索会话失败:', error)
    searchSessions.value = []
  } finally {
    isSearchingSessions.value = false
  }
}

const handleSessionSearchClear = () => {
  searchSessions.value = []
}

const openSessionSearchDialog = () => {
  if (isBusy.value) {
    return
  }

  sessionKeyword.value = ''
  searchSessions.value = []
  isSessionSearchDialogVisible.value = true
}

const focusSessionSearchInput = async () => {
  await nextTick()
  sessionSearchInputRef.value?.focus?.()
}

const selectSearchedSession = async (session) => {
  if (!session?.sessionId) {
    return
  }

  isSessionSearchDialogVisible.value = false
  sessionKeyword.value = ''
  searchSessions.value = []

  await loadSessions()
  await selectSession(session.sessionId, { force: true })
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

.session-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.session-panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #4a6151;
}

.session-search-trigger {
  width: 34px;
  height: 34px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(216, 230, 213, 0.9);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  color: #4a6151;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    color 0.2s ease;
}

.session-search-trigger:hover:not(:disabled) {
  border-color: #8cb890;
  background: #f6fbf5;
  color: #2b4634;
  box-shadow: 0 8px 18px rgba(86, 126, 94, 0.1);
}

.session-search-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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
  position: relative;
  border: 1px solid #d8e6d5;
  border-radius: 14px;
  background-color: rgba(255, 255, 255, 0.9);
  transition: all 0.2s ease;
}

.session-item:hover {
  border-color: #8cb890;
  box-shadow: 0 8px 18px rgba(86, 126, 94, 0.1);
  transform: translateY(-1px);
}

.session-item.active {
  border-color: #6fa173;
  background: linear-gradient(180deg, #f3fbf1 0%, #e5f2e4 100%);
  box-shadow: 0 10px 24px rgba(92, 145, 102, 0.16);
}

.session-select-button {
  width: 100%;
  padding: 14px 12px 14px 42px;
  border: none;
  border-radius: 14px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.session-select-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.session-delete-button {
  position: absolute;
  top: 12px;
  left: 10px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 77, 79, 0.12);
  color: #ff4d4f;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-delete-button:hover:not(:disabled) {
  background: #ff4d4f;
  color: #ffffff;
}

.session-delete-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.96),
    rgba(244, 249, 242, 0.94)
  );
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

.welcome-panel {
  width: min(980px, 100%);
  margin: auto;
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.1fr);
  gap: 24px;
  align-items: stretch;
}

.welcome-media {
  min-height: 360px;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid #dbe7d8;
  background: #eef5ed;
}

.welcome-media img {
  width: 100%;
  height: 100%;
  min-height: 360px;
  display: block;
  object-fit: cover;
}

.welcome-content {
  padding: 28px;
  border: 1px solid #dbe7d8;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 242, 0.94));
}

.welcome-eyebrow {
  display: inline-block;
  margin-bottom: 12px;
  color: #63806a;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.welcome-content h1 {
  margin: 0;
  color: #233a2c;
  font-size: 28px;
  line-height: 1.25;
}

.welcome-content p {
  margin: 14px 0 0;
  color: #5f7465;
  font-size: 15px;
  line-height: 1.8;
}

.hospital-info-grid {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.hospital-info-item {
  padding: 15px 16px;
  border: 1px solid #dce9d8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
}

.hospital-info-item span {
  display: block;
  color: #78907d;
  font-size: 12px;
  font-weight: 700;
}

.hospital-info-item strong {
  display: block;
  margin-top: 6px;
  color: #2d4334;
  font-size: 14px;
  line-height: 1.7;
}

.duty-panel {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid #dbe7d8;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
}

.duty-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.duty-panel-title {
  color: #233a2c;
  font-size: 16px;
  font-weight: 800;
}

.duty-panel-subtitle {
  margin-top: 4px;
  color: #738679;
  font-size: 12px;
  line-height: 1.5;
}

.duty-slot-switch {
  display: flex;
  gap: 8px;
}

.duty-slot-button {
  min-width: 60px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d6e3d5;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #3d5848;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.duty-slot-button.active {
  border-color: #6fa173;
  background: #eef8ec;
}

.duty-loading,
.duty-empty {
  margin-top: 14px;
  color: #6a7e6f;
  font-size: 13px;
  line-height: 1.7;
}

.duty-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.duty-card {
  padding: 14px 16px;
  border: 1px solid #dce9d8;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
}

.duty-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.duty-name {
  color: #284333;
  font-size: 14px;
  font-weight: 800;
}

.duty-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(111, 161, 115, 0.16);
  color: #45624d;
  font-size: 12px;
  font-weight: 700;
}

.duty-meta {
  margin-top: 8px;
  color: #516452;
  font-size: 13px;
  line-height: 1.6;
}

.duty-specialty {
  margin-top: 6px;
  color: #6a7e6f;
  font-size: 12px;
  line-height: 1.6;
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

.message-content {
  flex: 1;
  min-width: 0;
}

.message-body {
  display: block;
  min-width: 0;
  line-height: 1.7;
  word-break: break-word;
  color: #273229;
}

.message-references {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(95, 125, 103, 0.24);
}

.message-references-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #5e7564;
}

.reference-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reference-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(111, 161, 115, 0.14);
  color: #34553c;
  font-size: 12px;
  line-height: 1.4;
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

.dialog-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(31, 41, 55, 0.28);
  backdrop-filter: blur(8px);
  z-index: 1000;
}

.delete-dialog {
  width: min(560px, calc(100vw - 32px));
  padding: 34px 34px 26px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
}

.delete-dialog-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.4;
}

.delete-dialog-description {
  margin-top: 18px;
  color: #5b6474;
  font-size: 16px;
  line-height: 1.7;
}

.delete-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 34px;
}

.dialog-button {
  min-width: 108px;
  padding: 14px 26px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dialog-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.dialog-button-cancel {
  border: 1px solid #d8dde6;
  background: #ffffff;
  color: #293241;
}

.dialog-button-cancel:hover:not(:disabled) {
  border-color: #bcc5d1;
  background: #f8fafc;
}

.dialog-button-danger {
  border: 1px solid #ff3b30;
  background: #ff3b30;
  color: #ffffff;
  box-shadow: 0 10px 24px rgba(255, 59, 48, 0.22);
}

.dialog-button-danger:hover:not(:disabled) {
  background: #f33228;
  border-color: #f33228;
}

.session-search-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.session-search-empty {
  padding: 16px;
  border-radius: 16px;
  background: rgba(245, 249, 243, 0.9);
  color: #6b7f70;
  font-size: 13px;
  line-height: 1.7;
}

.session-search-results {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.session-search-result {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #d8e6d5;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  text-align: left;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.session-search-result:hover {
  border-color: #8cb890;
  box-shadow: 0 8px 18px rgba(86, 126, 94, 0.1);
  transform: translateY(-1px);
}

.session-search-result-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-search-result-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 700;
  color: #284333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-search-result-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #78907d;
}

.session-search-result-preview {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #617366;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
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

  .welcome-panel {
    grid-template-columns: 1fr;
  }

  .duty-grid {
    grid-template-columns: 1fr;
  }

  .welcome-media,
  .welcome-media img {
    min-height: 220px;
  }
}
</style>
