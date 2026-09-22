import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RoomView from '../views/RoomView.vue'
import AuthView from '../views/AuthView.vue'
import { useAuthStore } from '../stores/auth.js'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/login', component: AuthView },
    { path: '/room/:code', component: RoomView },
  ],
})

// 이미 로그인한 상태에서 /login에 접근하면 홈으로 리다이렉트
router.beforeEach((to) => {
  if (to.path === '/login') {
    const authStore = useAuthStore()
    if (authStore.account) return '/'
  }
})

export default router
