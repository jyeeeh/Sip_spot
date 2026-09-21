<template>
  <main class="home">
    <h1>Sip Spot</h1>

    <section>
      <h2>방 만들기</h2>
      <form @submit.prevent="handleCreate">
        <input v-model="createNickname" placeholder="닉네임 (1~12자)" maxlength="12" required />
        <button type="submit" :disabled="roomStore.loading">만들기</button>
      </form>
    </section>

    <section>
      <h2>코드로 입장</h2>
      <form @submit.prevent="handleJoin">
        <input v-model="joinCode" placeholder="방 코드 7자리" maxlength="7" required />
        <input v-model="joinNickname" placeholder="닉네임 (1~12자)" maxlength="12" required />
        <button type="submit" :disabled="roomStore.loading">입장</button>
      </form>
    </section>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomStore } from '../stores/room.js'

const router = useRouter()
const roomStore = useRoomStore()

const createNickname = ref('')
const joinCode = ref('')
const joinNickname = ref('')
const errorMsg = ref('')

async function handleCreate() {
  errorMsg.value = ''
  try {
    const data = await roomStore.createRoom(createNickname.value.trim())
    router.push(`/room/${data.code}`)
  } catch (e) {
    errorMsg.value = e.message
  }
}

async function handleJoin() {
  errorMsg.value = ''
  const code = joinCode.value.trim().toUpperCase()

  // 이미 토큰이 있으면 바로 이동
  const existing = roomStore.loadToken(code)
  if (existing) {
    router.push(`/room/${code}`)
    return
  }

  try {
    const data = await roomStore.joinRoom(code, joinNickname.value.trim())
    router.push(`/room/${data.code}`)
  } catch (e) {
    errorMsg.value = e.message
  }
}
</script>

<style scoped>
.home { max-width: 480px; margin: 40px auto; padding: 0 16px; }
section { margin-bottom: 32px; }
form { display: flex; flex-direction: column; gap: 8px; }
input { padding: 8px; font-size: 1rem; }
button { padding: 8px; font-size: 1rem; cursor: pointer; }
.error { color: red; }
</style>
