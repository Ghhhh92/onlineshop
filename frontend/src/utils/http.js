import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
  withCredentials: true,
})

http.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  (err) => {
    if (err.response?.status === 401) {
      ElMessage.error('请先登录')
      window.location.href = '/seller/login'
    } else {
      ElMessage.error(err.response?.data?.message || err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default http
