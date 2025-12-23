import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 路由规则数组
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'FaceLogin',
    component: () => import('@/views/FaceLogin.vue')
  }
]

// 创建 router 实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
