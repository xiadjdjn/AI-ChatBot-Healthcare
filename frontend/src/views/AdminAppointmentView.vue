<template>
  <div class="appointment-layout">
    <aside class="appointment-sidebar">
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
        <button type="button" class="directory-item active" @click="goToSection('appointment')">
          <span class="directory-icon">约</span>
          <span class="directory-label">用户预约情况</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('chat-session')">
          <span class="directory-icon">聊</span>
          <span class="directory-label">用户会话历史</span>
        </button>
      </div>
    </aside>

    <main class="appointment-main">
      <section class="hero-card panel-card">
        <div>
          <span class="eyebrow">APPOINTMENT CONSOLE</span>
          <h1 class="hero-title">用户预约情况</h1>
          <p class="hero-description">
            管理员可分页查看所有用户预约记录，支持按关键字和状态筛选，按更新时间倒序展示。
          </p>
        </div>
      </section>

      <section class="toolbar-card panel-card">
        <div class="toolbar-row">
          <div class="toolbar-actions">
            <el-button class="action-button action-button-secondary" @click="loadAppointments">
              <el-icon><RefreshRight /></el-icon>
              刷新列表
            </el-button>
          </div>

          <div class="toolbar-filters">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="按账号、就诊人、科室或医生搜索"
              @clear="resetAndLoadAppointments"
              @keyup.enter="resetAndLoadAppointments"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-select
              v-model="filters.status"
              clearable
              placeholder="全部状态"
              @change="resetAndLoadAppointments"
              @clear="resetAndLoadAppointments"
            >
              <el-option label="全部状态" value="" />
              <el-option label="进行中" value="进行中" />
              <el-option label="已取消" value="已取消" />
              <el-option label="已完成" value="已完成" />
            </el-select>

            <el-button type="primary" class="action-button" @click="resetAndLoadAppointments">
              查询
            </el-button>
          </div>
        </div>
      </section>

      <section class="table-card panel-card">
        <div class="table-head">
          <div>
            <div class="table-title">预约记录列表</div>
            <div class="table-subtitle">展示所有用户在系统中的预约记录</div>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="appointments"
          class="appointment-table"
          empty-text="暂无预约记录"
        >
          <el-table-column prop="accountUsername" label="账号" min-width="140">
            <template #default="{ row }">
              <span class="table-name">{{ row.accountUsername || '--' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="patientName" label="就诊人" min-width="120" />

          <el-table-column prop="idCard" label="身份证号" min-width="180" />

          <el-table-column prop="department" label="科室" min-width="120" />

          <el-table-column prop="doctorName" label="医生" min-width="120">
            <template #default="{ row }">
              {{ row.doctorName || '--' }}
            </template>
          </el-table-column>

          <el-table-column prop="date" label="预约日期" min-width="130" />

          <el-table-column prop="time" label="时段" min-width="100" />

          <el-table-column prop="status" label="状态" min-width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="appointmentStatusTagType(row.status)" effect="light" round>
                {{ row.status || '--' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="updatedAt" label="更新时间" min-width="180">
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
import { fetchAdminAppointments } from '@/api/adminAppointment'

const emit = defineEmits(['navigate'])

const loading = ref(false)
const appointments = ref([])
const total = ref(0)
const filters = ref({
  keyword: '',
  status: '',
})
const pagination = ref({
  pageNum: 1,
  pageSize: 10,
})

const goToSection = (section) => {
  emit('navigate', section)
}

const loadAppointments = async () => {
  loading.value = true

  try {
    const payload = await fetchAdminAppointments({
      keyword: filters.value.keyword || undefined,
      status: filters.value.status || undefined,
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
    })
    appointments.value = Array.isArray(payload?.records) ? payload.records : []
    total.value = Number(payload?.total || 0)
  } catch (error) {
    appointments.value = []
    total.value = 0
    ElMessage.error(error?.message || '加载用户预约记录失败')
  } finally {
    loading.value = false
  }
}

const resetAndLoadAppointments = () => {
  pagination.value.pageNum = 1
  loadAppointments()
}

const handlePageChange = (pageNum) => {
  pagination.value.pageNum = pageNum
  loadAppointments()
}

const handlePageSizeChange = (pageSize) => {
  pagination.value.pageSize = pageSize
  pagination.value.pageNum = 1
  loadAppointments()
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

const appointmentStatusTagType = (status) => {
  switch (status) {
    case '进行中':
      return 'success'
    case '已取消':
      return 'info'
    case '已完成':
      return 'warning'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadAppointments()
})
</script>

<style scoped>
.appointment-layout {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(202, 227, 206, 0.42), transparent 28%),
    linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.appointment-sidebar {
  width: 308px;
  padding: 88px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #f8fbf8 0%, #edf3ec 100%);
  border-right: 1px solid #dfe9dd;
  overflow-y: auto;
}

.appointment-main {
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

.toolbar-filters :deep(.el-select) {
  width: 180px;
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

.appointment-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.appointment-table :deep(th.el-table__cell) {
  background: rgba(237, 245, 234, 0.8);
  color: #4d6454;
  font-weight: 700;
}

.appointment-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.appointment-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(243, 251, 241, 0.82);
}

.table-name {
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
  .appointment-layout {
    flex-direction: column;
  }

  .appointment-sidebar {
    width: auto;
    padding-top: 92px;
    border-right: none;
    border-bottom: 1px solid #dfe9dd;
  }

  .appointment-main {
    padding-top: 20px;
  }
}

@media (max-width: 768px) {
  .appointment-main,
  .appointment-sidebar {
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
