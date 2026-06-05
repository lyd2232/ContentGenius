<script setup>
import { onMounted, ref } from 'vue'
import { uploadFile, listFiles, visionAnalyze, deleteFile } from '../api'

const files = ref([])
const loading = ref(false)
const platform = ref('xiaohongshu')
const analyzing = ref(false)
const styleHint = ref('')
const error = ref('')

function mapFileRow(data) {
  return {
    objectName: data.objectName,
    url: data.url,
    name: data.displayName || data.objectName?.split('/').pop() || '未命名文件'
  }
}

async function loadFiles() {
  loading.value = true
  error.value = ''
  try {
    const rows = await listFiles()
    files.value = (rows || []).map(mapFileRow)
  } catch (err) {
    error.value = err.message || '加载素材列表失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadFiles)

async function onPick(e) {
  const file = e.target.files?.[0]
  if (!file) return
  error.value = ''
  styleHint.value = ''
  try {
    const data = await uploadFile(file)
    files.value.unshift(mapFileRow({ ...data, displayName: data.displayName || file.name }))
    if (files.value.length > 3) {
      files.value = files.value.slice(0, 3)
    }
  } catch (err) {
    error.value = err.message
  }
  e.target.value = ''
}

async function analyze(item) {
  analyzing.value = true
  error.value = ''
  try {
    const res = await visionAnalyze({
      objectName: item.objectName,
      platform: platform.value
    })
    styleHint.value = res.styleHint
  } catch (err) {
    error.value = err.message
  } finally {
    analyzing.value = false
  }
}

async function remove(item) {
  error.value = ''
  try {
    await deleteFile(item.objectName)
    files.value = files.value.filter((f) => f.objectName !== item.objectName)
  } catch (err) {
    error.value = err.message || '删除失败'
  }
}
</script>

<template>
  <div class="materials">
    <header class="page-head">
      <h2>素材库</h2>
      <p class="text-muted">
        每用户最多 3 张；上传后可调用 Vision 解析写作风格
        <span v-if="files.length">（已上传 {{ files.length }}/3）</span>
      </p>
    </header>

    <div class="upload-zone" @click="$refs.fileInput.click()">
      <input ref="fileInput" type="file" accept="image/*" hidden @change="onPick" />
      <p class="upload-title">点击或拖拽上传图片</p>
      <p class="upload-hint text-muted">支持 JPG / PNG，单用户上限 3 个文件</p>
    </div>

    <label class="sketch-label">解析目标平台</label>
    <select v-model="platform" class="sketch-select platform-select">
      <option value="xiaohongshu">小红书</option>
      <option value="wechat">公众号</option>
      <option value="bilibili">B站</option>
    </select>

    <p v-if="loading" class="text-muted">正在加载素材列表…</p>
    <p v-if="error" class="text-error">{{ error }}</p>

    <div v-if="files.length" class="table-wrap sketch-box">
      <table class="table">
        <thead>
          <tr>
            <th>文件</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in files" :key="f.objectName">
            <td>
              <a :href="f.url" target="_blank" rel="noopener" class="file-link">{{ f.name }}</a>
            </td>
            <td class="cell-actions">
              <button
                class="sketch-btn secondary"
                type="button"
                :disabled="analyzing"
                @click="analyze(f)"
              >
                解析风格
              </button>
              <button class="sketch-btn danger" type="button" @click="remove(f)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <aside v-if="styleHint" class="hint sketch-box">
      <h3>写作风格建议</h3>
      <div class="hint-body">{{ styleHint }}</div>
    </aside>
  </div>
</template>

<style scoped>
.page-head h2 {
  margin: 0 0 0.25rem;
  color: var(--cg-green-900);
}

.page-head p {
  margin: 0 0 1.25rem;
  font-size: 0.875rem;
}

.upload-zone {
  text-align: center;
  padding: 2rem;
  cursor: pointer;
  margin-bottom: 1rem;
  border: 2px dashed var(--cg-green-700);
  border-radius: var(--cg-radius);
  background: var(--cg-green-50);
  transition: background 0.15s ease, border-color 0.15s ease;
}

.upload-zone:hover {
  background: #e8f5ec;
  border-color: var(--cg-green-800);
}

.upload-title {
  margin: 0;
  font-weight: 600;
  color: var(--cg-green-900);
}

.upload-hint {
  margin: 0.35rem 0 0;
  font-size: 0.8125rem;
}

.platform-select {
  max-width: 240px;
  margin-bottom: 1rem;
}

.table-wrap {
  padding: 0;
  overflow: hidden;
  margin-bottom: 1rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--cg-border);
  font-size: 0.875rem;
}

th {
  background: var(--cg-gray-50);
  font-weight: 600;
  color: var(--cg-gray-700);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.file-link {
  color: var(--cg-green-800);
  text-decoration: none;
  font-weight: 500;
}

.file-link:hover {
  text-decoration: underline;
}

.cell-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.hint h3 {
  margin-top: 0;
  font-size: 1rem;
  color: var(--cg-green-900);
}

.hint-body {
  white-space: pre-wrap;
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--cg-gray-700);
  max-height: 320px;
  overflow: auto;
}
</style>
