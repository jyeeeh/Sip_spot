import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RoomView from '../views/RoomView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/room/:code', component: RoomView },
  ],
})

export default router
