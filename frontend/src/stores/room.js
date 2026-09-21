import { defineStore } from 'pinia'
import { ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const TOKEN_KEY = (code) => `sipspot_token_${code.toUpperCase()}`

export const useRoomStore = defineStore('room', () => {
  const room = ref(null)
  const loading = ref(false)
  const error = ref(null)

  function saveToken(code, token) {
    localStorage.setItem(TOKEN_KEY(code), token)
  }

  function loadToken(code) {
    return localStorage.getItem(TOKEN_KEY(code))
  }

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

  return { room, loading, error, createRoom, joinRoom, fetchRoom, loadToken }
})
