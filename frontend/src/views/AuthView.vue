<template>
  <main class="auth-page">
    <h1>Sip Spot</h1>

    <div class="tabs">
      <button :class="{ 'tab--active': mode === 'login' }" @click="mode = 'login'">로그인</button>
      <button :class="{ 'tab--active': mode === 'signup' }" @click="mode = 'signup'">회원가입</button>
    </div>

    <!-- 로그인 -->
    <form v-if="mode === 'login'" @submit.prevent="handleLogin">
      <input v-model="username" type="text" placeholder="아이디" autocomplete="username" required />
      <input v-model="password" type="password" placeholder="비밀번호" autocomplete="current-password" required />
      <button type="submit" :disabled="authStore.loading">로그인</button>
    </form>

    <!-- 회원가입 -->
    <form v-else @submit.prevent="handleSignup">
      <input v-model="username" type="text" placeholder="아이디 (3~30자)" minlength="3" maxlength="30" required />
      <input v-model="password" type="password" placeholder="비밀번호 (8자 이상)" minlength="8" maxlength="72" autocomplete="new-password" required />
      <input v-model="nickname" type="text" placeholder="닉네임 (1~12자)" minlength="1" maxlength="12" required />
      <button type="submit" :disabled="authStore.loading">가입</button>
    </form>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const authStore = useAuthStore()

const mode = ref('login')
const username = ref('')
const password = ref('')
const nickname = ref('')
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  try {
    await authStore.login(username.value.trim(), password.value)
    router.push('/')
  } catch (e) {
    errorMsg.value = e.message
  }
}

async function handleSignup() {
  errorMsg.value = ''
  try {
    await authStore.signup(username.value.trim(), password.value, nickname.value.trim())
    router.push('/')
  } catch (e) {
    errorMsg.value = e.message
  }
}
</script>

<style scoped>
.auth-page { max-width: 400px; margin: 60px auto; padding: 0 16px; }
.tabs { display: flex; gap: 8px; margin-bottom: 24px; }
.tabs button { flex: 1; padding: 8px; font-size: 1rem; cursor: pointer; border: 1px solid #dee2e6; background: #f8f9fa; border-radius: 4px; }
.tab--active { background: #0d6efd; color: #fff; border-color: #0d6efd; }
form { display: flex; flex-direction: column; gap: 10px; }
input { padding: 8px; font-size: 1rem; border: 1px solid #dee2e6; border-radius: 4px; }
button[type="submit"] { padding: 10px; font-size: 1rem; cursor: pointer; background: #0d6efd; color: #fff; border: none; border-radius: 4px; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.error { color: red; margin-top: 8px; }
</style>
