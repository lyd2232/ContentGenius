<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { listVersions, getVersion, updateVersion, deleteVersion } from '../api'
import { copyText } from '../utils/clipboard'

const route = useRoute()
const versions = ref([])
const selected = ref(null)
const loading = ref(true)
const actionTip = ref('')
const finalizing = ref(false)
const deleting = ref(false)
const error = ref('')

const isFinalized = computed(() => selected.value?.status === 1)

const canFinalize = computed(
  () => selected.value?.id && selected.value?.content?.trim() && !isFinalized.value
)

onMounted(async () => {
  const projectId = route.params.id
  versions.value = await listVersions(projectId)
  const q = Number(route.query.versionId)
  if (q) {
    await open(q)
  }
  loading.value = false
})

async function open(id) {
  error.value = ''
  selected.value = await getVersion(id)
}

function sourceLabel(s) {
  if (s === 'agent') return 'Agent'
  if (s === 'manual') return '手动'
  return s || '未知'
}

function flashActionTip(msg) {
  actionTip.value = msg
  setTimeout(() => {
    if (actionTip.value === msg) actionTip.value = ''
  }, 2200)
}

async function copySelected() {
  try {
    const text = selected.value?.content || ''
    await copyText(text)
    flashActionTip('已复制到剪贴板')
  } catch (e) {
    flashActionTip(e.message || '复制失败')
  }
}

async function removeSelected() {
  const id = selected.value?.id
  if (!id) return
  const finalized = selected.value?.status === 1
  const msg = finalized
    ? '该版本已定稿。删除后将从版本历史中移除，并同步删除 RAG 向量库中的对应条目，不可恢复。确定删除？'
    : '确定删除该草稿版本？删除后不可恢复。'
  if (!window.confirm(msg)) return
  deleting.value = true
  error.value = ''
  try {
    await deleteVersion(id)
    versions.value = versions.value.filter((v) => v.id !== id)
    selected.value = null
    flashActionTip(finalized ? '已定稿版本已删除' : '草稿已删除')
  } catch (e) {
    error.value = e.message || '删除失败'
  } finally {
    deleting.value = false
  }
}

async function finalizeSelected() {
  const id = selected.value?.id
  if (!id) return
  if (
    !window.confirm(
      '定稿后将把该版本写入 RAG 向量库，供后续开启 RAG 的创作检索参考。确定定稿？'
    )
  ) {
    return
  }
  finalizing.value = true
  error.value = ''
  try {
    const updated = await updateVersion(id, { status: 1 })
    selected.value = updated
    const idx = versions.value.findIndex((v) => v.id === id)
    if (idx >= 0) versions.value[idx] = { ...versions.value[idx], status: 1 }
    flashActionTip('已定稿，正在写入知识库（异步）')
  } catch (e) {
    error.value = e.message || '定稿失败'
  } finally {
    finalizing.value = false
  }
}
</script>

<template>
  <div class="detail-page">
    <RouterLink to="/projects" class="back">← 返回项目列表</RouterLink>
    <h2>版本历史</h2>
    <div v-if="loading" class="text-muted">加载中…</div>
    <div v-else class="layout">
      <ul class="timeline sketch-box">
        <li v-for="v in versions" :key="v.id" class="item">
          <div class="ver">v{{ v.versionNo }}</div>
          <div class="tags">
            <span class="sketch-tag">{{ sourceLabel(v.source) }}</span>
            <span class="sketch-tag">{{ v.platform }}</span>
            <span v-if="v.status === 1" class="status-final">已定稿</span>
            <span v-else class="status-draft text-muted">草稿</span>
          </div>
          <p class="time text-muted">{{ v.createdAt?.replace('T', ' ').slice(0, 16) }}</p>
          <button class="sketch-btn secondary btn-sm" type="button" @click="open(v.id)">查看</button>
        </li>
        <li v-if="!versions.length" class="empty text-muted">暂无版本</li>
      </ul>
      <article v-if="selected" class="preview sketch-box">
        <div class="preview-head">
          <h3>{{ selected.title || '无标题' }}</h3>
          <div class="preview-tools">
            <span v-if="actionTip" class="action-tip">{{ actionTip }}</span>
            <button class="sketch-btn secondary btn-sm" type="button" @click="copySelected">
              复制全文
            </button>
            <button
              class="sketch-btn btn-sm"
              type="button"
              :disabled="!canFinalize || finalizing"
              @click="finalizeSelected"
            >
              {{ finalizing ? '定稿中…' : isFinalized ? '已定稿' : '定稿' }}
            </button>
            <button
              class="sketch-btn danger btn-sm"
              type="button"
              :disabled="deleting || finalizing"
              @click="removeSelected"
            >
              {{ deleting ? '删除中…' : '删除版本' }}
            </button>
          </div>
        </div>
        <pre class="body">{{ selected.content }}</pre>
      </article>
      <div v-else class="preview-placeholder sketch-box text-muted">
        选择左侧版本查看内容
      </div>
    </div>
    <p v-if="error" class="text-error">{{ error }}</p>
  </div>
</template>

<style scoped>
.detail-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.back {
  display: inline-block;
  margin-bottom: 0.75rem;
  color: var(--cg-green-800);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
}

.back:hover {
  text-decoration: underline;
}

h2 {
  margin: 0 0 1rem;
  color: var(--cg-green-900);
}

.layout {
  flex: 1;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 1rem;
  align-items: stretch;
  min-height: calc(100vh - 8.5rem);
}

@media (max-width: 800px) {
  .layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }
}

.timeline {
  list-style: none;
  margin: 0;
  padding: 0.5rem 0;
  overflow: auto;
  min-height: 0;
}

.item {
  border-bottom: 1px solid var(--cg-border);
  padding: 0.85rem 1rem;
}

.item:last-child {
  border-bottom: none;
}

.ver {
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--cg-green-900);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin: 0.4rem 0;
}

.time {
  font-size: 0.8125rem;
  margin: 0 0 0.5rem;
}

.status-final {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--cg-success);
}

.status-draft {
  font-size: 0.75rem;
}

.preview {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--cg-border);
  margin-bottom: 0.75rem;
}

.preview-head h3 {
  margin: 0;
  flex: 1;
  min-width: 120px;
  color: var(--cg-green-900);
}

.preview-tools {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.action-tip {
  font-size: 0.75rem;
  color: var(--cg-green-800);
}

.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.8125rem;
}

.preview .body {
  flex: 1;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 0.9375rem;
  line-height: 1.65;
  overflow: auto;
  margin: 0;
  min-height: 0;
  color: var(--cg-gray-700);
}

.preview-placeholder {
  display: grid;
  place-items: center;
  font-size: 0.875rem;
  min-height: 0;
}

.empty {
  padding: 1.5rem;
  text-align: center;
}

.text-error {
  margin-top: 0.75rem;
}
</style>
