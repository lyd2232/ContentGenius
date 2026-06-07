<script setup>
import { computed, onMounted, ref } from 'vue'
import { listFeedback, submitFeedback } from '../api'

const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const success = ref('')

const items = ref([])
const myCount = ref(0)
const maxPerUser = ref(3)
const content = ref('')

const canSubmit = computed(() => myCount.value < maxPerUser.value && content.value.trim().length > 0)
const remainHint = computed(() => {
  const left = Math.max(0, maxPerUser.value - myCount.value)
  return left > 0 ? `你还可以提交 ${left} 条` : '你已用完 3 条提交额度'
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await listFeedback()
    items.value = data?.items || []
    myCount.value = data?.myCount ?? 0
    maxPerUser.value = data?.maxPerUser ?? 3
  } catch (e) {
    error.value = e.message || '加载意见列表失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const data = await submitFeedback(content.value.trim())
    content.value = ''
    myCount.value = data?.myCount ?? myCount.value
    success.value = '感谢反馈，我们已收到'
    await load()
  } catch (e) {
    error.value = e.message || '提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sketch-box feedback-page">
    <h2>意见箱</h2>
    <p class="intro text-muted">
      当前为<strong>内测阶段</strong>：新注册用户默认享有<strong>每日 10 次</strong> AI 创作额度，欢迎把使用感受与改进建议告诉我们。
    </p>

    <section v-if="myCount < maxPerUser" class="submit-box">
      <label class="sketch-label" for="feedback-input">写下你的意见</label>
      <textarea
        id="feedback-input"
        v-model="content"
        class="sketch-textarea feedback-input"
        rows="3"
        maxlength="500"
        placeholder="例如：希望增加导出 Word、改稿速度再快一点…"
        :disabled="submitting"
      />
      <div class="submit-row">
        <span class="remain text-muted">{{ remainHint }}</span>
        <button class="sketch-btn" type="button" :disabled="submitting || !canSubmit" @click="submit">
          {{ submitting ? '提交中…' : '提交意见' }}
        </button>
      </div>
    </section>
    <p v-else class="quota-done text-muted">{{ remainHint }}</p>

    <p v-if="error" class="text-error msg">{{ error }}</p>
    <p v-if="success" class="text-success msg">{{ success }}</p>

    <section class="list-section">
      <h3>大家的声音</h3>
      <p class="list-hint text-muted">所有用户可见；每人最多保留 3 条。</p>
      <p v-if="loading" class="text-muted">加载中…</p>
      <div v-else-if="!items.length" class="empty text-muted">还没有意见，来做第一个吧</div>
      <table v-else class="feedback-table">
        <thead>
          <tr>
            <th>用户名称</th>
            <th>意见</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in items" :key="row.id">
            <td class="col-user">{{ row.username }}</td>
            <td class="col-content">{{ row.content }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
h2 {
  margin-top: 0;
  color: var(--cg-green-900);
}

h3 {
  margin: 0 0 0.35rem;
  font-size: 0.9375rem;
  color: var(--cg-gray-700);
}

.intro {
  margin: 0 0 1.25rem;
  font-size: 0.875rem;
  line-height: 1.6;
}

.intro strong {
  color: var(--cg-green-800);
  font-weight: 600;
}

.submit-box {
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--cg-border);
}

.feedback-input {
  min-height: 5rem;
  resize: vertical;
  margin-bottom: 0.65rem;
}

.submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.remain {
  font-size: 0.8125rem;
}

.quota-done {
  margin: 0 0 1rem;
  padding: 0.65rem 0.75rem;
  background: var(--cg-gray-50);
  border-radius: var(--cg-radius-sm);
  font-size: 0.875rem;
}

.msg {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
}

.list-hint {
  margin: 0 0 0.75rem;
  font-size: 0.8125rem;
}

.empty {
  padding: 1.5rem;
  text-align: center;
  background: var(--cg-gray-50);
  border-radius: var(--cg-radius-sm);
  font-size: 0.875rem;
}

.feedback-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

th,
td {
  border: 1px solid var(--cg-border);
  padding: 0.7rem 0.85rem;
  text-align: left;
  vertical-align: top;
}

th {
  background: var(--cg-gray-50);
  font-weight: 600;
  color: var(--cg-gray-700);
  font-size: 0.75rem;
}

.col-user {
  width: 7.5rem;
  white-space: nowrap;
  font-weight: 500;
  color: var(--cg-green-900);
}

.col-content {
  line-height: 1.5;
  color: var(--cg-gray-700);
  word-break: break-word;
}
</style>
