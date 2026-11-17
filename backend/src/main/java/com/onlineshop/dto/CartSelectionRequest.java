package com.onlineshop.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.List;

@Data
public class CartSelectionRequest {
    @NotEmpty(message = "请选择购物车商品")
    private List<@NotNull @Positive Long> cartItemIds;
}
