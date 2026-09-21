import { defineStore } from 'pinia'
import { ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const useHealthStore = defineStore('health', () => {
  const status = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function check() {
    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API_BASE}/api/health`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      status.value = data.status
    } catch (e) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  return { status, loading, error, check }
})
