import http from '@/utils/http'

/** 发布商品 */
export function publishProduct(data) {
  return http.post('/product/publish', data)
}

/** 查询当前在售商品 */
export function getOnSaleProduct() {
  return http.get('/product/on-sale')
}
