import request from './request'

export const fetchCurrentProfile = () => {
  return request.get('/users/me')
}

export const updateMyPassword = (data) => {
  return request.put('/users/me/password', data)
}

export const fetchMyAppointments = () => {
  return request.get('/users/me/appointments')
}
