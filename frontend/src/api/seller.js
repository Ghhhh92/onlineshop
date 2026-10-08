import http from '@/utils/http'

/** 卖家登录 */
export function login(data) {
  return http.post('/seller/login', data)
}

/** 获取当前卖家信息 */
export function getMe() {
  return http.get('/seller/me')
}

/** 退出登录 */
export function logout() {
  return http.post('/seller/logout')
}
