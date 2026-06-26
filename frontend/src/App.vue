<template>
  <LoginView v-if="!isAuthenticated" @login-success="handleLoginSuccess" />

  <div v-else class="app-shell">
    <transition name="view-fade" mode="out-in">
      <KnowledgeView
        v-if="isAdminUser && adminSection === 'knowledge'"
        key="knowledge"
        @navigate="handleAdminNavigate"
      />
      <DoctorDutyView
        v-else-if="isAdminUser && adminSection === 'doctor-duty'"
        key="doctor-duty"
        @navigate="handleAdminNavigate"
      />
      <AdminAppointmentView
        v-else-if="isAdminUser && adminSection === 'appointment'"
        key="appointment"
        @navigate="handleAdminNavigate"
      />
      <AdminChatSessionView
        v-else-if="isAdminUser && adminSection === 'chat-session'"
        key="chat-session"
        @navigate="handleAdminNavigate"
      />
      <ProfileView v-else-if="isProfileView" key="profile" />
      <ChatWindow v-else key="chat" />
    </transition>

    <nav v-if="!isAdminUser" class="user-nav" aria-label="普通用户导航">
      <button
        type="button"
        class="user-nav-button"
        @click="toggleProfileView"
      >
        {{ isProfileView ? '返回对话' : '个人中心' }}
      </button>
    </nav>

    <nav class="user-bar" aria-label="用户信息">
      <span class="user-name">{{ currentUserName }}</span>
      <button
        type="button"
        class="user-bar-button logout-button"
        @click="logout"
      >
        退出
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { clearAuth, getStoredUser, getToken } from '@/api/auth'
import ChatWindow from '@/components/ChatWindow.vue'
import KnowledgeView from '@/views/KnowledgeView.vue'
import DoctorDutyView from '@/views/DoctorDutyView.vue'
import AdminAppointmentView from '@/views/AdminAppointmentView.vue'
import AdminChatSessionView from '@/views/AdminChatSessionView.vue'
import LoginView from '@/views/LoginView.vue'
import ProfileView from '@/views/ProfileView.vue'

const isAuthenticated = ref(Boolean(getToken()))
const currentUser = ref(getStoredUser())
const isProfileView = ref(false)
const adminSection = ref('knowledge')

const getUserField = (user, keys, fallback = '') => {
  const sources = [user, user?.user, user?.profile]

  for (const source of sources) {
    if (!source || typeof source !== 'object') {
      continue
    }

    for (const key of keys) {
      const value = source[key]
      if (value !== undefined && value !== null && value !== '') {
        return value
      }
    }
  }

  return fallback
}

const currentUserName = computed(() => {
  return getUserField(
    currentUser.value,
    ['name', 'nickname', 'displayName', 'realName', 'username', 'account'],
    '当前用户'
  )
})

const isAdminUser = computed(() => {
  if (getUserField(currentUser.value, ['isAdmin'], false) === true) {
    return true
  }

  const rawRole = getUserField(currentUser.value, ['role', 'roles', 'userType', 'type'], '')
  const roles = Array.isArray(rawRole) ? rawRole : [rawRole]

  return roles.some((role) => {
    const normalizedRole = String(role).toLowerCase()
    return ['admin', 'administrator', 'manager', '管理员'].some((keyword) => {
      return normalizedRole.includes(keyword)
    })
  })
})

const handleLoginSuccess = (user) => {
  currentUser.value = user
  isAuthenticated.value = true
  isProfileView.value = false
  adminSection.value = 'knowledge'
}

const logout = () => {
  clearAuth()
  currentUser.value = null
  isAuthenticated.value = false
  isProfileView.value = false
  adminSection.value = 'knowledge'
}

const toggleProfileView = () => {
  isProfileView.value = !isProfileView.value
}

const handleAdminNavigate = (section) => {
  if (section === 'knowledge' || section === 'doctor-duty' || section === 'appointment' || section === 'chat-session') {
    adminSection.value = section
  }
}

onMounted(() => {
  window.addEventListener('auth:expired', logout)
})

onBeforeUnmount(() => {
  window.removeEventListener('auth:expired', logout)
})
</script>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
}

.user-bar {
  position: fixed;
  top: 18px;
  right: 20px;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 10px 16px;
}

.user-nav {
  position: fixed;
  left: 20px;
  bottom: 18px;
  z-index: 1200;
}

.user-nav-button {
  min-width: 104px;
  padding: 10px 16px;
  border: 1px solid #cfe0cb;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  color: #31513b;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(80, 116, 86, 0.14);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.user-nav-button:hover {
  border-color: #8cb890;
  box-shadow: 0 14px 28px rgba(80, 116, 86, 0.18);
  transform: translateY(-1px);
}

.user-name {
  max-width: 180px;
  padding: 0 8px 0 10px;
  color: #2d4334;
  font-size: 14px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-bar-button {
  padding: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 700;
  transition:
    color 0.2s ease,
    opacity 0.2s ease;
}

.user-bar-button:hover {
  opacity: 0.72;
}

.logout-button {
  color: #7a3d3d;
}

.view-fade-enter-active,
.view-fade-leave-active {
  transition: opacity 0.22s ease;
}

.view-fade-enter-from,
.view-fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .user-bar {
    top: 14px;
    right: 14px;
    padding: 8px 10px;
  }

  .user-nav {
    left: 14px;
    bottom: 14px;
  }

  .user-name {
    max-width: calc(100vw - 150px);
  }
}
</style>
