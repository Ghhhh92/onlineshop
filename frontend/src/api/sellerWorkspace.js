import http from "@/utils/http";
import { getOnSaleProduct } from "./product";

// 以下查询接口为前后端联调约定，主干后端尚未实现。404/405 单独呈现。
export async function getCurrentProduct() {
  try {
    return {
      product: await http.get("/seller/product/current", {
        optionalEndpoint: true,
      }),
      complete: true,
    };
  } catch (error) {
    if (![404, 405].includes(error.response?.status)) throw error;
    return { product: await getOnSaleProduct(), complete: false };
  }
}
export const getProductIntents = (id) =>
  http.get(`/seller/product/${id}/intents`, { optionalEndpoint: true });
export const getHistory = (page, size) =>
  http.get("/seller/product/history", {
    params: { page, size },
    optionalEndpoint: true,
  });

export const getProducts = (params) => http.get('/seller/product/list', { params });
export const getCategories = () => http.get('/category/tree');
export const saveCategory = (id, data) => id ? http.put(`/seller/category/${id}`, data) : http.post('/seller/category', data);
export const deleteCategory = (id) => http.delete(`/seller/category/${id}`);
export const updateStock = (id, stock) => http.put(`/seller/product/${id}/stock`, { stock });
export const getCustomers = (params) => http.get('/seller/customer/list', { params });
export const getCustomerOrders = (id, params) => http.get(`/seller/customer/${id}/orders`, { params });

// 挑战 B 订单管理
export const getSellerOrders = (params) => http.get('/seller/order/list', { params });
export const getSellerOrder = (id) => http.get(`/seller/order/${id}`);
export const confirmOrder = (id) => http.post(`/seller/order/${id}/confirm`);
export const prepareOrder = (id) => http.post(`/seller/order/${id}/prepare`);
export const shipOrder = (id) => http.post(`/seller/order/${id}/ship`);
export const completeOrder = (id) => http.post(`/seller/order/${id}/complete`);
export const cancelSellerOrder = (id) => http.post(`/seller/order/${id}/cancel`);