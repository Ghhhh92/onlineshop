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
