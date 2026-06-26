import request from './request'

export const fetchAdminChatSessions = (params = {}) => {
  return request.get('/admin/chat-sessions', { params })
}
