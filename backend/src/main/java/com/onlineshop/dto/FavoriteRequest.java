package com.onlineshop.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FavoriteRequest {
    @NotNull @Positive private Long productId;
}
