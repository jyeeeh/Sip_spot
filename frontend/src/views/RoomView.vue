<template>
  <main class="room-page">

    <!-- 연결 상태 뱃지 -->
    <div class="status-badge" :class="statusClass">
      {{ statusLabel }}
    </div>

    <template v-if="roomStore.loading && !roomStore.room">
      <p class="center">불러오는 중...</p>
    </template>

    <template v-else-if="roomStore.error && !roomStore.room">
      <p class="center error">{{ roomStore.error }}</p>
      <button class="btn" @click="router.push('/')">홈으로</button>
    </template>

    <template v-else-if="roomStore.room">
      <h2 class="room-code">방 코드: {{ roomStore.room.code }}</h2>

      <!-- 4구역 그리드 -->
      <div class="zones">
        <div
          v-for="zone in ZONES"
          :key="zone.key"
          class="zone"
          :class="{ 'zone--mine': isMyZone(zone.key) }"
          @click="moveToZone(zone.key)"
        >
          <div class="zone-label">{{ zone.label }}</div>
          <div class="avatars">
            <div
              v-for="m in membersInZone(zone.key)"
              :key="m.id"
              class="avatar"
              :class="{ 'avatar--offline': !m.online, 'avatar--me': isMe(m.id) }"
              :title="m.nickname"
            >
              {{ m.nickname.charAt(0).toUpperCase() }}
            </div>
          </div>
        </div>
      </div>
    </template>

  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRoomStore } from '../stores/room.js'

const route = useRoute()
const router = useRouter()
const roomStore = useRoomStore()

const ZONES = [
  { key: 'KITCHEN',     label: '부엌' },
  { key: 'LIVING_ROOM', label: '거실' },
  { key: 'BED',         label: '침대' },
  { key: 'OUTSIDE',     label: '밖' },
]

const code = route.params.code.toUpperCase()
const myMemberId = roomStore.loadMemberId(code)

const statusClass = computed(() => ({
  'badge--connected':    roomStore.connected,
  'badge--reconnecting': roomStore.reconnecting,
  'badge--disconnected': !roomStore.connected && !roomStore.reconnecting,
}))

const statusLabel = computed(() => {
  if (roomStore.connected) return '연결됨'
  if (roomStore.reconnecting) return '재연결 중...'
  return '연결 끊김'
})

function membersInZone(zoneKey) {
  return roomStore.room?.members.filter((m) => m.location === zoneKey) ?? []
}

function isMyZone(zoneKey) {
  if (!myMemberId || !roomStore.room) return false
  const me = roomStore.room.members.find((m) => m.id === myMemberId)
  return me?.location === zoneKey
}

function isMe(memberId) {
  return memberId === myMemberId
}

function moveToZone(zoneKey) {
  roomStore.sendLocation(zoneKey)
}

onMounted(() => {
  if (!roomStore.loadToken(code)) {
    router.replace('/')
    return
  }
  // connectStomp 내부에서 구독 → GET 스냅샷 순서 보장
  roomStore.connectStomp(code)
})

onUnmounted(() => {
  roomStore.disconnectStomp()
})
</script>

<style scoped>
.room-page { max-width: 600px; margin: 24px auto; padding: 0 16px; }

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  margin-bottom: 12px;
}
.badge--connected    { background: #d4edda; color: #155724; }
.badge--reconnecting { background: #fff3cd; color: #856404; }
.badge--disconnected { background: #f8d7da; color: #721c24; }

.room-code { margin-bottom: 16px; }

.zones {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.zone {
  border: 2px solid #dee2e6;
  border-radius: 12px;
  padding: 12px;
  min-height: 120px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.zone:hover         { border-color: #6c757d; }
.zone--mine         { border-color: #0d6efd; background: #f0f4ff; }

.zone-label {
  font-weight: bold;
  font-size: 0.9rem;
  margin-bottom: 8px;
  color: #495057;
}

.avatars { display: flex; flex-wrap: wrap; gap: 6px; }

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #0d6efd;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.9rem;
  transition: all 0.3s;
}
.avatar--offline { background: #adb5bd; }
.avatar--me      { outline: 2px solid #fd7e14; outline-offset: 2px; }

.center { text-align: center; margin-top: 40px; }
.error  { color: red; }
.btn    { display: block; margin: 16px auto; padding: 8px 16px; cursor: pointer; }
</style>
