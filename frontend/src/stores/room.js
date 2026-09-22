import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const WS_URL = API_BASE.replace(/^https/, 'wss').replace(/^http/, 'ws') + '/ws'

const TOKEN_KEY = (code) => `sipspot_token_${code.toUpperCase()}`
const MEMBER_ID_KEY = (code) => `sipspot_member_id_${code.toUpperCase()}`

export const useRoomStore = defineStore('room', () => {
  const room = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const connected = ref(false)
  const reconnecting = ref(false)

  let stompClient = null
  let currentCode = null

  // ── localStorage ───────────────────────────────────────────────────────────

  function saveToken(code, token) {
    localStorage.setItem(TOKEN_KEY(code), token)
  }

  function loadToken(code) {
    return localStorage.getItem(TOKEN_KEY(code))
  }

  function saveMemberId(code, memberId) {
    localStorage.setItem(MEMBER_ID_KEY(code), memberId)
  }

  function loadMemberId(code) {
    return localStorage.getItem(MEMBER_ID_KEY(code))
  }

  // ── REST API ───────────────────────────────────────────────────────────────

  async function createRoom(nickname) {
    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API_BASE}/api/rooms`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nickname }),
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail ?? `오류 ${res.status}`)
      }
      const data = await res.json()
      saveToken(data.code, data.token)
      saveMemberId(data.code, data.memberId)
      return data
    } finally {
      loading.value = false
    }
  }

  async function joinRoom(code, nickname) {
    loading.value = true
    error.value = null
    try {
      const upperCode = code.toUpperCase()
      const res = await fetch(`${API_BASE}/api/rooms/${upperCode}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nickname }),
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail ?? `오류 ${res.status}`)
      }
      const data = await res.json()
      saveToken(upperCode, data.token)
      saveMemberId(upperCode, data.memberId)
      return { ...data, code: upperCode }
    } finally {
      loading.value = false
    }
  }

  async function fetchRoom(code) {
    loading.value = true
    error.value = null
    const upperCode = code.toUpperCase()
    const token = loadToken(upperCode)
    if (!token) {
      error.value = '입장 정보가 없습니다. 다시 입장해주세요.'
      loading.value = false
      return null
    }
    try {
      const res = await fetch(`${API_BASE}/api/rooms/${upperCode}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
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

  function connectStomp(code) {
    const upperCode = code.toUpperCase()
    const token = loadToken(upperCode)
    if (!token) return

    currentCode = upperCode
    stompClient = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      debug: (msg) => {
        if (msg.startsWith('>>> CONNECT')) {
          console.debug('[STOMP] CONNECT 프레임 전송 —',
            msg.includes('Authorization') ? 'Authorization 헤더 있음' : 'Authorization 헤더 없음')
        }
      },
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
      const member = room.value.members.find((m) => m.id === event.memberId)
      if (member) member.location = event.location
    } else if (event.type === 'PRESENCE_CHANGED') {
      const member = room.value.members.find((m) => m.id === event.memberId)
      if (member) member.online = event.online
    } else if (event.type === 'MEMBER_JOINED') {
      const exists = room.value.members.some((m) => m.id === event.memberId)
      if (!exists) {
        room.value.members.push({
          id: event.memberId,
          nickname: event.nickname,
          location: event.location,
          online: event.online,
          host: false,
        })
      }
    }
  }

  return {
    room, loading, error, connected, reconnecting,
    createRoom, joinRoom, fetchRoom,
    connectStomp, disconnectStomp, sendLocation,
    loadToken, loadMemberId,
  }
})
