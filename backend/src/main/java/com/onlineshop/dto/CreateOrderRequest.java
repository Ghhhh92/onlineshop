package com.onlineshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户直接下单：选一件商品和数量。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "数量不能超过 999")
    private Integer quantity;
}
