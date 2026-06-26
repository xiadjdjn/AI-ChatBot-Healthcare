<template>
  <div class="login-layout">
    <section class="login-panel">
      <div class="login-brand">
        <img src="@/assets/logo.png" alt="医疗助手小小白" width="92" height="92" />
        <div>
          <span class="eyebrow">Medical Assistant Console</span>
          <h1>医疗助手小小白</h1>
          <p>{{ isRegisterMode ? '欢迎注册。小小白医疗助手给予你最真挚的服务。' : '欢迎登录预约平台。助手小小白助力您平平安安幸福生活。' }}</p>
        </div>
      </div>

      <el-form class="login-form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            autocomplete="username"
            placeholder="请输入用户名"
            size="large"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-form-item v-if="isRegisterMode" label="昵称">
          <el-input
            v-model="form.nickname"
            autocomplete="nickname"
            placeholder="请输入昵称"
            size="large"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            :autocomplete="isRegisterMode ? 'new-password' : 'current-password'"
            placeholder="请输入密码"
            show-password
            size="large"
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-form-item v-if="isRegisterMode" label="确认密码">
          <el-input
            v-model="form.confirmPassword"
            autocomplete="new-password"
            placeholder="请再次输入密码"
            show-password
            size="large"
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          class="login-error"
          type="error"
          show-icon
          :closable="false"
        />

        <el-button
          class="login-button"
          type="primary"
          size="large"
          :loading="loading"
          @click="submit"
        >
          {{ isRegisterMode ? '注册并进入对话' : '登录' }}
        </el-button>

        <el-button
          class="register-button"
          text
          type="primary"
          :disabled="loading"
          @click="toggleRegisterMode"
        >
          {{ isRegisterMode ? '已有账号，返回登录' : '注册' }}
        </el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { login, register } from '@/api/auth'

const emit = defineEmits(['login-success'])

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})
const loading = ref(false)
const errorMessage = ref('')
const isRegisterMode = ref(false)

const submit = async () => {
  const username = form.username.trim()
  const nickname = form.nickname.trim()
  const password = form.password.trim()
  const confirmPassword = form.confirmPassword.trim()

  if (!username || !password) {
    errorMessage.value = '请填写用户名和密码'
    return
  }
  if (isRegisterMode.value && password !== confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const user = isRegisterMode.value
      ? await register({ username, nickname, password, confirmPassword })
      : await login({ username, password })
    emit('login-success', user)
  } catch (error) {
    errorMessage.value = error?.message || (isRegisterMode.value ? '注册失败，请稍后重试' : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const toggleRegisterMode = () => {
  isRegisterMode.value = !isRegisterMode.value
  errorMessage.value = ''
  form.confirmPassword = ''
}
</script>

<style scoped>
.login-layout {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(202, 227, 206, 0.42), transparent 30%),
    linear-gradient(180deg, #f7faf7 0%, #eef4ec 100%);
}

.login-panel {
  width: min(100%, 520px);
  padding: 34px;
  border: 1px solid #dbe7d8;
  border-radius: 24px;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.96),
    rgba(244, 249, 242, 0.94)
  );
  box-shadow: 0 24px 60px rgba(96, 130, 103, 0.14);
}

.login-brand {
  display: flex;
  gap: 18px;
  align-items: center;
}

.login-brand img {
  flex: 0 0 auto;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  color: #6d8472;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #21352a;
  font-size: 30px;
  line-height: 1.15;
}

p {
  margin: 10px 0 0;
  color: #627668;
  font-size: 14px;
  line-height: 1.7;
}

.login-form {
  margin-top: 30px;
}

.login-error {
  margin-bottom: 18px;
}

.login-button {
  width: 100%;
  min-height: 44px;
  border-radius: 14px;
  font-weight: 700;
}

.register-button {
  width: 100%;
  margin: 12px 0 0;
  font-weight: 700;
}

@media (max-width: 560px) {
  .login-layout {
    padding: 16px;
  }

  .login-panel {
    padding: 24px;
  }

  .login-brand {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
