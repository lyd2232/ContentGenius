/** 复制纯文本到剪贴板 */
export async function copyText(text) {
  const value = text?.trim()
  if (!value) {
    throw new Error('暂无内容可复制')
  }
  await navigator.clipboard.writeText(value)
}
