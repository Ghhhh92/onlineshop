package com.onlineshop.dto;

import com.onlineshop.entity.CustomerOrderItem;

import java.math.BigDecimal;

/**
 * 订单中的一件商品：下单时的快照。
 *
 * @param productId   商品 id
 * @param productName 下单时的商品名称
 * @param price       下单时的单价
 * @param quantity    数量
 * @author 高佳豪
 * @date 2026-11-17
 */
public record OrderItemInfo(Long productId, String productName, BigDecimal price, Integer quantity) {

    public static OrderItemInfo of(CustomerOrderItem item) {
        return new OrderItemInfo(item.getProductId(), item.getProductName(), item.getPrice(), item.getQuantity());
    }
}
