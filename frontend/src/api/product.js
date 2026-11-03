import http from '@/utils/http'

/** 发布商品（卖家） */
export function publishProduct(data) {
  return http.post('/product/publish', data)
}

/** 买家商品列表：支持类别筛选与关键词搜索，分页 */
export function listProducts(params) {
  return http.get('/product/list', { params })
}

/** 买家查看商品详情（含多图与富文本描述） */
export function getProductDetail(id) {
  return http.get(`/product/${id}`)
}

/** 查询当前在售商品（兼容旧首页） */
export function getOnSaleProduct() {
  return http.get('/product/on-sale')
}

/** 两级类别树（买家和卖家共用） */
export function getCategoryTree() {
  return http.get('/category/tree')
}