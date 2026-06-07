const TOKEN_KEY = 'cg_token'

/** 无需登录的接口：不附带 token，避免本地残留过期 token 被网关拦成 401 */
const PUBLIC_API_PREFIXES = [
  '/api/users/login',
  '/api/users/register',
  '/api/users/sms/send'
]

function isPublicApi(url) {
  return PUBLIC_API_PREFIXES.some((prefix) => url.startsWith(prefix))
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export async function request(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
  const token = getToken()
  if (token && !isPublicApi(url)) {
    headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(url, { ...options, headers })
  const json = await res.json().catch(() => ({}))

  if (!res.ok) {
    throw new Error(json.message || `请求失败 (${res.status})`)
  }
  if (json.code !== undefined && json.code !== 0) {
    throw new Error(json.message || '业务错误')
  }
  return json.data
}

export async function uploadFile(file) {
  const form = new FormData()
  form.append('file', file)
  const token = getToken()
  const res = await fetch('/api/content/files/upload', {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form
  })
  const json = await res.json()
  if (json.code !== 0) {
    throw new Error(json.message || '上传失败')
  }
  return json.data
}
