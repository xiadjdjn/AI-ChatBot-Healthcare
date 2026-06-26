<template>
  <div class="profile-layout">
    <aside class="profile-sidebar">
      <section class="profile-card panel-card">
        <span class="eyebrow">Personal Center</span>
        <h1 class="profile-title">个人中心</h1>
        <p class="profile-description">
          查看当前登录账号信息、修改密码，并集中查看自己在系统中的全部预约记录。
        </p>
      </section>

      <section class="panel-card metrics-card">
        <div class="metrics-card-header">
          <div class="metrics-card-title">账号信息</div>
          <el-button class="metrics-refresh-button refresh-button" @click="loadProfile">
            刷新资料
          </el-button>
        </div>
        <div class="metric-item">
          <div class="metric-label">用户 ID</div>
          <div class="metric-value">{{ profile.userId ?? '--' }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">用户名</div>
          <div class="metric-value">{{ profile.username || '--' }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">昵称</div>
          <div class="metric-value">{{ profile.nickname || '未设置昵称' }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">预约数量</div>
          <div class="metric-value">{{ appointments.length }}</div>
        </div>
      </section>
    </aside>

    <main class="profile-main">
      <section class="panel-card section-card">
        <div class="section-header">
          <div>
            <div class="section-title">修改密码</div>
            <div class="section-subtitle">输入旧密码后设置新的登录密码</div>
          </div>
        </div>

        <el-form class="password-form" label-position="top" @submit.prevent>
          <el-form-item label="旧密码">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="至少 6 位"
            />
          </el-form-item>

          <el-form-item label="确认新密码">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入新密码"
            />
          </el-form-item>

          <div class="password-actions">
            <el-button @click="resetPasswordForm">重置</el-button>
            <el-button type="primary" :loading="passwordSubmitting" @click="submitPasswordChange">
              保存密码
            </el-button>
          </div>
        </el-form>
      </section>

      <section class="panel-card section-card">
        <div class="section-header">
          <div>
            <div class="section-title">我的预约记录</div>
            <div class="section-subtitle">当前登录用户在 MySQL 中的全部预约记录</div>
          </div>
          <el-button class="refresh-button" @click="loadAppointments">
            刷新预约
          </el-button>
        </div>

        <el-table
          v-loading="appointmentLoading"
          :data="appointments"
          empty-text="暂无预约记录"
          class="appointment-table"
        >
          <el-table-column prop="patientName" label="就诊人" min-width="120" />
          <el-table-column prop="idCard" label="身份证号" min-width="150" />
          <el-table-column prop="department" label="科室" min-width="120" />
          <el-table-column prop="doctorName" label="医生" min-width="120" />
          <el-table-column prop="date" label="日期" min-width="120" />
          <el-table-column prop="time" label="时段" min-width="100" />
          <el-table-column prop="status" label="状态" min-width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="appointmentStatusTagType(row.status)" effect="light" round>
                {{ row.status || '--' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCurrentProfile, fetchMyAppointments, updateMyPassword } from '@/api/profile'

const profileLoading = ref(false)
const appointmentLoading = ref(false)
const passwordSubmitting = ref(false)

const profile = reactive({
  userId: null,
  username: '',
  nickname: '',
})

const appointments = ref([])

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const loadProfile = async () => {
  profileLoading.value = true
  try {
    const data = await fetchCurrentProfile()
    profile.userId = data?.userId ?? null
    profile.username = data?.username || ''
    profile.nickname = data?.nickname || ''
  } catch (error) {
    ElMessage.error(error?.message || '加载个人资料失败')
  } finally {
    profileLoading.value = false
  }
}

const loadAppointments = async () => {
  appointmentLoading.value = true
  try {
    const data = await fetchMyAppointments()
    appointments.value = Array.isArray(data) ? data : []
  } catch (error) {
    appointments.value = []
    ElMessage.error(error?.message || '加载预约记录失败')
  } finally {
    appointmentLoading.value = false
  }
}

const appointmentStatusTagType = (status) => {
  switch (String(status || '').trim()) {
    case '进行中':
      return 'primary'
    case '已完成':
      return 'success'
    case '已取消':
      return 'info'
    default:
      return 'info'
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const submitPasswordChange = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请先填写完整的密码信息')
    return
  }

  passwordSubmitting.value = true
  try {
    await updateMyPassword({ ...passwordForm })
    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } catch (error) {
    ElMessage.error(error?.message || '修改密码失败')
  } finally {
    passwordSubmitting.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadAppointments()
})
</script>

<style scoped>
.profile-layout {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(196, 226, 210, 0.35), transparent 28%),
    linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.profile-sidebar {
  width: 300px;
  padding: 88px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #f8fbf8 0%, #edf3ec 100%);
  border-right: 1px solid #dfe9dd;
}

.profile-main {
  flex: 1;
  min-width: 0;
  padding: 88px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-card {
  border: 1px solid #dbe7d8;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 242, 0.94));
  box-shadow: 0 20px 45px rgba(96, 130, 103, 0.08);
}

.profile-card,
.metrics-card,
.section-card {
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

.profile-title,
.section-title {
  margin: 0;
  color: #284333;
}

.profile-title {
  font-size: 30px;
}

.profile-description,
.section-subtitle {
  margin-top: 10px;
  color: #627668;
  line-height: 1.7;
}

.metrics-card {
  position: relative;
  display: grid;
  gap: 14px;
}

.metrics-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metrics-card-title {
  color: #284333;
  font-weight: 800;
}

.metrics-refresh-button {
  flex: 0 0 auto;
}

.metric-label {
  display: block;
  color: #708274;
  font-size: 12px;
}

.metric-value {
  display: block;
  margin-top: 6px;
  color: #24392c;
  font-size: 20px;
  font-weight: 800;
  word-break: break-word;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.refresh-button {
  border-radius: 14px;
}

.password-form {
  max-width: 480px;
}

.password-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.appointment-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.appointment-table :deep(th.el-table__cell) {
  background: rgba(237, 245, 234, 0.8);
  color: #4d6454;
  font-weight: 700;
}

@media (max-width: 960px) {
  .profile-layout {
    flex-direction: column;
  }

  .profile-sidebar {
    width: auto;
    border-right: none;
    border-bottom: 1px solid #dfe9dd;
  }

  .profile-main,
  .profile-sidebar {
    padding-left: 14px;
    padding-right: 14px;
  }

  .section-header {
    flex-direction: column;
  }
}
</style>
