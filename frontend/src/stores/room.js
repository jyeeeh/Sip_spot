import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from './auth.js'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const WS_URL = API_BASE.replace(/^https/, 'wss').replace(/^http/, 'ws') + '/ws'

export const useRoomStore = defineStore('room', () => {
  // room: { code, hostNickname, location, online, viewerCount } | null
  const room = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const connected = ref(false)
  const reconnecting = ref(false)

  let stompClient = null
  let currentCode = null

  // ── REST API ───────────────────────────────────────────────────────────────

  async function createRoom() {
    const authStore = useAuthStore()
    const token = authStore.loadToken()
    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API_BASE}/api/rooms`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail ?? `오류 ${res.status}`)
      }
      const data = await res.json()
      // authStore.account.room 갱신
      if (authStore.account) {
        authStore.account.room = { code: data.code, location: 'LIVING_ROOM', online: false }
      }
      return data
    } finally {
      loading.value = false
    }
  }

  async function fetchRoom(code) {
    loading.value = true
    error.value = null
    const upperCode = code.toUpperCase()
    try {
      const res = await fetch(`${API_BASE}/api/rooms/${upperCode}`)
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail ?? `오류 ${res.status}`)
      }
      room.value = await res.json()
      return room.value
    } catch (e) {
      error.value = e.message
      return null
    } finally {
      loading.value = false
    }
  }

  // ── WebSocket / STOMP ──────────────────────────────────────────────────────

  function connectStomp(code, token) {
    const upperCode = code.toUpperCase()
    currentCode = upperCode

    const connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}

    stompClient = new Client({
      brokerURL: WS_URL,
      connectHeaders,
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        reconnecting.value = false
        // 구독 먼저 → GET 스냅샷 (이벤트 유실 방지)
        stompClient.subscribe(`/topic/rooms/${upperCode}`, (msg) => {
          handleWsEvent(JSON.parse(msg.body))
        })
        fetchRoom(upperCode)
      },
      onStompError: () => {
        connected.value = false
        reconnecting.value = true
      },
      onWebSocketError: () => {
        connected.value = false
        reconnecting.value = true
      },
      onDisconnect: () => {
        connected.value = false
      },
    })
    stompClient.activate()
  }

  function disconnectStomp() {
    stompClient?.deactivate()
    connected.value = false
    reconnecting.value = false
    currentCode = null
    room.value = null
  }

  function sendLocation(location) {
    if (!stompClient?.connected || !currentCode) return
    stompClient.publish({
      destination: `/app/rooms/${currentCode}/location`,
      body: JSON.stringify({ location }),
    })
  }

  // ── 이벤트 처리 ────────────────────────────────────────────────────────────

  function handleWsEvent(event) {
    if (!room.value) return

    if (event.type === 'LOCATION_CHANGED') {
      room.value.location = event.location
    } else if (event.type === 'PRESENCE_CHANGED') {
      room.value.online = event.online
    } else if (event.type === 'VIEWER_COUNT_CHANGED') {
      room.value.viewerCount = event.count
    }
  }

  return {
    room, loading, error, connected, reconnecting,
    createRoom, fetchRoom,
    connectStomp, disconnectStomp, sendLocation,
  }
})
