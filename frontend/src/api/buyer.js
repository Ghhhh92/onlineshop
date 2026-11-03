import http from '@/utils/http'

/** 提交购买意向（productId 选填，不传时提交到最新在售商品） */
export function submitIntent(data) {
  return http.post('/buyer/intent', data)
}

/** 凭口令码查询排队位次与状态 */
export function getIntentByCode(code) {
  return http.get(`/buyer/intent/${encodeURIComponent(code)}`)
}

/** 凭口令码撤销意向 */
export function cancelIntent(code) {
  return http.post(`/buyer/intent/${encodeURIComponent(code)}/cancel`)
}