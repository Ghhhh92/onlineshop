package com.onlineshop.dto;

/**
 * 下单明细：商品 id 和数量。
 * <p>
 * 直接下单时只有一条；购物车多选下单时由购物车模块按勾选条目组装后传给 {@code OrderService}。
 *
 * @param productId 商品 id
 * @param quantity  数量
 * @author 高佳豪
 * @date 2026-11-17
 */
public record OrderLine(Long productId, int quantity) {
}
