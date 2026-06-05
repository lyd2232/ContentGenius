import { getToken } from './http'

/**
 * 调用 SSE 流式写稿，onChunk 收到 partial content，onDone 收到 versionId。
 */
export async function chatStream(body, { onChunk, onDone, onError }) {
  const token = getToken()
  const res = await fetch('/api/agent/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body)
  })

  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || `流式请求失败 (${res.status})`)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n')
    buffer = parts.pop() || ''

    for (const line of parts) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith(':')) continue
      let jsonStr = trimmed
      if (trimmed.startsWith('data:')) {
        jsonStr = trimmed.slice(5).trim()
      }
      if (!jsonStr || jsonStr === '[DONE]') continue
      try {
        const payload = JSON.parse(jsonStr)
        const data = payload.data ?? payload
        if (data.content) {
          onChunk?.(data.content, data)
        }
        if (data.versionId) {
          onDone?.(data)
        }
      } catch {
        /* 忽略非 JSON 行 */
      }
    }
  }
}
