import request from './request'

export const uploadKnowledge = (file, name) => {
  const formData = new FormData()
  formData.append('file', file)

  if (name) {
    formData.append('name', name)
  }

  return request.post('/knowledge/documents/upload', formData)
}

export const createKnowledgeText = (data = {}) => {
  return request.post('/knowledge/documents/text', data)
}

export const fetchKnowledgeList = (params = {}) => {
  return request.get('/knowledge/documents', { params })
}

export const fetchKnowledgeDetail = (id) => {
  return request.get(`/knowledge/documents/${id}`)
}

export const fetchKnowledgeSegments = (id) => {
  return request.get(`/knowledge/documents/${id}/segments`)
}

export const reingestKnowledge = (id) => {
  return request.post(`/knowledge/documents/${id}/reingest`)
}

export const deleteKnowledge = (id) => {
  return request.delete(`/knowledge/documents/${id}`)
}
