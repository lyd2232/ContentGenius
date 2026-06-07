import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/http'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
      { path: 'projects', name: 'projects', component: () => import('../views/ProjectsView.vue') },
      { path: 'projects/:id', name: 'project-detail', component: () => import('../views/ProjectDetailView.vue') },
      { path: 'create', name: 'create', component: () => import('../views/CreateView.vue') },
      { path: 'materials', name: 'materials', component: () => import('../views/MaterialsView.vue') },
      { path: 'member', name: 'member', component: () => import('../views/MemberView.vue') },
      { path: 'settings', name: 'settings', component: () => import('../views/SettingsView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.path === '/login' || to.path === '/register') return true
  if (!getToken()) return '/login'
  return true
})

export default router
