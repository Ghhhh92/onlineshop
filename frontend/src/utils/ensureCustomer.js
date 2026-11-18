import { ElMessage } from 'element-plus'

export async function ensureCustomerLogin(customerStore, router) {
  if (!customerStore.checked) {
    try {
      await customerStore.restore()
    } catch {
      // 登录态恢复失败时，仍尝试跳转登录页
    }
  }
  if (customerStore.loggedIn) return true
  ElMessage.warning('请先登录')
  router.push({
    path: '/customer/login',
    query: { redirect: router.currentRoute.value.fullPath },
  })
  return false
}