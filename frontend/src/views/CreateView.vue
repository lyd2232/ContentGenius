<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { chat, listProjects, updateVersion } from '../api'
import { chatStream } from '../api/stream'
import { copyText } from '../utils/clipboard'

const route = useRoute()
const projects = ref([])
const projectId = ref(null)
/** 创作主题：稳定锚点，对应 creationTheme */
const topic = ref('')
/** 创作要求：首版/重新生成用，可随时修改，对应 topic（本轮指令） */
const requirement = ref('')
const platform = ref('xiaohongshu')
const mode = ref('fast')
const useStream = ref(true)
const useWebSearch = ref(false)
const useRag = ref(true)
/** 思考模式多轮改稿会话 id，须在首版请求前就传给后端 */
const memoryId = ref(null)

const followupText = ref('')
/** 思考模式四步改稿：每栏独立输入，由按钮携带 thinkAction，不依赖关键词 */
const thinkSteps = [
  { action: 'title', label: '改标题', placeholder: '例如：更吸睛、突出平价、15 字以内…' },
  { action: 'outline', label: '换大纲重编', placeholder: '例如：改成清单体、增加对比（会重做四步）…' },
  { action: 'rewrite', label: '改写正文', placeholder: '例如：重写第二段、少加表情包（保留当前稿结构）…' },
  { action: 'style', label: '润色', placeholder: '例如：语气更轻松、更专业、全文少 emoji…' }
]
const thinkInputs = ref({ title: '', outline: '', rewrite: '', style: '' })
const generating = ref(false)
const preview = ref('')
const versionInfo = ref(null)
/** 0 草稿 1 已定稿 */
const versionStatus = ref(null)
const actionTip = ref('')
const finalizing = ref(false)
const error = ref('')
const abortCtrl = ref(null)
/** 本页多次生成/改稿的版本列表（与项目版本历史独立） */
const sessionDrafts = ref([])
const selectedDraftId = ref(null)

const isThinkMode = computed(() => mode.value === 'think')
const isSmartMode = computed(() => mode.value === 'smart')
/** 思考 / 智能路由：同步编排 + 多轮 memory */
const isOrchestrationMode = computed(() => isThinkMode.value || isSmartMode.value)

const hasDraft = computed(() => Boolean(preview.value?.trim()) || versionInfo.value?.versionId)

const isFinalized = computed(() => versionStatus.value === 1)

const canFinalize = computed(
  () => versionInfo.value?.versionId && preview.value?.trim() && !isFinalized.value && !generating.value
)

const followupPlaceholder = computed(() => {
  if (isSmartMode.value) {
    return '用自然语言描述改稿意图，如：润色语气更轻松、只改标题、换大纲重来…'
  }
  return '输入新的创作要求，将按此重新写一版…'
})

const htmlPreview = computed(() => {
  if (generating.value && isOrchestrationMode.value && !preview.value) {
    const hint = isSmartMode.value
      ? '智能路由编排中（SmartRouter 判意图 → 执行对应步骤）'
      : '思考模式编排中（大纲 → 正文 → 润色 → 标题）'
    return marked.parse(`_${hint}，约 1–3 分钟，请勿关闭页面…_`)
  }
  return marked.parse(preview.value || '_等待生成…_')
})

function thinkMemoryStorageKey() {
  return `cg_think_memory_${projectId.value || 'default'}`
}

function smartMemoryStorageKey() {
  return `cg_smart_memory_${projectId.value || 'default'}`
}

function orchestrationMemoryStorageKey() {
  return isSmartMode.value ? smartMemoryStorageKey() : thinkMemoryStorageKey()
}

/** 思考 / 智能路由：首版就必须带 memoryId，否则 Redis 不会存上轮稿件，改稿必失败 */
function ensureOrchestrationMemoryId() {
  if (!isOrchestrationMode.value || !projectId.value) return null
  const existing = memoryId.value
  if (existing != null && existing !== '' && !Number.isNaN(Number(existing))) {
    return Number(existing)
  }
  const stored = sessionStorage.getItem(orchestrationMemoryStorageKey())
  if (stored) {
    memoryId.value = Number(stored)
    return memoryId.value
  }
  return null
}

function fastMemoryStorageKey() {
  return `cg_fast_memory_${projectId.value || 'default'}`
}

function applyMemoryFromResponse(data) {
  if (data?.memoryId != null) {
    memoryId.value = data.memoryId
    if (projectId.value) {
      const key = isOrchestrationMode.value ? orchestrationMemoryStorageKey() : fastMemoryStorageKey()
      sessionStorage.setItem(key, String(data.memoryId))
    }
  } else if (memoryId.value == null && data?.versionId) {
    memoryId.value = data.versionId
  }
}

function resetSession() {
  memoryId.value = null
  sessionStorage.removeItem(thinkMemoryStorageKey())
  sessionStorage.removeItem(smartMemoryStorageKey())
  sessionStorage.removeItem(fastMemoryStorageKey())
  sessionDrafts.value = []
  selectedDraftId.value = null
  preview.value = ''
  versionInfo.value = null
  versionStatus.value = null
  followupText.value = ''
  requirement.value = ''
  thinkInputs.value = { title: '', outline: '', rewrite: '', style: '' }
  error.value = ''
}

function summarizeInstruction(text) {
  const t = (text || '').trim().replace(/\s+/g, ' ')
  if (!t) return '首版'
  return t.length > 36 ? `${t.slice(0, 36)}…` : t
}

function recordSessionDraft(content, data, instructionText) {
  const versionId = data?.versionId
  const entry = {
    id: versionId ?? `local-${Date.now()}`,
    versionId,
    versionNo: data?.versionNo ?? sessionDrafts.value.length + 1,
    instruction: summarizeInstruction(instructionText),
    content,
    status: 0,
    createdAt: new Date().toISOString()
  }
  sessionDrafts.value.push(entry)
  selectedDraftId.value = entry.id
}

function applySessionDraft(entry) {
  if (!entry) return
  selectedDraftId.value = entry.id
  preview.value = entry.content
  if (entry.versionId) {
    versionInfo.value = {
      versionId: entry.versionId,
      versionNo: entry.versionNo,
      content: entry.content
    }
    versionStatus.value = entry.status ?? 0
    memoryId.value = entry.versionId
  }
}

function onSelectSessionDraft() {
  const entry = sessionDrafts.value.find((d) => d.id === selectedDraftId.value)
  applySessionDraft(entry)
}

/** 首版 / 重新生成：创作要求作为本轮 instruction */
function buildStartInstruction() {
  const req = requirement.value.trim()
  if (req) return req
  return `请围绕「${topic.value.trim()}」撰写完整稿件`
}

function applyModeConstraints() {
  if (isOrchestrationMode.value) {
    useStream.value = false
    useWebSearch.value = false
    useRag.value = false
    ensureOrchestrationMemoryId()
  }
}

watch(mode, applyModeConstraints, { immediate: true })

watch(projectId, () => {
  if (isOrchestrationMode.value) {
    memoryId.value = null
    const stored = sessionStorage.getItem(orchestrationMemoryStorageKey())
    if (stored) memoryId.value = Number(stored)
  }
})

onMounted(async () => {
  projects.value = await listProjects()
  const q = Number(route.query.projectId)
  if (q) projectId.value = q
  else if (projects.value.length) projectId.value = projects.value[0].id
  if (isOrchestrationMode.value) ensureOrchestrationMemoryId()
})

function buildBody(instructionText, { thinkAction } = {}) {
  const orchestration = isOrchestrationMode.value
  const mem = orchestration ? ensureOrchestrationMemoryId() : memoryId.value
  const creationTheme = topic.value.trim()
  const body = {
    projectId: projectId.value,
    creationTheme,
    topic: instructionText,
    platform: platform.value,
    mode: mode.value,
    isopen: orchestration ? false : useWebSearch.value,
    useRag: orchestration ? false : useRag.value,
    memoryId: mem != null && mem !== '' && !Number.isNaN(Number(mem)) ? Number(mem) : undefined
  }
  if (isThinkMode.value && thinkAction) {
    body.thinkAction = thinkAction
  }
  return body
}

async function runGeneration(
  instructionText,
  { clearPreview = true, thinkAction, keepMemory = false, freshStart = false } = {}
) {
  if (!projectId.value || !topic.value?.trim()) {
    error.value = '请选择项目并填写创作主题'
    return
  }
  if (!instructionText?.trim()) {
    error.value = '请填写本轮指令或修改要求'
    return
  }
  if (freshStart && !isOrchestrationMode.value) {
    memoryId.value = null
  }
  if (isOrchestrationMode.value) {
    ensureOrchestrationMemoryId()
  } else if (keepMemory && memoryId.value == null && projectId.value) {
    const stored = sessionStorage.getItem(fastMemoryStorageKey())
    if (stored) memoryId.value = Number(stored)
  }
  error.value = ''
  // 每轮生成均替换预览，避免流式改稿把多版正文拼成一段
  preview.value = ''
  if (!keepMemory) {
    versionInfo.value = null
    versionStatus.value = null
  }
  if (clearPreview) {
    sessionDrafts.value = []
    selectedDraftId.value = null
  }
  generating.value = true
  const instructionLabel = instructionText.trim()
  abortCtrl.value = new AbortController()

  const body = buildBody(instructionText.trim(), { thinkAction })
  const syncOnly = isOrchestrationMode.value || !useStream.value

  try {
    if (syncOnly) {
      const data = await chat(body)
      if (!data?.content) {
        throw new Error('接口成功但未返回正文，请查看 agent-service 日志')
      }
      preview.value = data.content
      versionInfo.value = data
      versionStatus.value = 0
      recordSessionDraft(data.content, data, instructionLabel)
      applyMemoryFromResponse(data)
      generating.value = false
    } else {
      await chatStream(body, {
        onChunk: (text) => {
          preview.value += text
        },
        onDone: (data) => {
          if (data?.content?.trim()) {
            preview.value = data.content
          }
          versionInfo.value = data
          versionStatus.value = 0
          recordSessionDraft(preview.value, data, instructionLabel)
          applyMemoryFromResponse(data)
          generating.value = false
        },
        onError: (e) => {
          error.value = e?.message || '流式失败'
          generating.value = false
        }
      })
    }
  } catch (e) {
    error.value = e.message || String(e)
    generating.value = false
  }
}

async function start() {
  if (!requirement.value.trim()) {
    error.value = '请填写创作要求（风格、结构、语气等）'
    return
  }
  // 开始生成 = 首版：不带 memoryId，避免误走「快速模式改稿」
  await runGeneration(buildStartInstruction(), { freshStart: true })
}

async function submitFollowup() {
  const text = followupText.value.trim()
  if (!text) {
    error.value = '请输入修改意见或新的创作要求'
    return
  }
  if (!hasDraft.value) {
    error.value = '请先生成一版内容，再在此输入改稿意见'
    return
  }
  followupText.value = ''
  await runGeneration(text, { clearPreview: false, keepMemory: true })
}

async function submitThinkStep(action) {
  const text = (thinkInputs.value[action] || '').trim()
  if (!text) {
    error.value = '请填写该步骤的修改要求'
    return
  }
  if (!hasDraft.value) {
    error.value = '请先用左侧「开始生成」完成首版，再使用下方四步改稿'
    return
  }
  ensureOrchestrationMemoryId()
  thinkInputs.value[action] = ''
  await runGeneration(text, { clearPreview: false, thinkAction: action })
}

async function regenerate() {
  if (!topic.value.trim()) {
    error.value = '请先在左侧填写创作主题'
    return
  }
  if (!requirement.value.trim()) {
    error.value = '请填写创作要求后再重新生成'
    return
  }
  // 重新生成 = 按当前「主题+要求」写新稿，不带旧 memoryId
  await runGeneration(buildStartInstruction(), { freshStart: true, clearPreview: false })
}

function stop() {
  abortCtrl.value?.abort()
  generating.value = false
}

function onFollowupKeydown(e) {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    if (!generating.value) submitFollowup()
  }
}

function flashActionTip(msg) {
  actionTip.value = msg
  setTimeout(() => {
    if (actionTip.value === msg) actionTip.value = ''
  }, 2200)
}

async function copyPreview() {
  try {
    await copyText(preview.value)
    flashActionTip('已复制到剪贴板')
  } catch (e) {
    flashActionTip(e.message || '复制失败')
  }
}

async function finalizeDraft() {
  const id = versionInfo.value?.versionId
  if (!id || !preview.value?.trim()) return
  if (
    !window.confirm(
      '定稿后将把当前正文写入 RAG 向量库，供后续开启 RAG 的创作检索参考。确定定稿？'
    )
  ) {
    return
  }
  finalizing.value = true
  error.value = ''
  try {
    const updated = await updateVersion(id, { status: 1 })
    versionStatus.value = updated?.status ?? 1
    const idx = sessionDrafts.value.findIndex((d) => d.versionId === id)
    if (idx >= 0) sessionDrafts.value[idx].status = 1
    flashActionTip('已定稿，正在写入知识库（异步）')
  } catch (e) {
    error.value = e.message || '定稿失败'
  } finally {
    finalizing.value = false
  }
}
</script>

<template>
  <div class="create-page">
  <div class="create-layout">
    <section class="panel sketch-box">
      <h2>AI 创作</h2>
      <p class="panel-hint text-muted">
        <template v-if="isThinkMode">
          思考模式：同步四步编排，多轮改稿会话在后台自动维持
        </template>
        <template v-else-if="isSmartMode">
          智能路由：由 SmartRouter（LLM）判断改稿意图，多轮会话在后台自动维持
        </template>
        <template v-else>快速模式：支持流式、RAG、联网</template>
      </p>
      <label class="sketch-label">选择项目</label>
      <select v-model="projectId" class="sketch-select">
        <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.title }}</option>
      </select>
      <label class="sketch-label">创作主题</label>
      <input v-model="topic" class="sketch-input" placeholder="例如：巧克力推荐、键盘推荐" />
      <p class="field-hint text-muted">保持稳定，用于 RAG / 会话记忆；换题请点「新会话」或改主题后重新生成。</p>
      <label class="sketch-label">创作要求</label>
      <textarea
        v-model="requirement"
        class="sketch-textarea requirement-input"
        rows="3"
        placeholder="例如：清单体、突出平价、语气口语化、少 emoji…"
      />
      <p class="field-hint text-muted">可随时修改；「开始生成」与「重新生成」以这里为准。</p>
      <label class="sketch-label">平台</label>
      <select v-model="platform" class="sketch-select">
        <option value="xiaohongshu">小红书</option>
        <option value="wechat">公众号</option>
        <option value="bilibili">B站</option>
      </select>
      <label class="sketch-label">模式</label>
      <select v-model="mode" class="sketch-select">
        <option value="fast">快速</option>
        <option value="think">思考（四步）</option>
        <option value="smart">智能路由</option>
      </select>
      <p v-if="isSmartMode" class="smart-route-warn">
        当前准确性待优化：改稿意图由模型自动识别，可能误判为润色 / 改写 / 四步重来等，建议用于技术演示。
      </p>
      <p class="session-warn">
        <template v-if="isOrchestrationMode">
          点击「新会话」清空预览与改稿记忆；之后需重新「开始生成」才能继续分栏 / 智能改稿。
        </template>
        <template v-else>
          点击「新会话」清空预览与 memoryId；之后「开始生成」将按「主题 + 要求」新写一篇（不会接着旧稿改）。
        </template>
      </p>
      <button
        class="sketch-btn secondary new-session-btn"
        type="button"
        :disabled="generating"
        @click="resetSession"
      >
        新会话
      </button>
      <div v-if="!isOrchestrationMode" class="toggles">
        <label class="toggle"><input v-model="useStream" type="checkbox" /> 流式输出</label>
        <label class="toggle"><input v-model="useWebSearch" type="checkbox" /> 联网搜索</label>
        <label class="toggle"><input v-model="useRag" type="checkbox" /> RAG</label>
      </div>
      <p v-else-if="isThinkMode" class="think-note">
        已禁用流式 / RAG / 联网。首版填「主题 + 要求」后点「开始生成」；细改在预览区分栏。
      </p>
      <p v-else class="think-note">
        已禁用流式 / RAG / 联网。首版填「主题 + 要求」；改稿在预览区用自然语言（SmartRouter）。
      </p>
      <div class="actions">
        <button class="sketch-btn" type="button" :disabled="generating" @click="start">
          {{ generating ? (isSmartMode ? '路由中…' : '编排中…') : '开始生成' }}
        </button>
        <button v-if="generating && !isOrchestrationMode" class="sketch-btn danger" type="button" @click="stop">
          停止
        </button>
      </div>
      <p v-if="error" class="text-error">{{ error }}</p>
      <p v-if="versionInfo?.versionId" class="text-success version-ok">
        已存草稿 v{{ versionInfo.versionNo }}
        <span v-if="isFinalized">· 已定稿</span>
        <span v-else class="text-muted">· 未定稿</span>
      </p>
    </section>

    <section class="preview-column sketch-box">
      <div class="preview-head">
        <h3>预览</h3>
        <div class="preview-tools">
          <span v-if="actionTip" class="action-tip">{{ actionTip }}</span>
          <button
            class="sketch-btn secondary btn-sm"
            type="button"
            :disabled="!preview?.trim() || generating"
            @click="copyPreview"
          >
            复制全文
          </button>
          <button
            class="sketch-btn btn-sm"
            type="button"
            :disabled="!canFinalize || finalizing"
            @click="finalizeDraft"
          >
            {{ finalizing ? '定稿中…' : isFinalized ? '已定稿' : '定稿' }}
          </button>
        </div>
      </div>

      <div v-if="sessionDrafts.length" class="draft-picker sketch-box">
        <label class="sketch-label" for="session-draft-select">本页版本（{{ sessionDrafts.length }}）</label>
        <select
          id="session-draft-select"
          v-model="selectedDraftId"
          class="sketch-select"
          :disabled="generating"
          @change="onSelectSessionDraft"
        >
          <option v-for="d in sessionDrafts" :key="d.id" :value="d.id">
            第 {{ d.versionNo }} 版 · {{ d.instruction }}{{ d.status === 1 ? '（已定稿）' : '' }}
          </option>
        </select>
        <p class="draft-picker-hint text-muted">
          每次生成/改稿单独成版，用下拉切换预览；不会再把多版正文拼在一起。
        </p>
      </div>

      <div
        class="markdown-body preview-scroll"
        :class="{ typewriter: generating && !isOrchestrationMode }"
        v-html="htmlPreview"
      />

      <div class="followup-bar">
        <template v-if="isThinkMode">
          <p class="followup-bar-title">局部改稿（每栏只填具体要求）</p>
          <div
            v-for="step in thinkSteps"
            :key="step.action"
            class="think-step-row"
          >
            <span class="think-step-label">{{ step.label }}</span>
            <input
              v-model="thinkInputs[step.action]"
              class="think-step-input"
              type="text"
              :placeholder="step.placeholder"
              :disabled="generating"
              @keydown.enter.prevent="!generating && hasDraft && submitThinkStep(step.action)"
            />
            <button
              class="followup-btn primary think-step-btn"
              type="button"
              :disabled="generating || !hasDraft"
              @click="submitThinkStep(step.action)"
            >
              应用
            </button>
          </div>
        </template>
        <template v-else-if="isSmartMode">
          <p class="followup-bar-title">智能改稿（自然语言，由 SmartRouter 判意图）</p>
          <textarea
            v-model="followupText"
            class="followup-input"
            :placeholder="followupPlaceholder"
            :disabled="generating"
            rows="3"
            @keydown="onFollowupKeydown"
          />
          <div class="followup-actions">
            <button
              class="followup-btn primary"
              type="button"
              :disabled="generating || !hasDraft"
              @click="submitFollowup"
            >
              发送（智能路由）
            </button>
          </div>
          <p class="followup-hint smart-route-hint">
            当前准确性待优化 · 示例：「润色得更口语」「只改标题」「换大纲重来」· Ctrl+Enter 发送
          </p>
        </template>
        <template v-else>
          <textarea
            v-model="followupText"
            class="followup-input"
            :placeholder="followupPlaceholder"
            :disabled="generating"
            rows="3"
            @keydown="onFollowupKeydown"
          />
          <div class="followup-actions">
            <button
              class="followup-btn primary"
              type="button"
              :disabled="generating || !hasDraft"
              @click="submitFollowup"
            >
              发送改稿
            </button>
            <button
              class="followup-btn secondary"
              type="button"
              :disabled="generating"
              @click="regenerate"
            >
              重新生成一版
            </button>
          </div>
          <p class="followup-hint">快速模式可流式；Ctrl+Enter 发送</p>
        </template>
      </div>
    </section>
  </div>
  </div>
</template>

<style scoped>
.create-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.create-layout {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
  gap: 0.75rem;
  align-items: stretch;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.panel {
  min-height: 0;
  overflow-y: auto;
  padding: 1rem 1.1rem;
}

@media (max-width: 900px) {
  .create-page {
    overflow: auto;
    height: auto;
  }

  .create-layout {
    grid-template-columns: 1fr;
    height: auto;
    overflow: visible;
  }

  .panel {
    max-height: none;
  }

  .preview-column {
    min-height: 60vh;
  }
}

h2,
h3 {
  margin-top: 0;
  color: var(--cg-green-900);
}

.panel h2 {
  margin: 0 0 0.35rem;
  font-size: 1.15rem;
}

.panel-hint {
  margin: 0 0 0.75rem;
  font-size: 0.75rem;
}

.field-hint {
  margin: 0.2rem 0 0.5rem;
  font-size: 0.7rem;
  line-height: 1.35;
}

.requirement-input {
  min-height: 3.25rem;
  max-height: 5rem;
  resize: none;
}

.session-warn {
  margin: 0.4rem 0 0.35rem;
  font-size: 0.7rem;
  line-height: 1.35;
  color: var(--cg-gray-600);
}

.new-session-btn {
  width: 100%;
  margin-bottom: 0.25rem;
}

.smart-route-warn {
  margin: 0.5rem 0 0;
  padding: 0.5rem 0.65rem;
  font-size: 0.8rem;
  line-height: 1.45;
  color: #c9a227;
  background: rgba(201, 162, 39, 0.12);
  border: 1px solid rgba(201, 162, 39, 0.35);
  border-radius: 6px;
}

.smart-route-hint {
  color: #c9a227;
}

.think-note {
  margin: 0.75rem 0;
  padding: 0.65rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--cg-green-900);
  background: var(--cg-green-50);
  border: 1px solid rgba(45, 106, 79, 0.2);
  border-radius: var(--cg-radius-sm);
}

.toggles {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin: 0.75rem 0;
  padding: 0.75rem;
  background: var(--cg-gray-50);
  border-radius: var(--cg-radius-sm);
  border: 1px solid var(--cg-border);
}

.toggle {
  font-size: 0.875rem;
  color: var(--cg-gray-700);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.toggle input {
  accent-color: var(--cg-green-800);
}

.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.preview-column {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  min-height: 0;
  height: 100%;
}

.draft-picker {
  margin-bottom: 0.75rem;
  padding: 0.65rem 0.75rem;
  background: rgba(34, 84, 61, 0.04);
  border: 1px solid var(--cg-border);
  border-radius: 8px;
}

.draft-picker .sketch-select {
  width: 100%;
  max-width: 100%;
}

.draft-picker-hint {
  margin: 0.4rem 0 0;
  font-size: 0.75rem;
  line-height: 1.4;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  padding: 1.25rem 1.5rem 0.5rem;
}

.preview-head h3 {
  margin: 0;
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

.preview-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 1.25rem 0.75rem;
  min-height: 0;
}

.followup-bar {
  flex-shrink: 0;
  max-height: 42%;
  overflow-y: auto;
  padding: 0.75rem 1rem 0.85rem;
  background: linear-gradient(160deg, var(--cg-green-900) 0%, var(--cg-green-950) 100%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.followup-input {
  width: 100%;
  box-sizing: border-box;
  font-family: inherit;
  font-size: 0.9375rem;
  line-height: 1.5;
  padding: 0.75rem 1rem;
  border-radius: var(--cg-radius-sm);
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(0, 0, 0, 0.22);
  color: #f0f4f1;
  resize: vertical;
  min-height: 72px;
}

.followup-input::placeholder {
  color: rgba(232, 240, 235, 0.45);
}

.followup-input:focus {
  outline: none;
  border-color: var(--cg-green-600);
  box-shadow: 0 0 0 3px rgba(82, 183, 136, 0.25);
}

.followup-input:disabled {
  opacity: 0.6;
}

.followup-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.65rem;
  flex-wrap: wrap;
}

.followup-btn {
  font-family: inherit;
  font-size: 0.875rem;
  font-weight: 500;
  padding: 0.45rem 1rem;
  border-radius: var(--cg-radius-sm);
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s ease;
}

.followup-btn.primary {
  background: var(--cg-green-600);
  color: #fff;
}

.followup-btn.primary:hover:not(:disabled) {
  background: var(--cg-green-700);
}

.followup-btn.secondary {
  background: transparent;
  color: #e8f0eb;
  border-color: rgba(255, 255, 255, 0.35);
}

.followup-btn.secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.08);
}

.followup-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.followup-hint {
  margin: 0.5rem 0 0;
  font-size: 0.75rem;
  color: rgba(232, 240, 235, 0.55);
  line-height: 1.4;
}

.followup-bar-title {
  margin: 0 0 0.65rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: rgba(232, 240, 235, 0.85);
}

.think-step-row {
  display: grid;
  grid-template-columns: 4.75rem 1fr auto;
  gap: 0.4rem;
  align-items: center;
  margin-bottom: 0.35rem;
}

.think-step-label {
  font-size: 0.8125rem;
  color: rgba(232, 240, 235, 0.75);
  white-space: nowrap;
}

.think-step-input {
  font-family: inherit;
  font-size: 0.875rem;
  padding: 0.45rem 0.65rem;
  border-radius: var(--cg-radius-sm);
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(0, 0, 0, 0.22);
  color: #f0f4f1;
  min-width: 0;
}

.think-step-input::placeholder {
  color: rgba(232, 240, 235, 0.4);
  font-size: 0.8125rem;
}

.think-step-input:focus {
  outline: none;
  border-color: var(--cg-green-600);
}

.think-step-btn {
  white-space: nowrap;
  padding: 0.4rem 0.75rem;
}

@media (max-width: 640px) {
  .think-step-row {
    grid-template-columns: 1fr;
  }

  .think-step-label {
    margin-bottom: -0.25rem;
  }

  .think-step-btn {
    justify-self: start;
  }
}

.version-ok {
  font-size: 0.8125rem;
  margin-top: 0.75rem;
}
</style>
