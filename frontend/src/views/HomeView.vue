<template>
  <main>
    <h1>Sip Spot</h1>
    <button @click="healthStore.check()" :disabled="healthStore.loading">
      {{ healthStore.loading ? '확인 중...' : 'Health 체크' }}
    </button>
    <p v-if="healthStore.status">서버 상태: <strong>{{ healthStore.status }}</strong></p>
    <p v-if="healthStore.error" style="color: red;">오류: {{ healthStore.error }}</p>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useHealthStore } from '../stores/health.js'

const healthStore = useHealthStore()
onMounted(() => healthStore.check())
</script>
