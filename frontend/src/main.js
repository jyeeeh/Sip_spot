import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
import { useAuthStore } from './stores/auth.js'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// 앱 시작 시 localStorage 토큰으로 세션 복구
useAuthStore().restoreSession()

app.mount('#app')
