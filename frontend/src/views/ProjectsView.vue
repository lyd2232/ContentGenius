<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listProjects, listVersions, createProject, deleteProject } from '../api'

const router = useRouter()
const projects = ref([])
const selectedProjectId = ref(null)
const drafts = ref([])
const finalized = ref([])
const loading = ref(true)
const versionsLoading = ref(false)
const showModal = ref(false)
const form = ref({ title: '', description: '' })
const error = ref('')

const selectedProject = computed(() =>
  projects.value.find((p) => p.id === selectedProjectId.value) ?? null
)

function formatTime(iso) {
  return iso?.replace('T', ' ').slice(0, 16) || '-'
}

function platformLabel(p) {
  if (p === 'xiaohongshu') return '小红书'
  if (p === 'wechat') return '公众号'
  if (p === 'bilibili') return 'B站'
  return p || '-'
}

async function loadVersions(projectId) {
  if (!projectId) {
    drafts.value = []
    finalized.value = []
    return
  }
  versionsLoading.value = true
  try {
    const versions = await listVersions(projectId)
    drafts.value = versions.filter((v) => v.status !== 1)
    finalized.value = versions.filter((v) => v.status === 1)
  } catch (e) {
    error.value = e.message
  } finally {
    versionsLoading.value = false
  }
}

async function loadProjects() {
  loading.value = true
  error.value = ''
  try {
    projects.value = await listProjects()
    if (!projects.value.length) {
      selectedProjectId.value = null
      drafts.value = []
      finalized.value = []
      return
    }
    const stillExists = projects.value.some((p) => p.id === selectedProjectId.value)
    if (!stillExists) {
      selectedProjectId.value = projects.value[0].id
    }
    await loadVersions(selectedProjectId.value)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

watch(selectedProjectId, (id) => {
  if (!loading.value && id) loadVersions(id)
})

async function submitCreate() {
  error.value = ''
  try {
    const created = await createProject(form.value)
    showModal.value = false
    form.value = { title: '', description: '' }
    await loadProjects()
    if (created?.id) selectedProjectId.value = created.id
  } catch (e) {
    error.value = e.message
  }
}

async function removeSelected() {
  const id = selectedProjectId.value
  if (!id) return
  if (!confirm('确定删除该项目？相关版本将一并移除。')) return
  await deleteProject(id)
  selectedProjectId.value = null
  await loadProjects()
}

function openVersion(versionId) {
  router.push({ path: `/projects/${selectedProjectId.value}`, query: { versionId } })
}

onMounted(loadProjects)
</script>

<template>
  <div class="projects-page">
    <div class="head">
      <div>
        <h2>我的项目</h2>
        <p class="text-muted head-sub">每个项目可保存多版稿件与 Agent 生成记录</p>
      </div>
      <button class="sketch-btn" type="button" @click="showModal = true">新建项目</button>
    </div>

    <p v-if="error" class="text-error">{{ error }}</p>
    <div v-if="loading" class="text-muted">加载中…</div>

    <div v-else-if="!projects.length" class="empty sketch-box">
      <p class="empty-title">暂无项目</p>
      <p class="text-muted">点击「新建项目」开始你的第一篇内容</p>
    </div>

    <template v-else>
      <div class="toolbar sketch-box">
        <label class="sketch-label" for="project-select">选择项目</label>
        <select id="project-select" v-model="selectedProjectId" class="sketch-select project-select">
          <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.title }}</option>
        </select>
      </div>

      <section v-if="selectedProject" class="project-block sketch-box">
        <div class="project-head">
          <div>
            <p class="desc text-muted">{{ selectedProject.description || '暂无描述' }}</p>
            <p class="meta">更新 {{ formatTime(selectedProject.updatedAt) }}</p>
          </div>
          <div class="project-actions">
            <button
              class="sketch-btn"
              type="button"
              @click="router.push({ path: '/create', query: { projectId: selectedProject.id } })"
            >
              AI 创作
            </button>
            <button
              class="sketch-btn secondary"
              type="button"
              @click="router.push(`/projects/${selectedProject.id}`)"
            >
              全部版本
            </button>
            <button class="sketch-btn danger" type="button" @click="removeSelected">删除项目</button>
          </div>
        </div>

        <div v-if="versionsLoading" class="text-muted versions-loading">稿件加载中…</div>

        <div v-else class="manuscript-columns">
          <div class="column column-history">
            <h4 class="column-title">历史（草稿）</h4>
            <ul v-if="drafts.length" class="manuscript-list">
              <li
                v-for="v in drafts"
                :key="v.id"
                class="manuscript-item"
                @click="openVersion(v.id)"
              >
                <div class="item-head">
                  <span class="item-title">{{ v.title || '无标题' }}</span>
                  <span class="sketch-tag">v{{ v.versionNo }}</span>
                </div>
                <div class="item-meta">
                  <span class="sketch-tag">{{ platformLabel(v.platform) }}</span>
                  <span class="text-muted">{{ formatTime(v.createdAt) }}</span>
                </div>
              </li>
            </ul>
            <p v-else class="column-empty text-muted">暂无草稿</p>
          </div>

          <div class="column column-final">
            <h4 class="column-title">定稿</h4>
            <ul v-if="finalized.length" class="manuscript-list">
              <li
                v-for="v in finalized"
                :key="v.id"
                class="manuscript-item manuscript-item--final"
                @click="openVersion(v.id)"
              >
                <div class="item-head">
                  <span class="item-title">{{ v.title || '无标题' }}</span>
                  <span class="status-final">已定稿</span>
                </div>
                <div class="item-meta">
                  <span class="sketch-tag">v{{ v.versionNo }}</span>
                  <span class="sketch-tag">{{ platformLabel(v.platform) }}</span>
                  <span class="text-muted">{{ formatTime(v.createdAt) }}</span>
                </div>
              </li>
            </ul>
            <p v-else class="column-empty text-muted">暂无定稿</p>
          </div>
        </div>
      </section>

      <p class="rag-hint sketch-box">
        定稿的文章在使用快速模式 + RAG 模式时，会将当前项目以及平台作为参考修饰当前稿件。
      </p>
    </template>

    <div v-if="showModal" class="modal-mask" @click.self="showModal = false">
      <form class="modal sketch-box" @submit.prevent="submitCreate">
        <h3>新建项目</h3>
        <label class="sketch-label">项目名称</label>
        <input v-model="form.title" class="sketch-input" required />
        <label class="sketch-label">描述</label>
        <textarea v-model="form.description" class="sketch-textarea" />
        <div class="row">
          <button class="sketch-btn secondary" type="button" @click="showModal = false">
            取消
          </button>
          <button class="sketch-btn" type="submit">保存</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.projects-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 0;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

h2 {
  margin: 0 0 0.25rem;
  color: var(--cg-green-900);
}

.head-sub {
  margin: 0;
  font-size: 0.875rem;
}

.toolbar {
  padding: 0.85rem 1rem;
}

.project-select {
  max-width: 360px;
}

.project-block {
  padding: 1rem 1.15rem;
}

.project-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--cg-border);
  margin-bottom: 0.85rem;
}

.desc {
  margin: 0 0 0.25rem;
  font-size: 0.875rem;
}

.meta {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--cg-gray-400);
}

.project-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.versions-loading {
  padding: 1rem 0;
  font-size: 0.875rem;
}

.manuscript-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  align-items: start;
}

@media (max-width: 720px) {
  .manuscript-columns {
    grid-template-columns: 1fr;
  }
}

.column-title {
  margin: 0 0 0.65rem;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--cg-green-800);
}

.column-history .column-title {
  padding-left: 0.5rem;
  border-left: 3px solid var(--cg-gray-300);
}

.column-final .column-title {
  padding-left: 0.5rem;
  border-left: 3px solid var(--cg-success);
}

.manuscript-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: min(52vh, 420px);
  overflow-y: auto;
}

.manuscript-item {
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--cg-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.manuscript-item:hover {
  border-color: var(--cg-green-600);
  background: rgba(34, 84, 61, 0.04);
}

.manuscript-item--final {
  border-color: rgba(34, 120, 80, 0.25);
  background: rgba(34, 120, 80, 0.03);
}

.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
}

.item-title {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--cg-green-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
}

.status-final {
  flex-shrink: 0;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--cg-success);
}

.column-empty {
  margin: 0;
  font-size: 0.875rem;
  padding: 1rem 0.5rem;
}

.rag-hint {
  margin: 0;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--cg-gray-600);
  background: rgba(34, 84, 61, 0.05);
}

.empty {
  text-align: center;
  padding: 2.5rem;
}

.empty-title {
  margin: 0 0 0.35rem;
  font-weight: 600;
  color: var(--cg-green-900);
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.45);
  display: grid;
  place-items: center;
  z-index: 20;
}

.modal {
  width: min(420px, 92vw);
}

.modal h3 {
  margin-top: 0;
  color: var(--cg-green-900);
}

.row {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
}
</style>
