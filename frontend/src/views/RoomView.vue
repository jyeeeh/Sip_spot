<template>
  <main class="room-page">

    <button class="btn-home" @click="router.push('/')">← 홈으로</button>

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
      <div class="room-header">
        <div>
          <h2 class="room-code">방 코드: {{ roomStore.room.code }}</h2>
          <p class="host-info">
            호스트: <strong>{{ roomStore.room.hostNickname }}</strong>
            <span class="presence-dot" :class="roomStore.room.online ? 'dot--online' : 'dot--offline'"></span>
          </p>
        </div>
        <div class="viewer-count">{{ roomStore.room.viewerCount ?? 0 }}명이 보고 있습니다</div>
      </div>

      <p v-if="isHost" class="host-hint">구역을 클릭해서 위치를 알려주세요</p>

      <!-- 4구역 그리드 (호스트/게스트 공통) — 호스트만 클릭 활성화 -->
      <div class="zones">
        <div
          v-for="zone in ZONES"
          :key="zone.key"
          class="zone"
          :class="{
            'zone--current': roomStore.room.location === zone.key,
            'zone--clickable': isHost,
          }"
          @click="isHost && moveToZone(zone.key)"
        >
          <div class="zone-label">{{ zone.label }}</div>
          <div v-if="roomStore.room.location === zone.key" class="zone-avatar">
            {{ roomStore.room.hostNickname.charAt(0).toUpperCase() }}
          </div>
        </div>
      </div>
    </template>

  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useRoomStore } from '../stores/room.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const roomStore = useRoomStore()

const ZONES = [
  { key: 'KITCHEN',     label: '부엌' },
  { key: 'LIVING_ROOM', label: '거실' },
  { key: 'BED',         label: '침대' },
  { key: 'OUTSIDE',     label: '밖' },
]

const ZONE_LABELS = Object.fromEntries(ZONES.map(z => [z.key, z.label]))

const code = route.params.code.toUpperCase()

// 이 방의 호스트인지 여부
const isHost = computed(() => authStore.account?.room?.code === code)

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

function moveToZone(zoneKey) {
  roomStore.sendLocation(zoneKey)
}

onMounted(() => {
  // 호스트이면 인증 토큰으로 연결, 게스트이면 익명 연결
  const token = isHost.value ? authStore.loadToken() : null
  roomStore.connectStomp(code, token)
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

.room-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.room-code { margin-bottom: 4px; }
.host-info { color: #495057; font-size: 0.9rem; display: flex; align-items: center; gap: 6px; }
.presence-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot--online  { background: #28a745; }
.dot--offline { background: #adb5bd; }
.viewer-count { font-size: 0.85rem; color: #6c757d; text-align: right; }

.host-hint { color: #6c757d; font-size: 0.9rem; margin-bottom: 12px; }

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
  cursor: default;
  transition: border-color 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.zone--clickable         { cursor: pointer; }
.zone--clickable:hover   { border-color: #6c757d; }
.zone--current           { border-color: #0d6efd; background: #f0f4ff; }
.zone-label { font-weight: bold; font-size: 1rem; color: #495057; }
.zone-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #0d6efd;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1rem;
  margin-top: 10px;
}

.btn-home { background: none; border: none; color: #6c757d; cursor: pointer; font-size: 0.9rem; padding: 0; margin-bottom: 12px; }
.btn-home:hover { color: #343a40; }
.center { text-align: center; margin-top: 40px; }
.error  { color: red; }
.btn    { display: block; margin: 16px auto; padding: 8px 16px; cursor: pointer; }
</style>
