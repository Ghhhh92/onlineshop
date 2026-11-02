package com.onlineshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 卖家修改库存（补货，US-33）：和发布时一样，1～99999 件
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Data
public class UpdateStockRequest {

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为 1")
    @Max(value = 99999, message = "库存不能超过 99999")
    private Integer stock;
}
