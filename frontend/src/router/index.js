import { createRouter, createWebHistory } from 'vue-router'
import { useSellerStore } from '@/stores/seller'

const routes = [
  // 买家端
  { path: '/', name: 'Home', component: () => import('@/views/buyer/Home.vue') },
  { path: '/buy', name: 'BuyForm', component: () => import('@/views/buyer/BuyForm.vue') },
  { path: '/done/:code', name: 'Done', component: () => import('@/views/buyer/Done.vue') },
  { path: '/query', name: 'Query', component: () => import('@/views/buyer/Query.vue') },

  // 卖家端
  { path: '/seller/login', name: 'SellerLogin', component: () => import('@/views/seller/Login.vue') },
  {
    path: '/seller/admin',
    name: 'SellerAdmin',
    component: () => import('@/views/seller/Admin.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/seller/publish',
    name: 'SellerPublish',
    component: () => import('@/views/seller/Publish.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/seller/history',
    name: 'SellerHistory',
    component: () => import('@/views/seller/History.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫：未登录访问卖家后台跳转到登录页
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useSellerStore().loggedIn) {
    return '/seller/login'
  }
})

export default router
