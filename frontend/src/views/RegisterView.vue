<script setup>
import { onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, sendRegisterSms } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const phone = ref('')
const smsCode = ref('')
const loading = ref(false)
const sendingSms = ref(false)
const smsCooldown = ref(0)
const error = ref('')
const success = ref('')
const smsHint = ref('')

let cooldownTimer = null

function startCooldown(seconds = 60) {
  smsCooldown.value = seconds
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    smsCooldown.value -= 1
    if (smsCooldown.value <= 0) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})

async function sendSms() {
  error.value = ''
  smsHint.value = ''
  const p = phone.value.trim()
  if (!/^1[3-9]\d{9}$/.test(p)) {
    error.value = '请先填写正确的手机号'
    return
  }
  if (smsCooldown.value > 0) return
  sendingSms.value = true
  try {
    const data = await sendRegisterSms(p)
    startCooldown(60)
    smsHint.value = '验证码已发送，请查收短信'
  } catch (e) {
    error.value = e.message
  } finally {
    sendingSms.value = false
  }
}

async function submit() {
  error.value = ''
  success.value = ''
  smsHint.value = ''
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (!smsCode.value.trim()) {
    error.value = '请填写短信验证码'
    return
  }
  loading.value = true
  try {
    await register({
      username: username.value.trim(),
      password: password.value,
      phone: phone.value.trim(),
      smsCode: smsCode.value.trim(),
      email: email.value.trim() || undefined
    })
    success.value = '注册成功，请登录'
    setTimeout(() => router.push('/login'), 800)
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
      <img src="/logo.png" alt="" class="brand-logo brand-logo--lg" width="52" height="52" />
      <h1>ContentGenius</h1>
      <p class="tagline">多平台 AI 写稿，从主题到定稿</p>
      <ul class="features">
        <li>LangChain4j Agent 流式创作</li>
        <li>项目版本与 RAG 知识库</li>
        <li>图片风格解析（Vision）</li>
      </ul>
    </aside>
    <form class="login-card sketch-box" @submit.prevent="submit">
      <h2>注册</h2>
      <p class="sub text-muted">创建账号后进入创作工作台</p>
      <label class="sketch-label">用户名</label>
      <input v-model="username" class="sketch-input" required autocomplete="username" />
      <label class="sketch-label">密码</label>
      <input
        v-model="password"
        type="password"
        class="sketch-input"
        required
        minlength="6"
        autocomplete="new-password"
      />
      <label class="sketch-label">确认密码</label>
      <input
        v-model="confirmPassword"
        type="password"
        class="sketch-input"
        required
        minlength="6"
        autocomplete="new-password"
      />
      <label class="sketch-label">手机号</label>
      <input
        v-model="phone"
        type="tel"
        class="sketch-input"
        required
        maxlength="11"
        pattern="1[3-9]\d{9}"
        autocomplete="tel"
        placeholder="11 位中国大陆手机号"
      />
      <label class="sketch-label">短信验证码</label>
      <div class="sms-row">
        <input
          v-model="smsCode"
          class="sketch-input sms-input"
          required
          maxlength="6"
          inputmode="numeric"
          autocomplete="one-time-code"
          placeholder="6 位验证码"
        />
        <button
          class="sketch-btn sms-btn"
          type="button"
          :disabled="sendingSms || smsCooldown > 0"
          @click="sendSms"
        >
          {{ sendingSms ? '发送中…' : smsCooldown > 0 ? `${smsCooldown}s` : '获取验证码' }}
        </button>
      </div>
      <p v-if="smsHint" class="sms-hint text-muted">{{ smsHint }}</p>
      <label class="sketch-label">邮箱（选填）</label>
      <input v-model="email" type="email" class="sketch-input" autocomplete="email" />
      <p v-if="error" class="msg text-error">{{ error }}</p>
      <p v-if="success" class="msg text-success">{{ success }}</p>
      <button class="sketch-btn login-submit" type="submit" :disabled="loading">
        {{ loading ? '注册中…' : '创建账号' }}
      </button>
      <p class="switch-link">
        已有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
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

.msg {
  margin: 0;
  font-size: 0.875rem;
}

.switch-link {
  margin: 0.75rem 0 0;
  text-align: center;
  font-size: 0.875rem;
  color: var(--cg-gray-600);
}

.switch-link a {
  color: var(--cg-green-800);
  font-weight: 600;
  text-decoration: none;
}

.switch-link a:hover {
  text-decoration: underline;
}

.sms-row {
  display: flex;
  gap: 0.5rem;
  align-items: stretch;
}

.sms-input {
  flex: 1;
  min-width: 0;
}

.sms-btn {
  flex-shrink: 0;
  white-space: nowrap;
  padding-left: 0.85rem;
  padding-right: 0.85rem;
}

.sms-hint {
  margin: -0.25rem 0 0.5rem;
  font-size: 0.8125rem;
}
</style>
