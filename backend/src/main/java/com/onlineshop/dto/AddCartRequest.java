package com.onlineshop.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddCartRequest {
    @NotNull @Positive private Long productId;
    @NotNull @Min(1) @Max(999) private Integer quantity = 1;
}
