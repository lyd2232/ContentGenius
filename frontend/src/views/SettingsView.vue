<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, deleteAccount, fetchMe, updateProfile } from '../api'
import { clearToken } from '../api/http'

const router = useRouter()
const panel = ref('hub')
const loading = ref(true)
const savingProfile = ref(false)
const savingPassword = ref(false)
const deleting = ref(false)
const error = ref('')
const success = ref('')

const profile = ref({
  username: '',
  email: '',
  phone: '',
  memberLevel: 0
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const deletePassword = ref('')

const menuItems = [
  {
    id: 'profile',
    title: '账号资料',
    desc: '邮箱、手机号',
    icon: '👤'
  },
  {
    id: 'password',
    title: '修改密码',
    desc: '定期更换更安全',
    icon: '🔒'
  },
  {
    id: 'delete',
    title: '注销账号',
    desc: '永久删除当前账号',
    icon: '⚠️',
    danger: true
  },
  {
    id: 'about',
    title: '关于',
    desc: '版本与开发说明',
    icon: 'ℹ️'
  }
]

const panelTitle = computed(() => {
  const map = {
    hub: '设置',
    profile: '账号资料',
    password: '修改密码',
    delete: '注销账号',
    about: '关于'
  }
  return map[panel.value] || '设置'
})

const memberLabel = computed(() => {
  const level = profile.value.memberLevel ?? 0
  if (level >= 2) return '内测用户（每日 10 次）'
  if (level === 1) return '标准会员'
  return '免费版'
})

const memberTagClass = computed(() => (profile.value.memberLevel >= 2 ? 'sketch-tag--vip' : ''))

onMounted(async () => {
  try {
    profile.value = await fetchMe()
  } catch (e) {
    error.value = e.message || '加载账号信息失败'
  } finally {
    loading.value = false
  }
})

function openPanel(id) {
  error.value = ''
  success.value = ''
  panel.value = id
}

function goHub() {
  error.value = ''
  success.value = ''
  panel.value = 'hub'
}

function flash(msg) {
  success.value = msg
  error.value = ''
  setTimeout(() => {
    success.value = ''
  }, 3000)
}

async function saveProfile() {
  savingProfile.value = true
  error.value = ''
  try {
    await updateProfile({
      email: profile.value.email?.trim() || '',
      phone: profile.value.phone?.trim() || ''
    })
    flash('资料已保存')
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    savingProfile.value = false
  }
}

async function submitPassword() {
  error.value = ''
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  savingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    flash('密码已修改，下次登录请使用新密码')
  } catch (e) {
    error.value = e.message || '修改密码失败'
  } finally {
    savingPassword.value = false
  }
}

async function confirmDelete() {
  if (!deletePassword.value) {
    error.value = '请输入密码以确认注销'
    return
  }
  if (!window.confirm('注销后账号与数据将无法恢复，确定继续？')) {
    return
  }
  deleting.value = true
  error.value = ''
  try {
    await deleteAccount(deletePassword.value)
    clearToken()
    router.push('/login')
  } catch (e) {
    error.value = e.message || '注销失败'
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="settings-page">
    <div class="sketch-box settings">
      <header class="settings-head">
        <button
          v-if="panel !== 'hub'"
          class="back-btn"
          type="button"
          @click="goHub"
        >
          ← 返回
        </button>
        <h2>{{ panelTitle }}</h2>
      </header>

      <p v-if="loading" class="text-muted">加载中…</p>
      <p v-else-if="error" class="text-error banner">{{ error }}</p>
      <p v-else-if="success" class="text-success banner">{{ success }}</p>

      <!-- 入口列表 -->
      <template v-if="!loading && panel === 'hub'">
        <div class="user-card">
          <img src="/logo.png" alt="" class="user-avatar" width="48" height="48" />
          <div class="user-meta">
            <div class="user-name">{{ profile.username }}</div>
            <span class="sketch-tag" :class="memberTagClass">{{ memberLabel }}</span>
          </div>
        </div>

        <nav class="menu-list">
          <button
            v-for="item in menuItems"
            :key="item.id"
            class="menu-item"
            :class="{ 'menu-item--danger': item.danger }"
            type="button"
            @click="openPanel(item.id)"
          >
            <span class="menu-icon" aria-hidden="true">{{ item.icon }}</span>
            <span class="menu-text">
              <span class="menu-title">{{ item.title }}</span>
              <span class="menu-desc">{{ item.desc }}</span>
            </span>
            <span class="menu-arrow">›</span>
          </button>
        </nav>
      </template>

      <!-- 账号资料 -->
      <section v-else-if="!loading && panel === 'profile'" class="panel-form">
        <p class="block-desc text-muted">用户名不可修改；邮箱与手机号可选填。</p>
        <div class="form-grid">
          <label class="sketch-label">用户名</label>
          <input class="sketch-input" :value="profile.username" disabled />

          <label class="sketch-label">会员等级</label>
          <input class="sketch-input" :value="memberLabel" disabled />

          <label class="sketch-label">邮箱</label>
          <input v-model="profile.email" class="sketch-input" type="email" autocomplete="email" />

          <label class="sketch-label">手机号</label>
          <input v-model="profile.phone" class="sketch-input" type="tel" autocomplete="tel" />
        </div>
        <button class="sketch-btn" type="button" :disabled="savingProfile" @click="saveProfile">
          {{ savingProfile ? '保存中…' : '保存资料' }}
        </button>
      </section>

      <!-- 修改密码 -->
      <section v-else-if="!loading && panel === 'password'" class="panel-form">
        <p class="block-desc text-muted">修改成功后需使用新密码重新登录。</p>
        <div class="form-grid">
          <label class="sketch-label">当前密码</label>
          <input
            v-model="passwordForm.oldPassword"
            class="sketch-input"
            type="password"
            autocomplete="current-password"
          />

          <label class="sketch-label">新密码</label>
          <input
            v-model="passwordForm.newPassword"
            class="sketch-input"
            type="password"
            minlength="6"
            autocomplete="new-password"
          />

          <label class="sketch-label">确认新密码</label>
          <input
            v-model="passwordForm.confirmPassword"
            class="sketch-input"
            type="password"
            minlength="6"
            autocomplete="new-password"
          />
        </div>
        <button class="sketch-btn" type="button" :disabled="savingPassword" @click="submitPassword">
          {{ savingPassword ? '提交中…' : '确认修改' }}
        </button>
      </section>

      <!-- 注销账号 -->
      <section v-else-if="!loading && panel === 'delete'" class="panel-form danger-zone">
        <p class="block-desc text-muted">
          永久删除当前账号。关联的项目与草稿不会自动清理。
        </p>
        <label class="sketch-label">输入密码确认注销</label>
        <input
          v-model="deletePassword"
          class="sketch-input"
          type="password"
          autocomplete="current-password"
        />
        <button class="sketch-btn danger" type="button" :disabled="deleting" @click="confirmDelete">
          {{ deleting ? '注销中…' : '确认注销账号' }}
        </button>
      </section>

      <!-- 关于 -->
      <section v-else-if="panel === 'about'" class="panel-form about">
        <p class="about-version">ContentGenius v1.0.0</p>
        <p class="text-muted">多平台 AI 内容创作工作台</p>
        <ul class="about-list text-muted">
          <li>LangChain4j Agent 流式创作</li>
          <li>项目版本与 RAG 知识库</li>
          <li>图片风格解析（Vision）</li>
        </ul>
        <p class="text-muted about-api">
          API：<code>http://localhost:8080</code> · 开发代理 <code>/api</code>
        </p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 480px;
}

.settings-head {
  margin-bottom: 1rem;
}

.settings-head h2 {
  margin: 0.35rem 0 0;
  color: var(--cg-green-900);
}

.back-btn {
  border: none;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 0.875rem;
  color: var(--cg-green-800);
  cursor: pointer;
  font-weight: 500;
}

.back-btn:hover {
  text-decoration: underline;
}

.banner {
  margin: 0 0 1rem;
  font-size: 0.875rem;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem;
  margin-bottom: 1rem;
  background: var(--cg-green-50);
  border: 1px solid rgba(45, 106, 79, 0.15);
  border-radius: var(--cg-radius);
}

.user-avatar {
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.user-name {
  font-weight: 600;
  font-size: 1.05rem;
  color: var(--cg-green-900);
  margin-bottom: 0.25rem;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.85rem 1rem;
  border: 1px solid var(--cg-border);
  border-radius: var(--cg-radius-sm);
  background: var(--cg-surface);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.menu-item:hover {
  background: var(--cg-gray-50);
  border-color: var(--cg-green-700);
}

.menu-item--danger .menu-title {
  color: var(--cg-danger);
}

.menu-icon {
  font-size: 1.15rem;
  line-height: 1;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.menu-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--cg-gray-900);
}

.menu-desc {
  font-size: 0.75rem;
  color: var(--cg-text-muted);
}

.menu-arrow {
  font-size: 1.25rem;
  color: var(--cg-gray-400);
  line-height: 1;
}

.panel-form {
  padding-top: 0.25rem;
}

.block-desc {
  margin: 0 0 1rem;
  font-size: 0.8125rem;
  line-height: 1.45;
}

.form-grid {
  display: grid;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.danger-zone .block-desc {
  color: var(--cg-gray-600);
}

.about-version {
  margin: 0 0 0.35rem;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--cg-green-900);
}

.about-list {
  margin: 1rem 0;
  padding-left: 1.2rem;
  font-size: 0.875rem;
  line-height: 1.7;
}

.about-api {
  font-size: 0.8125rem;
  margin: 0;
}

code {
  font-size: 0.8125rem;
  background: var(--cg-gray-100);
  padding: 0.15em 0.4em;
  border-radius: 4px;
  color: var(--cg-green-900);
}

.sketch-input:disabled {
  background: var(--cg-gray-50);
  color: var(--cg-gray-500);
}
</style>
