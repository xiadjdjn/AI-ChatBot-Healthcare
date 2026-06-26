import request from './request'

export const fetchDoctorDutyList = (params = {}) => {
  return request.get('/doctor-duties', { params })
}

export const fetchCurrentDoctorDuties = (params = {}) => {
  return request.get('/doctor-duties/current', { params })
}

export const fetchDoctorDutyStats = () => {
  return request.get('/doctor-duties/stats')
}

export const createDoctorDuty = (data) => {
  return request.post('/doctor-duties', data)
}

export const updateDoctorDuty = (id, data) => {
  return request.put(`/doctor-duties/${id}`, data)
}

export const updateDoctorDutyStatus = (id, data) => {
  return request.patch(`/doctor-duties/${id}/duty`, data)
}

export const deleteDoctorDuty = (id) => {
  return request.delete(`/doctor-duties/${id}`)
}
