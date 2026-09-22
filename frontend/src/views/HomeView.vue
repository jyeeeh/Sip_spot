<template>
  <main class="home">
    <h1>Sip Spot</h1>

    <!-- 로그인 상태 -->
    <template v-if="authStore.account">
      <div class="account-bar">
        <span>{{ authStore.account.nickname }}</span>
        <button class="btn-text" @click="handleLogout">로그아웃</button>
      </div>

      <!-- 내 방 -->
      <section v-if="authStore.account.room">
        <h2>내 방</h2>
        <p class="room-code-display">{{ authStore.account.room.code }}</p>
        <button class="btn" @click="router.push(`/room/${authStore.account.room.code}`)">
          내 방으로 이동
        </button>
      </section>

      <section v-else>
        <h2>방 만들기</h2>
        <button class="btn" :disabled="roomStore.loading" @click="handleCreate">
          방 만들기
        </button>
        <p v-if="createError" class="error">{{ createError }}</p>
      </section>
    </template>

    <!-- 비로그인 -->
    <template v-else>
      <p class="desc">방 위치를 실시간으로 공유하세요.</p>
      <button class="btn" @click="router.push('/login')">로그인 / 회원가입</button>
    </template>

    <!-- 방 코드로 보기 (누구나) -->
    <section class="guest-section">
      <h2>방 코드로 보기</h2>
      <form @submit.prevent="handleView">
        <input v-model="viewCode" placeholder="방 코드 7자리" maxlength="7" required />
        <button type="submit">보기</button>
      </form>
      <p v-if="viewError" class="error">{{ viewError }}</p>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useRoomStore } from '../stores/room.js'

const router = useRouter()
const authStore = useAuthStore()
const roomStore = useRoomStore()

const viewCode = ref('')
const viewError = ref('')
const createError = ref('')

async function handleCreate() {
  createError.value = ''
  try {
    const data = await roomStore.createRoom()
    router.push(`/room/${data.code}`)
  } catch (e) {
    createError.value = e.message
  }
}

function handleView() {
  viewError.value = ''
  const code = viewCode.value.trim().toUpperCase()
  if (code.length !== 7) {
    viewError.value = '방 코드는 7자리입니다.'
    return
  }
  router.push(`/room/${code}`)
}

async function handleLogout() {
  await authStore.logout()
}
</script>

<style scoped>
.home { max-width: 480px; margin: 40px auto; padding: 0 16px; }
.account-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; background: #f8f9fa; padding: 10px 14px; border-radius: 8px; }
section { margin-bottom: 32px; }
.guest-section { border-top: 1px solid #dee2e6; padding-top: 24px; }
.room-code-display { font-size: 1.6rem; font-weight: bold; letter-spacing: 0.1em; margin: 8px 0 12px; }
.desc { color: #6c757d; margin-bottom: 16px; }
form { display: flex; gap: 8px; }
input { flex: 1; padding: 8px; font-size: 1rem; border: 1px solid #dee2e6; border-radius: 4px; }
.btn { padding: 10px 20px; font-size: 1rem; cursor: pointer; background: #0d6efd; color: #fff; border: none; border-radius: 4px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
button[type="submit"] { padding: 8px 14px; font-size: 1rem; cursor: pointer; border: 1px solid #dee2e6; border-radius: 4px; background: #fff; }
.btn-text { background: none; border: none; color: #6c757d; cursor: pointer; font-size: 0.9rem; }
.error { color: red; margin-top: 8px; }
</style>
