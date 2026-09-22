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

      <!-- 커피 패널 -->
      <section class="coffee-panel">
        <h3>☕ 커피</h3>

        <!-- 게스트 전송 폼 -->
        <form v-if="!isHost" class="coffee-form" @submit.prevent="handleSendCoffee">
          <input
            v-model="coffeeMessage"
            type="text"
            placeholder="응원 메시지 (선택)"
            maxlength="200"
          />
          <button type="submit" :disabled="coffeeSending">보내기</button>
        </form>
        <p v-if="coffeeError" class="error">{{ coffeeError }}</p>
        <p v-if="coffeeSent" class="coffee-sent">커피를 보냈습니다 ☕</p>

        <!-- 커피 목록 -->
        <div class="coffee-body">
          <ul class="coffee-list">
            <li v-for="coffee in pagedCoffees" :key="coffee.id" class="coffee-item">
              <span class="coffee-msg">{{ coffee.message || '커피를 후원했습니다.' }}</span>
              <button v-if="isHost" class="btn-delete" @click="requestDelete(coffee.id)">✕</button>
            </li>
            <li v-if="roomStore.coffees.length === 0" class="coffee-empty">아직 커피가 없습니다.</li>
          </ul>
        </div>

        <!-- 페이지네이션 (6개 이상일 때만 표시) -->
        <div v-if="totalPages > 1" class="coffee-pagination">
          <button
            v-for="page in totalPages"
            :key="page"
            class="page-btn"
            :class="{ 'page-btn--active': coffeePage === page }"
            @click="coffeePage = page"
          >{{ page }}</button>
        </div>
      </section>
    </template>

  </main>

  <!-- 삭제 확인 모달 -->
  <div v-if="deleteConfirmId !== null" class="modal-overlay" @click.self="deleteConfirmId = null">
    <div class="modal">
      <p class="modal-text">정말 삭제하시겠습니까?</p>
      <div class="modal-buttons">
        <button class="btn-modal-confirm" @click="confirmDelete">확인</button>
        <button class="btn-modal-cancel" @click="deleteConfirmId = null">취소</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
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

// ── 커피 ──────────────────────────────────────────────────────────────────

const PAGE_SIZE = 5
const coffeePage = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(roomStore.coffees.length / PAGE_SIZE)))
const pagedCoffees = computed(() =>
  roomStore.coffees.slice((coffeePage.value - 1) * PAGE_SIZE, coffeePage.value * PAGE_SIZE)
)
// 항목 삭제 등으로 totalPages가 줄면 현재 페이지를 마지막 페이지로 조정
watch(totalPages, (pages) => {
  if (coffeePage.value > pages) coffeePage.value = pages
})

const coffeeMessage = ref('')
const coffeeSending = ref(false)
const coffeeError = ref('')
const coffeeSent = ref(false)
const deleteConfirmId = ref(null)

async function handleSendCoffee() {
  coffeeError.value = ''
  coffeeSent.value = false
  coffeeSending.value = true
  try {
    await roomStore.sendCoffee(code, coffeeMessage.value.trim() || null)
    coffeeMessage.value = ''
    coffeeSent.value = true
    setTimeout(() => { coffeeSent.value = false }, 3000)
  } catch (e) {
    coffeeError.value = e.message
  } finally {
    coffeeSending.value = false
  }
}

function requestDelete(id) {
  deleteConfirmId.value = id
}

async function confirmDelete() {
  const id = deleteConfirmId.value
  deleteConfirmId.value = null
  try {
    await roomStore.deleteCoffee(code, id)
  } catch (e) {
    // 이미 WS로 반영되므로 무시
  }
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

/* ── 커피 패널 ─────────────────────────────────────────────────────────── */
.coffee-panel { margin-top: 24px; border-top: 1px solid #dee2e6; padding-top: 16px; }
.coffee-panel h3 { margin-bottom: 12px; font-size: 1rem; }
.coffee-form { display: flex; gap: 8px; margin-bottom: 8px; }
.coffee-form input { flex: 1; padding: 6px 8px; font-size: 0.9rem; border: 1px solid #dee2e6; border-radius: 4px; }
.coffee-form button { padding: 6px 12px; font-size: 0.9rem; cursor: pointer; background: #0d6efd; color: #fff; border: none; border-radius: 4px; }
.coffee-form button:disabled { opacity: 0.6; cursor: not-allowed; }
.coffee-sent { color: #28a745; font-size: 0.85rem; margin-bottom: 8px; }
.coffee-body { min-height: 252px; display: flex; flex-direction: column; }
.coffee-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.coffee-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; background: #f8f9fa; border-radius: 6px; font-size: 0.9rem; }
.coffee-msg { flex: 1; word-break: break-word; color: #343a40; }
.coffee-empty { color: #adb5bd; font-size: 0.85rem; padding: 8px 0; }
.btn-delete { background: none; border: none; color: #adb5bd; cursor: pointer; font-size: 0.85rem; padding: 0 4px; flex-shrink: 0; }
.btn-delete:hover { color: #dc3545; }
.coffee-pagination { display: flex; gap: 4px; margin-top: 10px; flex-wrap: wrap; }
.page-btn { padding: 4px 10px; font-size: 0.85rem; border: 1px solid #dee2e6; border-radius: 4px; background: #fff; cursor: pointer; color: #495057; }
.page-btn:hover { border-color: #0d6efd; color: #0d6efd; }
.page-btn--active { background: #0d6efd; color: #fff; border-color: #0d6efd; }

/* ── 삭제 확인 모달 ────────────────────────────────────────────────────── */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; border-radius: 8px; padding: 24px 28px; min-width: 240px; text-align: center; box-shadow: 0 4px 24px rgba(0,0,0,0.15); }
.modal-text { margin-bottom: 18px; font-size: 1rem; color: #343a40; }
.modal-buttons { display: flex; gap: 10px; justify-content: center; }
.btn-modal-confirm { padding: 8px 20px; background: #dc3545; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
.btn-modal-cancel  { padding: 8px 20px; background: #f8f9fa; color: #343a40; border: 1px solid #dee2e6; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
</style>
