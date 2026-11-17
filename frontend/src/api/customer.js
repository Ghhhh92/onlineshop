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

/** 退出登录 */
export function logout() {
  return http.post('/customer/logout')
}

/** 收藏列表（分页，按收藏时间倒序） */
export function getFavorites(params) {
  return http.get('/customer/favorite', { params })
}

/** 收藏商品（幂等去重） */
export function addFavorite(productId) {
  return http.post('/customer/favorite', { productId })
}

/** 取消收藏 */
export function removeFavorite(productId) {
  return http.delete(`/customer/favorite/${productId}`)
}

/** 购物车列表（数组） */
export function getCart() {
  return http.get('/customer/cart')
}

/** 加入购物车，同商品累加数量 */
export function addCart(productId, quantity = 1) {
  return http.post('/customer/cart', { productId, quantity })
}

/** 修改购物车数量，传 0 等同删除 */
export function updateCart(id, quantity) {
  return http.put(`/customer/cart/${id}`, { quantity })
}

/** 移出购物车 */
export function removeCart(id) {
  return http.delete(`/customer/cart/${id}`)
}

/** 购物车多选转收藏 */
export function cartToFavorite(cartItemIds) {
  return http.post('/customer/cart/to-favorite', { cartItemIds })
}

/** 购物车多选下单 */
export function cartCheckout(cartItemIds) {
  return http.post('/customer/cart/checkout', { cartItemIds })
}

/** 直接下单（不经购物车） */
export function createOrder(productId, quantity) {
  return http.post('/customer/order', { productId, quantity })
}

/** 我的订单（分页，可按状态筛选） */
export function getOrderList(params) {
  return http.get('/customer/order/list', { params })
}

/** 我的订单详情 */
export function getOrderDetail(id) {
  return http.get(`/customer/order/${id}`)
}

/** 客户取消订单（开始发货前） */
export function cancelCustomerOrder(id) {
  return http.post(`/customer/order/${id}/cancel`)
}