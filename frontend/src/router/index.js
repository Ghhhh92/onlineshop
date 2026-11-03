import { createRouter, createWebHistory } from 'vue-router'
import { useSellerStore } from '@/stores/seller'
import { useCustomerStore } from '@/stores/customer'

const routes = [
  // 买家端
  { path: '/', name: 'Home', component: () => import('@/views/buyer/Home.vue') },
  { path: '/product/:id', name: 'ProductDetail', component: () => import('@/views/buyer/ProductDetail.vue') },
  { path: '/buy', name: 'BuyForm', component: () => import('@/views/buyer/BuyForm.vue') },
  { path: '/done/:code', name: 'Done', component: () => import('@/views/buyer/Done.vue') },
  { path: '/query', name: 'Query', component: () => import('@/views/buyer/Query.vue') },

  // 客户端
  { path: '/customer/login', name: 'CustomerLogin', component: () => import('@/views/customer/Login.vue') },
  { path: '/customer/register', name: 'CustomerRegister', component: () => import('@/views/customer/Register.vue') },
  {
    path: '/customer/orders',
    name: 'CustomerOrders',
    component: () => import('@/views/customer/Orders.vue'),
    meta: { requiresCustomer: true },
  },

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

// 登录守卫：分别处理客户与卖家的登录态
router.beforeEach(async (to) => {
  if (to.meta.requiresAuth || to.name === 'SellerLogin') {
    const store = useSellerStore()
    try { await store.restore() } catch {
      return to.name === 'SellerLogin' ? true : { path: '/seller/login', query: { reason: 'connection' } }
    }
    if (to.meta.requiresAuth && !store.loggedIn) return '/seller/login'
    if (to.name === 'SellerLogin' && store.loggedIn) return '/seller/admin'
  }

  if (to.meta.requiresCustomer || to.name === 'CustomerLogin' || to.name === 'CustomerRegister') {
    const store = useCustomerStore()
    try { await store.restore() } catch {
      return to.name === 'CustomerLogin' ? true : { path: '/customer/login', query: { reason: 'connection' } }
    }
    if (to.meta.requiresCustomer && !store.loggedIn) return '/customer/login'
    if ((to.name === 'CustomerLogin' || to.name === 'CustomerRegister') && store.loggedIn) return '/customer/orders'
  }
})

export default router