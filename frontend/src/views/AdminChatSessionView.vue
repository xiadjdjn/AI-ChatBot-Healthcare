<template>
  <div class="session-layout">
    <aside class="session-sidebar">
      <div class="sidebar-brand panel-card">
        <span class="eyebrow">ADMIN DIRECTORY</span>
        <h2 class="sidebar-title">医疗助手管理台</h2>
      </div>

      <div class="sidebar-nav panel-card">
        <button type="button" class="directory-item" @click="goToSection('knowledge')">
          <span class="directory-icon">库</span>
          <span class="directory-label">知识库管理</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('doctor-duty')">
          <span class="directory-icon">医</span>
          <span class="directory-label">值班医生列表</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('appointment')">
          <span class="directory-icon">约</span>
          <span class="directory-label">用户预约情况</span>
        </button>
        <button type="button" class="directory-item active" @click="goToSection('chat-session')">
          <span class="directory-icon">聊</span>
          <span class="directory-label">用户会话历史</span>
        </button>
      </div>
    </aside>

    <main class="session-main">
      <section class="hero-card panel-card">
        <div>
          <span class="eyebrow">SESSION TITLE CONSOLE</span>
          <h1 class="hero-title">用户会话历史</h1>
          <p class="hero-description">
            管理员可按用户名或昵称查询所有用户的会话标题列表，不展示会话详情内容。
          </p>
        </div>
      </section>

      <section class="toolbar-card panel-card">
        <div class="toolbar-row">
          <div class="toolbar-actions">
            <el-button class="action-button action-button-secondary" @click="loadSessionList">
              <el-icon><RefreshRight /></el-icon>
              刷新列表
            </el-button>
          </div>

          <div class="toolbar-filters">
            <el-input
              v-model="filters.username"
              clearable
              placeholder="按用户名查询"
              @clear="resetAndLoadSessionList"
              @keyup.enter="resetAndLoadSessionList"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-input
              v-model="filters.nickname"
              clearable
              placeholder="按昵称查询"
              @clear="resetAndLoadSessionList"
              @keyup.enter="resetAndLoadSessionList"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-button type="primary" class="action-button" @click="resetAndLoadSessionList">
              查询
            </el-button>
          </div>
        </div>
      </section>

      <section class="table-card panel-card">
        <div class="table-head">
          <div>
            <div class="table-title">会话标题列表</div>
            <div class="table-subtitle">仅展示标题与用户信息，不展示消息详情内容</div>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="sessionList"
          class="session-table"
          empty-text="暂无会话记录"
        >
          <el-table-column prop="username" label="用户名" min-width="140">
            <template #default="{ row }">
              <span class="table-name">{{ row.username || '--' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="nickname" label="昵称" min-width="140">
            <template #default="{ row }">
              {{ row.nickname || '--' }}
            </template>
          </el-table-column>

          <el-table-column prop="title" label="会话标题" min-width="260">
            <template #default="{ row }">
              <span class="session-title-text">{{ row.title || '未命名会话' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column prop="updatedAt" label="更新时间" min-width="170">
            <template #default="{ row }">
              {{ formatTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            background
            layout="sizes, prev, pager, next, total"
            :current-page="pagination.pageNum"
            :page-size="pagination.pageSize"
            :page-sizes="[10, 15, 20]"
            :total="total"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchAdminChatSessions } from '@/api/adminChatSession'

const emit = defineEmits(['navigate'])

const loading = ref(false)
const sessionList = ref([])
const total = ref(0)
const filters = ref({
  username: '',
  nickname: '',
})
const pagination = ref({
  pageNum: 1,
  pageSize: 10,
})

const goToSection = (section) => {
  emit('navigate', section)
}

const loadSessionList = async () => {
  loading.value = true

  try {
    const payload = await fetchAdminChatSessions({
      username: filters.value.username || undefined,
      nickname: filters.value.nickname || undefined,
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
    })
    sessionList.value = Array.isArray(payload?.records) ? payload.records : []
    total.value = Number(payload?.total || 0)
  } catch (error) {
    sessionList.value = []
    total.value = 0
    ElMessage.error(error?.message || '加载用户会话历史失败')
  } finally {
    loading.value = false
  }
}

const resetAndLoadSessionList = () => {
  pagination.value.pageNum = 1
  loadSessionList()
}

const handlePageChange = (pageNum) => {
  pagination.value.pageNum = pageNum
  loadSessionList()
}

const handlePageSizeChange = (pageSize) => {
  pagination.value.pageSize = pageSize
  pagination.value.pageNum = 1
  loadSessionList()
}

const formatTime = (value) => {
  if (!value) {
    return '--'
  }

  const normalizedValue = String(value).replace(' ', 'T')
  const date = new Date(normalizedValue)

  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  loadSessionList()
})
</script>

<style scoped>
.session-layout {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(202, 227, 206, 0.42), transparent 28%),
    linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.session-sidebar {
  width: 308px;
  padding: 88px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #f8fbf8 0%, #edf3ec 100%);
  border-right: 1px solid #dfe9dd;
  overflow-y: auto;
}

.session-main {
  min-width: 0;
  flex: 1;
  padding: 88px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.panel-card {
  border: 1px solid #dbe7d8;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 242, 0.94));
  box-shadow: 0 20px 45px rgba(96, 130, 103, 0.08);
}

.sidebar-brand,
.sidebar-nav {
  padding: 22px 20px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: #6d8472;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sidebar-title {
  margin: 0;
  color: #284333;
  font-size: 28px;
  line-height: 1.1;
}

.sidebar-nav {
  display: grid;
  gap: 10px;
}

.directory-item {
  display: grid;
  grid-template-columns: 28px 1fr;
  align-items: center;
  min-height: 52px;
  padding: 0 16px;
  border: 1px solid #d8e6d5;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.76);
  color: #486a5a;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.directory-item:hover {
  border-color: #8cb890;
  box-shadow: 0 8px 18px rgba(86, 126, 94, 0.1);
  transform: translateY(-1px);
}

.directory-item.active {
  background: linear-gradient(180deg, #f3fbf1 0%, #e5f2e4 100%);
  border-color: #6fa173;
  box-shadow: 0 10px 24px rgba(92, 145, 102, 0.16);
  color: #1f4031;
}

.directory-icon {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 1px solid currentColor;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1;
}

.directory-label {
  font-size: 16px;
  font-weight: 800;
}

.hero-card {
  padding: 24px;
}

.hero-title {
  margin: 0;
  color: #21352a;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.05;
  letter-spacing: -0.03em;
}

.hero-description {
  margin: 12px 0 0;
  color: #627668;
  font-size: 14px;
  line-height: 1.7;
}

.toolbar-card,
.table-card {
  padding: 18px;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-actions,
.toolbar-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-filters {
  width: min(100%, 720px);
}

.toolbar-filters :deep(.el-input) {
  flex: 1;
  min-width: 220px;
}

.action-button {
  min-height: 42px;
  padding-inline: 16px;
  border-radius: 14px;
  font-weight: 700;
}

.action-button-secondary {
  border-color: #d6e3d5;
  color: #31503a;
}

.table-head {
  margin-bottom: 14px;
}

.table-title {
  color: #2d4334;
  font-size: 16px;
  font-weight: 800;
}

.table-subtitle {
  margin-top: 6px;
  color: #708274;
  font-size: 13px;
}

.session-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.session-table :deep(th.el-table__cell) {
  background: rgba(237, 245, 234, 0.8);
  color: #4d6454;
  font-weight: 700;
}

.session-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.session-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(243, 251, 241, 0.82);
}

.table-name,
.session-title-text {
  color: #284333;
  font-size: 14px;
  font-weight: 700;
}

.table-pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .session-layout {
    flex-direction: column;
  }

  .session-sidebar {
    width: auto;
    padding-top: 92px;
    border-right: none;
    border-bottom: 1px solid #dfe9dd;
  }

  .session-main {
    padding-top: 20px;
  }
}

@media (max-width: 768px) {
  .session-main,
  .session-sidebar {
    padding-left: 14px;
    padding-right: 14px;
  }

  .toolbar-actions,
  .toolbar-filters {
    width: 100%;
  }

  .toolbar-actions > * {
    flex: 1;
  }
}
</style>
