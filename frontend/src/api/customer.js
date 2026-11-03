import http from '@/utils/http'

/** 客户注册（成功后即登录） */
export function register(data) {
  return http.post('/customer/register', data)
}

/** 客户登录 */
export function login(data) {
  return http.post('/customer/login', data)
}

/** 当前登录客户 */
export function getMe() {
  return http.get('/customer/me')
}

/** 我的订单（分页） */
export function getOrders(params) {
  return http.get('/customer/orders', { params })
}

/** 退出登录 */
export function logout() {
  return http.post('/customer/logout')
}