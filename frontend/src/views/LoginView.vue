<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'
import { setToken } from '../api/http'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const token = await login(username.value, password.value)
    setToken(token)
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <aside class="login-brand">
      <div class="brand-mark" aria-hidden="true" />
      <h1>ContentGenius</h1>
      <p class="tagline">多平台 AI 写稿，从主题到定稿</p>
      <ul class="features">
        <li>LangChain4j Agent 流式创作</li>
        <li>项目版本与 RAG 知识库</li>
        <li>图片风格解析（Vision）</li>
      </ul>
    </aside>
    <form class="login-card sketch-box" @submit.prevent="submit">
      <h2>登录</h2>
      <p class="sub text-muted">使用账号进入创作工作台</p>
      <label class="sketch-label">用户名</label>
      <input v-model="username" class="sketch-input" required autocomplete="username" />
      <label class="sketch-label">密码</label>
      <input
        v-model="password"
        type="password"
        class="sketch-input"
        required
        autocomplete="current-password"
      />
      <p v-if="error" class="err text-error">{{ error }}</p>
      <button class="sketch-btn login-submit" type="submit" :disabled="loading">
        {{ loading ? '登录中…' : '进入工作台' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
}

@media (max-width: 768px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-brand {
    padding: 2rem 1.5rem !important;
  }
}

.login-brand {
  background: linear-gradient(160deg, var(--cg-green-950) 0%, var(--cg-green-900) 45%, var(--cg-green-800) 100%);
  color: #e8f0eb;
  padding: 3rem 2.5rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  background: linear-gradient(145deg, #1b4332 0%, #0d2818 100%);
  box-shadow: 0 0 0 3px rgba(201, 162, 39, 0.45);
  margin-bottom: 1.5rem;
}

.login-brand h1 {
  margin: 0;
  font-size: 1.75rem;
  color: #fff;
}

.tagline {
  margin: 0.5rem 0 2rem;
  font-size: 1rem;
  color: rgba(232, 240, 235, 0.8);
}

.features {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 0.9rem;
  color: rgba(232, 240, 235, 0.65);
}

.features li {
  padding: 0.4rem 0;
  padding-left: 1.1rem;
  position: relative;
}

.features li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.75rem;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--cg-accent);
}

.login-card {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  justify-content: center;
  max-width: 400px;
  width: 100%;
  margin: auto;
  padding: 2.5rem;
  border: none;
  box-shadow: none;
  background: transparent;
}

.login-card h2 {
  margin: 0;
  font-size: 1.5rem;
  color: var(--cg-green-900);
}

.sub {
  margin: 0 0 1rem;
}

.login-submit {
  margin-top: 0.5rem;
  width: 100%;
}

.err {
  margin: 0;
  font-size: 0.875rem;
}
</style>
