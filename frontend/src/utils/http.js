import axios from "axios";
import { ElMessage } from "element-plus";
import { useSellerStore } from "@/stores/seller";
const http = axios.create({
  baseURL: "/api",
  timeout: 8000,
  withCredentials: true,
});
http.interceptors.response.use(
  (res) => {
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || "请求失败");
      return Promise.reject(new Error(res.data.message || "请求失败"));
    }
    return res.data.data;
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      useSellerStore().logout();
      if (
        !error.config?.url?.endsWith("/seller/me") &&
        window.location.pathname !== "/seller/login"
      ) {
        ElMessage.error("登录已失效，请重新登录");
        window.location.assign("/seller/login");
      }
    } else if (!(
      error.config?.optionalEndpoint && [404, 405].includes(status)
    )) {
      ElMessage.error(
        error.response?.data?.message || "请求失败，请检查网络后重试",
      );
    }
    return Promise.reject(error);
  },
);
export default http;
