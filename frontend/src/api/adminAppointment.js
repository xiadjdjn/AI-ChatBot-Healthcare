import request from './request'

export const fetchAdminAppointments = (params = {}) => {
  return request.get('/admin/appointments', { params })
}
