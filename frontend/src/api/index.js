import { request, uploadFile } from './http'

export { uploadFile }

export const login = (username, password) =>
  request('/api/users/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  }).then((d) => d.token)

export const register = (body) =>
  request('/api/users/register', {
    method: 'POST',
    body: JSON.stringify(body)
  })

/** 发送注册短信验证码 */
export const sendRegisterSms = (phone) =>
  request('/api/users/sms/send', {
    method: 'POST',
    body: JSON.stringify({ phone })
  })

export const fetchMe = () => request('/api/users/me')

export const updateProfile = (body) =>
  request('/api/users/me', { method: 'PUT', body: JSON.stringify(body) })

export const changePassword = (body) =>
  request('/api/users/me/password', { method: 'PUT', body: JSON.stringify(body) })

export const deleteAccount = (password) =>
  request('/api/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ password })
  })

export const listFeedback = () => request('/api/users/feedback')

export const submitFeedback = (content) =>
  request('/api/users/feedback', {
    method: 'POST',
    body: JSON.stringify({ content })
  })

export const listProjects = () => request('/api/content/projects')

export const createProject = (body) =>
  request('/api/content/projects', { method: 'POST', body: JSON.stringify(body) })

export const deleteProject = (id) =>
  request(`/api/content/projects/${id}`, { method: 'DELETE' })

export const listVersions = (projectId) =>
  request(`/api/content/projects/${projectId}/versions`)

export const getVersion = (id) => request(`/api/content/versions/${id}`)

export const updateVersion = (id, body) =>
  request(`/api/content/versions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body)
  })

export const deleteVersion = (id) =>
  request(`/api/content/versions/${id}`, { method: 'DELETE' })

export const chat = (body) =>
  request('/api/agent/chat', { method: 'POST', body: JSON.stringify(body) })

export const visionAnalyze = (body) =>
  request('/api/agent/vision/analyze', { method: 'POST', body: JSON.stringify(body) })

export const listFiles = () => request('/api/content/files')

export const deleteFile = (objectName) =>
  request(`/api/content/files?objectName=${encodeURIComponent(objectName)}`, {
    method: 'DELETE'
  })
