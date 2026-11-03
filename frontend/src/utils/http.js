import axios from "axios";
import { ElMessage } from "element-plus";
import { useSellerStore } from "@/stores/seller";
import { useCustomerStore } from "@/stores/customer";

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
    const url = error.config?.url || "";
    const isCustomer = url.startsWith("/customer/");
    if (status === 401) {
      if (isCustomer) {
        useCustomerStore().logout();
        const isMe = url === "/customer/me";
        if (!isMe && window.location.pathname !== "/customer/login") {
          ElMessage.error("登录已失效，请重新登录");
          window.location.assign("/customer/login");
        }
      } else {
        useSellerStore().logout();
        const isSellerMe = url === "/seller/me";
        if (!isSellerMe && window.location.pathname !== "/seller/login") {
          ElMessage.error("登录已失效，请重新登录");
          window.location.assign("/seller/login");
        }
      }
    } else if (!(error.config?.optionalEndpoint && [404, 405].includes(status))) {
      ElMessage.error(
        error.response?.data?.message || "请求失败，请检查网络后重试",
      );
    }
    return Promise.reject(error);
  },
);

export default http;