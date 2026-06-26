import request from './request'

const TOKEN_KEY = 'xiaoxiaobai_token'
const USER_KEY = 'xiaoxiaobai_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY)

export const getStoredUser = () => {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export const saveAuth = ({ token, ...user }) => {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export const login = async (data) => {
  const result = await request.post('/auth/login', data)
  saveAuth(result)
  return result
}

export const register = async (data) => {
  const result = await request.post('/auth/register', data)
  saveAuth(result)
  return result
}
