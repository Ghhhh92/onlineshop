package com.onlineshop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品展示实时读取；商品不可购买或已删除时仍保留条目，供客户移除。 */
public record ShoppingItem(Long id, Long productId, String productName, BigDecimal price,
                           String image, Integer quantity, Integer stock, String status,
                           boolean delisted, boolean available, LocalDateTime createTime) {
}
