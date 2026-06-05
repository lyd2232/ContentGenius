<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchMe } from '../api'
import { clearToken } from '../api/http'

const router = useRouter()
const route = useRoute()
const user = ref(null)

const nav = [
  { to: '/', label: '工作台' },
  { to: '/projects', label: '我的项目' },
  { to: '/create', label: 'AI 创作' },
  { to: '/materials', label: '素材库' },
  { to: '/member', label: '会员中心' },
  { to: '/settings', label: '设置' }
]

const quotaLabel = computed(() => {
  const level = user.value?.memberLevel ?? 0
  if (level >= 2) return '今日额度：充足（VIP）'
  if (level === 1) return '今日额度：3 次 / 日'
  return '今日额度：1 次 / 日'
})

const memberTag = computed(() => {
  const level = user.value?.memberLevel ?? 0
  if (level >= 2) return { text: 'VIP', accent: true }
  if (level === 1) return { text: '标准', accent: false }
  return { text: '免费', accent: false }
})

onMounted(async () => {
  try {
    user.value = await fetchMe()
  } catch {
    clearToken()
    router.push('/login')
  }
})

function logout() {
  clearToken()
  router.push('/login')
}

function isActive(item) {
  if (item.to === '/') return route.path === '/'
  return route.path === item.to || route.path.startsWith(item.to + '/')
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark" aria-hidden="true" />
        <div>
          <div class="brand-name">ContentGenius</div>
          <div class="brand-sub">AI 内容工作台</div>
        </div>
      </div>
      <nav class="nav">
        <RouterLink
          v-for="item in nav"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: isActive(item) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="quota">{{ quotaLabel }}</span>
        <div class="user-meta">
          <span class="sketch-tag" :class="{ 'sketch-tag--vip': memberTag.accent }">
            {{ memberTag.text }}
          </span>
          <span class="username">{{ user?.username || '…' }}</span>
          <button class="sketch-btn secondary" type="button" @click="logout">退出</button>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--cg-green-950);
  color: #e8f0eb;
  display: flex;
  flex-direction: column;
  padding: 1.25rem 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0 1.25rem 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 0.75rem;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  flex-shrink: 0;
  background: linear-gradient(145deg, #1b4332 0%, #0d2818 100%);
  box-shadow: 0 0 0 2px rgba(201, 162, 39, 0.45);
}

.brand-name {
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #fff;
}

.brand-sub {
  font-size: 0.7rem;
  color: rgba(232, 240, 235, 0.55);
  margin-top: 0.1rem;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding: 0 0.75rem;
}

.nav-item {
  display: block;
  padding: 0.55rem 0.75rem;
  text-decoration: none;
  color: rgba(232, 240, 235, 0.75);
  border-radius: var(--cg-radius-sm);
  font-size: 0.9375rem;
  font-weight: 500;
  border-left: 3px solid transparent;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-item.active {
  background: rgba(64, 145, 108, 0.25);
  color: #fff;
  border-left-color: var(--cg-green-600);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--cg-page);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0.875rem 1.5rem;
  background: var(--cg-surface);
  border-bottom: 1px solid var(--cg-border);
}

.quota {
  font-size: 0.875rem;
  color: var(--cg-text-muted);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.username {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--cg-gray-700);
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 1.5rem;
  max-width: 1280px;
  width: 100%;
  box-sizing: border-box;
}
</style>
