<template>
  <main class="room">
    <template v-if="roomStore.loading">
      <p>불러오는 중...</p>
    </template>

    <template v-else-if="roomStore.error">
      <p class="error">{{ roomStore.error }}</p>
      <button @click="router.push('/')">홈으로</button>
    </template>

    <template v-else-if="roomStore.room">
      <h1>방 코드: {{ roomStore.room.code }}</h1>
      <p>정원: {{ roomStore.room.members.length }} / {{ roomStore.room.maxMembers }}</p>

      <h2>멤버 목록</h2>
      <ul>
        <li v-for="m in roomStore.room.members" :key="m.id">
          {{ m.nickname }}{{ m.host ? ' (방장)' : '' }}
        </li>
      </ul>

      <button @click="refresh">새로고침</button>
    </template>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRoomStore } from '../stores/room.js'

const route = useRoute()
const router = useRouter()
const roomStore = useRoomStore()

async function refresh() {
  await roomStore.fetchRoom(route.params.code)
}

onMounted(async () => {
  const code = route.params.code.toUpperCase()
  const token = roomStore.loadToken(code)
  if (!token) {
    router.replace('/')
    return
  }
  await roomStore.fetchRoom(code)
})
</script>

<style scoped>
.room { max-width: 480px; margin: 40px auto; padding: 0 16px; }
ul { list-style: none; padding: 0; }
li { padding: 6px 0; border-bottom: 1px solid #eee; }
.error { color: red; }
button { margin-top: 16px; padding: 8px 16px; cursor: pointer; }
</style>
