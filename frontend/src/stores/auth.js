import { defineStore } from 'pinia'
import { ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const TOKEN_KEY = 'sipspot_account_token'

export const useAuthStore = defineStore('auth', () => {
  // account: { accountId, nickname, token, room } | null
  const account = ref(null)
  const loading = ref(false)

  function saveToken(token) {
    localStorage.setItem(TOKEN_KEY, token)
  }

  function loadToken() {
    return localStorage.getItem(TOKEN_KEY)
  }

  function clearToken() {
    localStorage.removeItem(TOKEN_KEY)
  }

  async function signup(username, password, nickname) {
    loading.value = true
    try {
      const res = await fetch(`${API_BASE}/api/auth/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, nickname }),
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail ?? `오류 ${res.status}`)
      }
      const data = await res.json()
      saveToken(data.token)
      account.value = { accountId: data.accountId, nickname: data.nickname, token: data.token, room: null }
      return data
    } finally {
      loading.value = false
    }
  }

  async function login(username, password) {
    loading.value = true
    try {
      const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail ?? `오류 ${res.status}`)
      }
      const data = await res.json()
      saveToken(data.token)
      account.value = { accountId: data.accountId, nickname: data.nickname, token: data.token, room: data.room }
      return data
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    const token = loadToken()
    if (token) {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {})
    }
    clearToken()
    account.value = null
  }

  // 앱 시작 시 호출 — localStorage 토큰으로 세션 복구
  async function restoreSession() {
    const token = loadToken()
    if (!token) return
    try {
      const res = await fetch(`${API_BASE}/api/me`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) {
        clearToken()
        return
      }
      const data = await res.json()
      account.value = { accountId: data.accountId, nickname: data.nickname, token, room: data.room }
    } catch {
      // 네트워크 오류 시 무시
    }
  }

  return { account, loading, signup, login, logout, restoreSession, loadToken }
})
