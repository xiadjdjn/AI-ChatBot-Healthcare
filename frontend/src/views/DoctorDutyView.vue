<template>
  <div class="doctor-layout">
    <aside class="doctor-sidebar">
      <div class="sidebar-brand panel-card">
        <span class="eyebrow">ADMIN DIRECTORY</span>
        <h2 class="sidebar-title">医疗助手管理台</h2>
      </div>

      <div class="sidebar-nav panel-card">
        <button type="button" class="directory-item" @click="goToSection('knowledge')">
          <span class="directory-icon">库</span>
          <span class="directory-label">知识库管理</span>
        </button>
        <button type="button" class="directory-item active" @click="goToSection('doctor-duty')">
          <span class="directory-icon">医</span>
          <span class="directory-label">值班医生列表</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('appointment')">
          <span class="directory-icon">约</span>
          <span class="directory-label">用户预约情况</span>
        </button>
        <button type="button" class="directory-item" @click="goToSection('chat-session')">
          <span class="directory-icon">聊</span>
          <span class="directory-label">用户会话历史</span>
        </button>
      </div>
    </aside>

    <main class="doctor-main">
      <section class="hero-card panel-card">
        <div class="hero-copy">
          <span class="eyebrow">DOCTOR DUTY CONSOLE</span>
          <h1 class="hero-title">值班医生列表</h1>
          <p class="hero-description">
            管理医生资料，并调整上午/下午值班状态，用户端只展示当前时段值班医生。
          </p>
        </div>

        <div class="hero-side">
          <div class="hero-metrics">
            <div class="metric-card panel-card">
              <div class="metric-label">医生总数</div>
              <div class="metric-value">{{ stats.totalDoctors }}</div>
            </div>
            <div class="metric-card panel-card">
              <div class="metric-label">启用医生</div>
              <div class="metric-value">{{ stats.enabledDoctors }}</div>
            </div>
            <div class="metric-card panel-card">
              <div class="metric-label">当前值班</div>
              <div class="metric-value">{{ stats.currentDutyDoctors }}</div>
            </div>
          </div>

          <div class="hero-actions">
            <button type="button" class="outline-button" @click="openEditor()">
              新增医生
            </button>
            <button
              type="button"
              class="outline-button"
              :class="{ active: slotFilter === 'AM' }"
              @click="setSlotFilter('AM')"
            >
              上午值班
            </button>
            <button
              type="button"
              class="outline-button"
              :class="{ active: slotFilter === 'PM' }"
              @click="setSlotFilter('PM')"
            >
              下午值班
            </button>
          </div>
        </div>
      </section>

      <section class="toolbar-card panel-card">
        <div class="toolbar-row">
          <div class="toolbar-actions">
            <el-button type="primary" class="action-button" @click="openEditor()">
              <el-icon><Plus /></el-icon>
              新增医生
            </el-button>

            <el-button class="action-button action-button-secondary" @click="reloadData">
              <el-icon><RefreshRight /></el-icon>
              刷新列表
            </el-button>
          </div>

          <div class="toolbar-filters">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索医生姓名、科室或擅长方向"
              @clear="resetFilters"
              @keyup.enter="resetFilters"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-select
              v-model="filters.status"
              clearable
              placeholder="全部状态"
              @change="resetFilters"
              @clear="resetFilters"
            >
              <el-option label="全部状态" value="all" />
              <el-option label="启用" value="enabled" />
              <el-option label="停用" value="disabled" />
            </el-select>
          </div>
        </div>
      </section>

      <section class="table-card panel-card">
        <div class="table-head">
          <div>
            <div class="table-title">值班医生列表</div>
            <div class="table-subtitle">支持维护医生信息、启用状态和上午/下午值班安排</div>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="pagedDoctors"
          class="doctor-table"
          empty-text="暂无医生数据"
        >
          <el-table-column prop="doctorName" label="医生姓名" min-width="120">
            <template #default="{ row }">
              <span class="table-name">{{ row.doctorName }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="department" label="科室" min-width="120" />

          <el-table-column prop="title" label="职称" min-width="120" />

          <el-table-column prop="specialty" label="擅长方向" min-width="220" />

          <el-table-column label="启用" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" effect="light" round>
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="上午" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.morningDuty"
                inline-prompt
                active-text="启"
                inactive-text="停"
                @change="persistStatus(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="下午" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.afternoonDuty"
                inline-prompt
                active-text="启"
                inactive-text="停"
                @change="persistStatus(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openEditor(row)">
                  <el-icon><EditPen /></el-icon>
                  编辑
                </el-button>
                <el-button link type="danger" @click="removeDoctor(row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="filteredDoctors.length"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>

    <el-dialog
      v-model="editorVisible"
      :title="editingDoctorId ? '编辑医生' : '新增医生'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="editorFormRef" :model="editorForm" label-position="top">
        <el-form-item label="医生姓名" prop="doctorName" required>
          <el-input v-model="editorForm.doctorName" placeholder="请输入医生姓名" />
        </el-form-item>

        <el-form-item label="科室" prop="department" required>
          <el-input v-model="editorForm.department" placeholder="请输入科室" />
        </el-form-item>

        <el-form-item label="职称" prop="title" required>
          <el-input v-model="editorForm.title" placeholder="请输入职称" />
        </el-form-item>

        <el-form-item label="擅长方向" prop="specialty" required>
          <el-input
            v-model="editorForm.specialty"
            type="textarea"
            :rows="3"
            placeholder="请输入擅长方向"
          />
        </el-form-item>

        <el-form-item label="是否启用">
          <el-switch v-model="editorForm.enabled" />
        </el-form-item>

        <div class="duty-grid">
          <el-form-item label="上午值班">
            <el-switch v-model="editorForm.morningDuty" />
          </el-form-item>
          <el-form-item label="下午值班">
            <el-switch v-model="editorForm.afternoonDuty" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEditor">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Delete,
  EditPen,
  Plus,
  RefreshRight,
  Search,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDoctorDuty,
  deleteDoctorDuty,
  fetchDoctorDutyList,
  fetchDoctorDutyStats,
  updateDoctorDuty,
  updateDoctorDutyStatus,
} from '@/api/doctorDuty'

const emit = defineEmits(['navigate'])

const loading = ref(false)
const submitting = ref(false)
const editorVisible = ref(false)
const doctorList = ref([])
const stats = ref({
  totalDoctors: 0,
  enabledDoctors: 0,
  currentDutyDoctors: 0,
})
const editingDoctorId = ref(null)
const editorFormRef = ref()
const pagination = ref({
  pageNum: 1,
  pageSize: 6,
})
const filters = ref({
  keyword: '',
  status: 'all',
})
const slotFilter = ref('ALL')

const editorForm = reactive({
  doctorName: '',
  department: '',
  title: '',
  specialty: '',
  enabled: true,
  morningDuty: true,
  afternoonDuty: true,
})

const normalizeDoctor = (item = {}) => ({
  id: item.id,
  doctorName: item.doctorName || '',
  department: item.department || '',
  title: item.title || '',
  specialty: item.specialty || '',
  enabled: Boolean(item.enabled),
  morningDuty: Boolean(item.morningDuty),
  afternoonDuty: Boolean(item.afternoonDuty),
  currentSlot: item.currentSlot || '',
  currentDuty: Boolean(item.currentDuty),
})

const filteredDoctors = computed(() => {
  const keyword = filters.value.keyword.trim().toLowerCase()
  const status = filters.value.status

  let rows = doctorList.value.filter((item) => {
    const text = [
      item.doctorName,
      item.department,
      item.title,
      item.specialty,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    if (keyword && !text.includes(keyword)) {
      return false
    }

    if (status === 'enabled' && !item.enabled) {
      return false
    }

    if (status === 'disabled' && item.enabled) {
      return false
    }

    if (slotFilter.value === 'AM' && !item.morningDuty) {
      return false
    }

    if (slotFilter.value === 'PM' && !item.afternoonDuty) {
      return false
    }

    return true
  })

  return rows
})

const pagedDoctors = computed(() => {
  const start = (pagination.value.pageNum - 1) * pagination.value.pageSize
  return filteredDoctors.value.slice(start, start + pagination.value.pageSize)
})

const goToSection = (section) => {
  emit('navigate', section)
}

const setSlotFilter = (slot) => {
  slotFilter.value = slotFilter.value === slot ? 'ALL' : slot
  pagination.value.pageNum = 1
}

const resetFilters = () => {
  pagination.value.pageNum = 1
  loadDoctors()
}

const handlePageChange = (pageNum) => {
  pagination.value.pageNum = pageNum
}

const loadDoctors = async () => {
  loading.value = true
  try {
    const payload = await fetchDoctorDutyList()
    const list = Array.isArray(payload) ? payload : []
    doctorList.value = list.map(normalizeDoctor)
    pagination.value.pageNum = 1
  } catch (error) {
    doctorList.value = []
    ElMessage.error(error?.message || '加载医生列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const payload = await fetchDoctorDutyStats()
    stats.value = {
      totalDoctors: Number(payload?.totalDoctors || 0),
      enabledDoctors: Number(payload?.enabledDoctors || 0),
      currentDutyDoctors: Number(payload?.currentDutyDoctors || 0),
    }
  } catch (error) {
    ElMessage.error(error?.message || '加载统计失败')
  }
}

const reloadData = async () => {
  await Promise.all([loadDoctors(), loadStats()])
}

const openEditor = (row = null) => {
  editingDoctorId.value = row?.id ?? null
  editorForm.doctorName = row?.doctorName || ''
  editorForm.department = row?.department || ''
  editorForm.title = row?.title || ''
  editorForm.specialty = row?.specialty || ''
  editorForm.enabled = row ? row.enabled : true
  editorForm.morningDuty = row ? row.morningDuty : true
  editorForm.afternoonDuty = row ? row.afternoonDuty : true
  editorVisible.value = true
}

const submitEditor = async () => {
  if (!editorForm.doctorName.trim() || !editorForm.department.trim() || !editorForm.title.trim() || !editorForm.specialty.trim()) {
    ElMessage.warning('请填写完整的医生信息')
    return
  }

  submitting.value = true
  try {
    const payload = {
      doctorName: editorForm.doctorName.trim(),
      department: editorForm.department.trim(),
      title: editorForm.title.trim(),
      specialty: editorForm.specialty.trim(),
      enabled: editorForm.enabled ? 1 : 0,
      morningDuty: editorForm.morningDuty ? 1 : 0,
      afternoonDuty: editorForm.afternoonDuty ? 1 : 0,
    }

    if (editingDoctorId.value) {
      await updateDoctorDuty(editingDoctorId.value, payload)
      ElMessage.success('医生信息已更新')
    } else {
      await createDoctorDuty(payload)
      ElMessage.success('医生已新增')
    }

    editorVisible.value = false
    await reloadData()
  } catch (error) {
    ElMessage.error(error?.message || '保存医生信息失败')
  } finally {
    submitting.value = false
  }
}

const persistStatus = async (row) => {
  try {
    await updateDoctorDutyStatus(row.id, {
      enabled: row.enabled ? 1 : 0,
      morningDuty: row.morningDuty ? 1 : 0,
      afternoonDuty: row.afternoonDuty ? 1 : 0,
    })
    ElMessage.success('值班状态已更新')
    await reloadData()
  } catch (error) {
    ElMessage.error(error?.message || '更新值班状态失败')
    await reloadData()
  }
}

const removeDoctor = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除医生「${row.doctorName}」吗？删除后上午/下午值班记录会一起移除。`,
      '删除医生',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  try {
    await deleteDoctorDuty(row.id)
    ElMessage.success('医生已删除')
    await reloadData()
  } catch (error) {
    ElMessage.error(error?.message || '删除医生失败')
  }
}

onMounted(() => {
  reloadData()
})
</script>

<style scoped>
.doctor-layout {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(202, 227, 206, 0.42), transparent 28%),
    linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.doctor-sidebar {
  width: 308px;
  padding: 88px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #f8fbf8 0%, #edf3ec 100%);
  border-right: 1px solid #dfe9dd;
  overflow-y: auto;
}

.doctor-main {
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
  padding: 24px 24px 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.hero-copy {
  min-width: 0;
}

.hero-title {
  margin: 0;
  color: #21352a;
  font-size: 42px;
  line-height: 1.05;
  letter-spacing: -0.03em;
}

.hero-description {
  margin: 12px 0 0;
  color: #627668;
  font-size: 14px;
  line-height: 1.7;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 118px));
  gap: 8px;
}

.metric-card {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(96, 130, 103, 0.06);
}

.metric-label {
  font-size: 11px;
  color: #607665;
  font-weight: 700;
}

.metric-value {
  margin-top: 5px;
  font-size: 24px;
  text-align: center;
  color: #233a2b;
  font-weight: 800;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.outline-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid #d6e3d5;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #31503a;
  font-weight: 700;
  cursor: pointer;
}

.outline-button.active {
  border-color: #6fa173;
  background: #eff8ec;
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
  width: min(100%, 560px);
}

.toolbar-filters :deep(.el-input) {
  flex: 1;
  min-width: 260px;
}

.toolbar-filters :deep(.el-select) {
  flex: 0 0 168px;
  min-width: 168px;
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

.doctor-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.doctor-table :deep(th.el-table__cell) {
  background: rgba(237, 245, 234, 0.8);
  color: #4d6454;
  font-weight: 700;
}

.doctor-table :deep(.el-table__fixed-right .el-table__header-wrapper th.el-table__cell),
.doctor-table :deep(.el-table__fixed-right-patch) {
  background: rgba(237, 245, 234, 0.8);
}

.doctor-table :deep(.el-table__fixed-right .el-table__header-wrapper th.el-table__cell) {
  color: #4d6454;
}

.doctor-table :deep(th.el-table-fixed-column--right) {
  background: rgba(237, 245, 234, 0.8) !important;
}

.doctor-table :deep(td.el-table-fixed-column--right) {
  background: #ffffff !important;
}

.doctor-table :deep(.el-table__cell) {
  padding: 14px 0;
}

.doctor-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(243, 251, 241, 0.82);
}

.doctor-table :deep(.el-table__row:hover > td.el-table-fixed-column--right) {
  background: rgba(243, 251, 241, 0.82) !important;
}

.table-name {
  color: #284333;
  font-size: 14px;
  font-weight: 700;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.table-pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.duty-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 1180px) {
  .doctor-layout {
    flex-direction: column;
    min-height: 100vh;
  }

  .doctor-sidebar {
    width: auto;
    padding-top: 92px;
    border-right: none;
    border-bottom: 1px solid #dfe9dd;
  }

  .doctor-main {
    padding-top: 20px;
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-side {
    width: 100%;
    align-items: flex-start;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .hero-metrics {
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .doctor-main,
  .doctor-sidebar {
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

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .duty-grid {
    grid-template-columns: 1fr;
  }
}
</style>
